using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// Service for managing cash register (caja) operations.
/// Enforces business rules: only one open caja at a time, user FK validation,
/// non-negative amounts.
/// </summary>
public class CajaService : ICajaService
{
    private readonly AppDbContext _context;

    private static readonly int[] _estadosFinales = 
    [
        (int)EstadoPedidoEnum.Entregado,
        (int)EstadoPedidoEnum.Retirado,
        (int)EstadoPedidoEnum.Cancelado,
        (int)EstadoPedidoEnum.Devuelto
    ];

    public CajaService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<CajaResponse> AperturaAsync(int usuarioAperturaId, decimal montoApertura)
    {
        if (montoApertura < 0)
            throw new InvalidOperationException("El monto de apertura no puede ser negativo.");

        var usuario = await _context.Usuarios.FindAsync(usuarioAperturaId) 
            ?? throw new KeyNotFoundException("Usuario no encontrado.");

        if (await _context.Cajas.AnyAsync(c => c.FechaCierre == null))
            throw new InvalidOperationException("Ya existe una caja abierta.");

        var caja = new Caja
        {
            UsuarioAperturaId = usuarioAperturaId,
            MontoApertura = montoApertura,
            FechaApertura = DateTime.UtcNow
        };

        _context.Cajas.Add(caja);
        await _context.SaveChangesAsync();

        return MapToResponse(caja, usuario.UsuarioNombre, null, 0, 0, 0);
    }

    public async Task<CajaResponse> CierreAsync(int cajaId, int usuarioCierreId, decimal montoCierreTeorico, decimal montoCierreReal)
    {
        var caja = await _context.Cajas
            .Include(c => c.UsuarioApertura)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoPago)
            .FirstOrDefaultAsync(c => c.Id == cajaId) 
            ?? throw new KeyNotFoundException("Caja no encontrada.");

        if (caja.FechaCierre != null)
            throw new InvalidOperationException("La caja ya se encuentra cerrada.");

        if (montoCierreTeorico < 0 || montoCierreReal < 0)
            throw new InvalidOperationException("Los montos de cierre no pueden ser negativos.");

        var usuarioCierre = await _context.Usuarios.FindAsync(usuarioCierreId) 
            ?? throw new KeyNotFoundException("Usuario no encontrado.");

        var pedidosPendientes = caja.Pedidos.Count(p => !_estadosFinales.Contains(p.EstadoId));

        if (pedidosPendientes > 0)
        {
            throw new BusinessRuleException(
                "PENDING_ORDERS_ON_CLOSE",
                $"No se puede cerrar la caja porque tiene {pedidosPendientes} pedido(s) pendiente(s). Resuelva todos los pedidos antes de cerrar.");
        }

        caja.UsuarioCierreId = usuarioCierreId;
        caja.MontoCierreTeorico = montoCierreTeorico;
        caja.MontoCierreReal = montoCierreReal;
        caja.FechaCierre = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        var (ingEfectivo, ingTransferencia, ingTarjeta) = CalcularIngresos(caja.Pedidos);

        return MapToResponse(caja, caja.UsuarioApertura.UsuarioNombre, usuarioCierre.UsuarioNombre, ingEfectivo, ingTransferencia, ingTarjeta);
    }

    public async Task<IEnumerable<CajaResponse>> ObtenerTodasAsync(string? estado = null)
    {
        IQueryable<Caja> query = _context.Cajas
            .Include(c => c.UsuarioApertura)
            .Include(c => c.UsuarioCierre)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoPago);

        if (estado == "abiertas")
            query = query.Where(c => c.FechaCierre == null);
        else if (estado == "cerradas")
            query = query.Where(c => c.FechaCierre != null);

        query = query.OrderByDescending(c => c.FechaApertura);

        var cajas = await query.ToListAsync();

        return cajas.Select(c =>
        {
            var (efectivo, trans, tarjeta) = CalcularIngresos(c.Pedidos);
            return MapToResponse(c, c.UsuarioApertura.UsuarioNombre, c.UsuarioCierre?.UsuarioNombre, efectivo, trans, tarjeta);
        });
    }

    public async Task<CajaResponse?> ObtenerPorIdAsync(int id)
    {
        var caja = await _context.Cajas
            .Include(c => c.UsuarioApertura)
            .Include(c => c.UsuarioCierre)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoPago)
            .FirstOrDefaultAsync(c => c.Id == id);

        if (caja == null)
            return null;

        var (efectivo, trans, tarjeta) = CalcularIngresos(caja.Pedidos);

        return MapToResponse(caja, caja.UsuarioApertura.UsuarioNombre, caja.UsuarioCierre?.UsuarioNombre, efectivo, trans, tarjeta);
    }

    public async Task<IEnumerable<CajaHistorialResumenDTO>> ObtenerHistorialAsync()
    {
        var cajas = await _context.Cajas
            .AsNoTracking()
            .Where(c => c.FechaCierre != null)
            .Include(c => c.UsuarioApertura)
            .Include(c => c.UsuarioCierre)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoPago)
            .OrderByDescending(c => c.FechaCierre)
            .ToListAsync();

        return cajas.Select(MapToHistorialResumen);
    }

    public async Task<CajaHistorialDetalleDTO?> ObtenerHistorialDetalleAsync(int id)
    {
        var caja = await _context.Cajas
            .AsNoTracking()
            .Where(c => c.Id == id && c.FechaCierre != null)
            .Include(c => c.UsuarioApertura)
            .Include(c => c.UsuarioCierre)
            .Include(c => c.Pedidos).ThenInclude(p => p.Estado)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoPago)
            .Include(c => c.Pedidos).ThenInclude(p => p.MetodoVenta)
            .Include(c => c.Pedidos).ThenInclude(p => p.DetallePedidos)
            .FirstOrDefaultAsync();

        return caja is null ? null : MapToHistorialDetalle(caja);
    }

    private static (decimal Efectivo, decimal Transferencia, decimal Tarjeta) CalcularIngresos(IEnumerable<Pedido> pedidos)
    {
        decimal efectivo = 0, transferencia = 0, tarjeta = 0;

        // Solo sumar pedidos que representen un ingreso real (ignoramos cancelados o devueltos)
        var pedidosValidos = pedidos.Where(p => 
            p.EstadoId != (int)EstadoPedidoEnum.Cancelado && 
            p.EstadoId != (int)EstadoPedidoEnum.Devuelto);

        foreach (var p in pedidosValidos)
        {
            var monto = (decimal)p.TotalEstimado;
            var metodo = p.MetodoPago?.Nombre?.ToLower() ?? "";

            if (metodo.Contains("trans"))
            {
                transferencia += monto;
            }
            else if (metodo.Contains("tarjeta"))
            {
                tarjeta += monto;
            }
            else
            {
                // Fallback por defecto: Efectivo
                efectivo += monto;
            }
        }

        return (efectivo, transferencia, tarjeta);
    }

    private static CajaResponse MapToResponse(Caja caja, string usuarioAperturaNombre, string? usuarioCierreNombre, decimal ingresosEfectivo, decimal ingresosTransferencia, decimal ingresosTarjeta) => new(
        caja.Id,
        caja.UsuarioAperturaId,
        usuarioAperturaNombre,
        caja.UsuarioCierreId,
        usuarioCierreNombre,
        caja.FechaApertura,
        caja.FechaCierre,
        caja.MontoApertura,
        caja.MontoCierreTeorico,
        caja.MontoCierreReal,
        ingresosEfectivo,
        ingresosTransferencia,
        ingresosTarjeta
    );

    private static CajaHistorialResumenDTO MapToHistorialResumen(Caja caja)
    {
        var (efectivo, transferencia, tarjeta) = CalcularIngresos(caja.Pedidos);
        var teorico = caja.MontoCierreTeorico ?? 0;
        var real = caja.MontoCierreReal ?? 0;

        return new CajaHistorialResumenDTO(
            caja.Id,
            caja.UsuarioApertura.UsuarioNombre,
            caja.UsuarioCierre?.UsuarioNombre,
            caja.FechaApertura,
            caja.FechaCierre!.Value,
            caja.MontoApertura,
            teorico,
            real,
            real - teorico,
            efectivo,
            transferencia,
            tarjeta,
            caja.Pedidos.Count);
    }

    private static CajaHistorialDetalleDTO MapToHistorialDetalle(Caja caja)
    {
        var (efectivo, transferencia, tarjeta) = CalcularIngresos(caja.Pedidos);
        var teorico = caja.MontoCierreTeorico ?? 0;
        var real = caja.MontoCierreReal ?? 0;

        var pedidos = caja.Pedidos
            .OrderByDescending(p => p.FechaIngreso)
            .Select(p => new PedidoCajaDetalleDTO(
                p.Id,
                p.EstadoEnum.ToString(),
                p.ClienteNombre,
                p.MetodoVenta?.Nombre,
                p.MetodoPago?.Nombre,
                p.TotalEstimado,
                p.FechaIngreso,
                p.DetallePedidos.Select(d => new DetallePedidoCajaDTO(
                    d.ProductoId,
                    d.Nombre,
                    d.Cantidad,
                    d.Precio)).ToList()))
            .ToList();

        return new CajaHistorialDetalleDTO(
            caja.Id,
            caja.UsuarioAperturaId,
            caja.UsuarioApertura.UsuarioNombre,
            caja.UsuarioCierreId,
            caja.UsuarioCierre?.UsuarioNombre,
            caja.FechaApertura,
            caja.FechaCierre!.Value,
            caja.MontoApertura,
            teorico,
            real,
            real - teorico,
            efectivo,
            transferencia,
            tarjeta,
            pedidos);
    }
}

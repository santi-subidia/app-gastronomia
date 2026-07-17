using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.SignalR;
using ApiGastronomia.Domain;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

public class PedidoService : IPedidoService
{
    private readonly AppDbContext _context;
    private readonly IHubContext<LogisticaHub> _hubContext;
    private readonly ILogger<PedidoService> _logger;

    public PedidoService(AppDbContext context, IHubContext<LogisticaHub> hubContext, ILogger<PedidoService> logger)
    {
        _context = context;
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task<Pedido> CrearPedidoAsync(Pedido pedido)
    {
        // Task 2.3: Validate DetallePedidos is not null or empty
        if (pedido.DetallePedidos == null || pedido.DetallePedidos.Count == 0)
            throw new InvalidOperationException("El pedido debe contener al menos un producto.");

        var cajaAbierta = await _context.Cajas
            .Where(c => c.FechaCierre == null)
            .OrderByDescending(c => c.FechaApertura)
            .FirstOrDefaultAsync()
            ?? throw new BusinessRuleException(
                "NO_OPEN_REGISTER",
                "No hay una caja abierta para registrar el pedido.");

        // The active caja is resolved by the backend; the client cannot choose a historical session.
        pedido.CajaId = cajaAbierta.Id;

        if (!await _context.MetodoPago.AnyAsync(mp => mp.Id == pedido.MetodoPagoId))
            throw new InvalidOperationException($"Método de pago #{pedido.MetodoPagoId} no encontrado.");

        if (!await _context.MetodosVenta.AnyAsync(mv => mv.Id == pedido.MetodoVentaId))
            throw new InvalidOperationException($"Método de venta #{pedido.MetodoVentaId} no encontrado.");

        var productoIds = pedido.DetallePedidos.Select(d => d.ProductoId).Distinct().ToList();
        var existingProductos = await _context.Productos
            .Where(p => productoIds.Contains(p.Id))
            .ToListAsync();

        var existingProductoIds = existingProductos.Select(p => p.Id).ToList();

        var missingProductoIds = productoIds.Except(existingProductoIds).ToList();
        if (missingProductoIds.Count > 0)
            throw new InvalidOperationException($"Producto(s) no encontrado(s): {string.Join(", ", missingProductoIds)}.");

        // Determinar estado inicial según demora de productos (0 = sin cocina)
        bool requiereCocina = existingProductos.Any(p => p.Demora > 0);
        pedido.EstadoId = requiereCocina ? (int)EstadoPedidoEnum.Pendiente : (int)EstadoPedidoEnum.ListoParaRetirar;
        pedido.FechaIngreso = DateTime.UtcNow;

        _context.Pedidos.Add(pedido);
        await _context.SaveChangesAsync();

        if (requiereCocina)
        {
            await _hubContext.Clients.All.SendAsync("NuevoPedido", new NuevoPedidoMessage(
                pedido.Id,
                pedido.ClienteNombre ?? "Desconocido",
                pedido.TotalEstimado,
                DateTime.UtcNow));
        }

        _logger.LogInformation("Pedido #{PedidoId} creado con estado {Estado}", pedido.Id, (EstadoPedidoEnum)pedido.EstadoId);
        return pedido;
    }

    public async Task<Pedido?> ObtenerPedidoPorIdAsync(int id)
    {
        return await _context.Pedidos
            .Include(p => p.Estado)
            .Include(p => p.MetodoPago)
            .Include(p => p.MetodoVenta)
            .Include(p => p.Repartidor)
            .Include(p => p.Caja)
            .Include(p => p.DetallePedidos)
                .ThenInclude(d => d.Producto)
            .Include(p => p.Demoras)
            .FirstOrDefaultAsync(p => p.Id == id);
    }

    public async Task<IEnumerable<Pedido>> ObtenerPedidosAsync()
    {
        var cajaId = await ObtenerCajaAbiertaIdAsync();
        if (cajaId is null)
            return Array.Empty<Pedido>();

        return await _context.Pedidos
            .Include(p => p.Estado)
            .Include(p => p.MetodoVenta)
            .Include(p => p.Repartidor)
            .Where(p => p.CajaId == cajaId)
            .OrderByDescending(p => p.FechaIngreso)
            .ToListAsync();
    }

    public async Task<IEnumerable<Pedido>> ObtenerPedidosPorEstadoAsync(EstadoPedidoEnum estado)
    {
        var cajaId = await ObtenerCajaAbiertaIdAsync();
        if (cajaId is null)
            return Array.Empty<Pedido>();

        return await _context.Pedidos
            .Include(p => p.Estado)
            .Include(p => p.MetodoVenta)
            .Include(p => p.Repartidor)
            .Where(p => p.CajaId == cajaId && p.EstadoId == (int)estado)
            .OrderBy(p => p.FechaIngreso)
            .ToListAsync();
    }

    private async Task<int?> ObtenerCajaAbiertaIdAsync()
    {
        return await _context.Cajas
            .Where(c => c.FechaCierre == null)
            .OrderByDescending(c => c.FechaApertura)
            .Select(c => (int?)c.Id)
            .FirstOrDefaultAsync();
    }

    public async Task<Pedido> CambiarEstadoAsync(int pedidoId, EstadoPedidoEnum nuevoEstado)
    {
        var pedido = await _context.Pedidos
            .Include(p => p.Estado)
            .FirstOrDefaultAsync(p => p.Id == pedidoId)
            ?? throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        var estadoAnterior = (EstadoPedidoEnum)pedido.EstadoId;

        if (estadoAnterior is EstadoPedidoEnum.Entregado or EstadoPedidoEnum.Retirado or EstadoPedidoEnum.Cancelado or EstadoPedidoEnum.Devuelto)
        {
            throw new InvalidOperationException(
                $"No se puede cambiar el estado de un pedido en estado '{estadoAnterior}'.");
        }

        pedido.EstadoId = (int)nuevoEstado;

        // Setear fechas según el nuevo estado
        switch (nuevoEstado)
        {
            case EstadoPedidoEnum.EnCamino:
                pedido.FechaEnCamino = DateTime.UtcNow;
                break;
            case EstadoPedidoEnum.Entregado:
            case EstadoPedidoEnum.Retirado:
            case EstadoPedidoEnum.Cancelado:
            case EstadoPedidoEnum.Devuelto:
                pedido.FechaFinalizado = DateTime.UtcNow;
                break;
        }

        await _context.SaveChangesAsync();

        await _hubContext.Clients.All.SendAsync("EstadoCambiado", new EstadoCambiadoMessage(
            pedidoId,
            estadoAnterior.ToString(),
            nuevoEstado.ToString(),
            DateTime.UtcNow));

        await _hubContext.Clients.All.SendAsync("PedidoActualizado", new PedidoActualizadoMessage(
            pedidoId,
            nuevoEstado.ToString(),
            DateTime.UtcNow));

        // Send PedidoFinalizado event for terminal states
        if (nuevoEstado is EstadoPedidoEnum.Entregado or EstadoPedidoEnum.Retirado
            or EstadoPedidoEnum.Cancelado or EstadoPedidoEnum.Devuelto)
        {
            await _hubContext.Clients.All.SendAsync("PedidoFinalizado",
                new PedidoFinalizadoMessage(pedidoId, nuevoEstado.ToString(), DateTime.UtcNow));
        }

        _logger.LogInformation("Pedido #{PedidoId}: {Anterior} -> {Nuevo}", pedidoId, estadoAnterior, nuevoEstado);
        return pedido;
    }

    public async Task<Pedido> AsignarRepartidorAsync(int pedidoId, int repartidorId)
    {
        var pedido = await _context.Pedidos.FindAsync(pedidoId)
            ?? throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        var repartidor = await _context.Usuarios
            .Include(u => u.Rol)
            .FirstOrDefaultAsync(u => u.Id == repartidorId)
            ?? throw new KeyNotFoundException($"Repartidor #{repartidorId} no encontrado.");

        if (repartidor.Rol.Nombre != "Repartidor")
            throw new InvalidOperationException($"El usuario #{repartidorId} no tiene rol de repartidor.");

        if (!repartidor.Disponible)
            throw new InvalidOperationException($"El repartidor #{repartidorId} no está disponible.");

        if (!repartidor.Activo)
            throw new InvalidOperationException($"El usuario #{repartidorId} no está activo.");

        pedido.RepartidorId = repartidorId;
        pedido.FechaAsignado = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        await _hubContext.Clients.All.SendAsync("RepartidorAsignado", new RepartidorAsignadoMessage(
            pedidoId,
            repartidorId,
            repartidor.UsuarioNombre,
            DateTime.UtcNow));

        _logger.LogInformation("Repartidor #{RepartidorId} asignado al pedido #{PedidoId}", repartidorId, pedidoId);
        return pedido;
    }
}

using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.SignalR;
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

        // Task 2.2: FK existence validation
        if (pedido.CajaId.HasValue && !await _context.Cajas.AnyAsync(c => c.Id == pedido.CajaId.Value))
            throw new InvalidOperationException($"Caja #{pedido.CajaId.Value} no encontrada.");

        if (!await _context.MetodoPago.AnyAsync(mp => mp.Id == pedido.MetodoPagoId))
            throw new InvalidOperationException($"Método de pago #{pedido.MetodoPagoId} no encontrado.");

        if (!await _context.MetodosVenta.AnyAsync(mv => mv.Id == pedido.MetodoVentaId))
            throw new InvalidOperationException($"Método de venta #{pedido.MetodoVentaId} no encontrado.");

        var productoIds = pedido.DetallePedidos.Select(d => d.ProductoId).Distinct().ToList();
        var existingProductoIds = await _context.Productos
            .Where(p => productoIds.Contains(p.Id))
            .Select(p => p.Id)
            .ToListAsync();

        var missingProductoIds = productoIds.Except(existingProductoIds).ToList();
        if (missingProductoIds.Count > 0)
            throw new InvalidOperationException($"Producto(s) no encontrado(s): {string.Join(", ", missingProductoIds)}.");

        pedido.EstadoId = (int)EstadoPedidoEnum.Pendiente;
        pedido.FechaIngreso = DateTime.UtcNow;

        _context.Pedidos.Add(pedido);
        await _context.SaveChangesAsync();

        await _hubContext.Clients.Group("cocina").SendAsync("NuevoPedido", new
        {
            PedidoId = pedido.Id,
            Cliente = pedido.ClienteNombre ?? "Desconocido",
            Total = pedido.TotalEstimado,
            Fecha = DateTime.UtcNow
        });

        _logger.LogInformation("Pedido #{PedidoId} creado con estado Pendiente", pedido.Id);
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
        return await _context.Pedidos
            .Include(p => p.Estado)
            .Include(p => p.MetodoVenta)
            .Include(p => p.Repartidor)
            .OrderByDescending(p => p.FechaIngreso)
            .ToListAsync();
    }

    public async Task<IEnumerable<Pedido>> ObtenerPedidosPorEstadoAsync(EstadoPedidoEnum estado)
    {
        return await _context.Pedidos
            .Include(p => p.Estado)
            .Include(p => p.MetodoVenta)
            .Include(p => p.Repartidor)
            .Where(p => p.EstadoId == (int)estado)
            .OrderBy(p => p.FechaIngreso)
            .ToListAsync();
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

        await _hubContext.Clients.Group($"pedido_{pedidoId}").SendAsync("EstadoCambiado", new
        {
            PedidoId = pedidoId,
            EstadoAnterior = estadoAnterior.ToString(),
            EstadoNuevo = nuevoEstado.ToString(),
            Fecha = DateTime.UtcNow
        });

        await _hubContext.Clients.Group("cocina").SendAsync("PedidoActualizado", new
        {
            PedidoId = pedidoId,
            Estado = nuevoEstado.ToString(),
            Fecha = DateTime.UtcNow
        });

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

        await _hubContext.Clients.Group($"pedido_{pedidoId}").SendAsync("RepartidorAsignado", new
        {
            PedidoId = pedidoId,
            RepartidorId = repartidorId,
            NombreRepartidor = repartidor.UsuarioNombre,
            Fecha = DateTime.UtcNow
        });

        _logger.LogInformation("Repartidor #{RepartidorId} asignado al pedido #{PedidoId}", repartidorId, pedidoId);
        return pedido;
    }
}

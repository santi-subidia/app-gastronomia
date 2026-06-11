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

        if (estadoAnterior is EstadoPedidoEnum.Entregado or EstadoPedidoEnum.Retirado or EstadoPedidoEnum.Cancelado)
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

        var repartidor = await _context.Usuarios.FindAsync(repartidorId)
            ?? throw new KeyNotFoundException($"Repartidor #{repartidorId} no encontrado.");

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

    public async Task<Demora> RegistrarDemoraAsync(int pedidoId, int usuarioId, int demoraMinutos, string? sector)
    {
        _ = await _context.Pedidos.FindAsync(pedidoId)
            ?? throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        _ = await _context.Usuarios.FindAsync(usuarioId)
            ?? throw new KeyNotFoundException($"Usuario #{usuarioId} no encontrado.");

        var demora = new Demora
        {
            PedidoId = pedidoId,
            UsuarioId = usuarioId,
            DemoraMinutos = demoraMinutos,
            Sector = sector
        };

        _context.Demoras.Add(demora);
        await _context.SaveChangesAsync();

        await _hubContext.Clients.Group($"pedido_{pedidoId}").SendAsync("DemoraRegistrada", new
        {
            PedidoId = pedidoId,
            Motivo = sector ?? "No especificado",
            TiempoEstimadoMinutos = demoraMinutos,
            Fecha = DateTime.UtcNow
        });

        _logger.LogInformation("Demora registrada en pedido #{PedidoId}: {Minutos}min (sector: {Sector})",
            pedidoId, demoraMinutos, sector);
        return demora;
    }
}

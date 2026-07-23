using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

public sealed class EstimacionPedidoService : IEstimacionPedidoService
{
    private const int DeliveryMetodoVentaId = 1;
    private readonly AppDbContext _context;
    private readonly IRoutingService _routingService;
    private readonly IHubContext<LogisticaHub> _hubContext;
    private readonly ILogger<EstimacionPedidoService> _logger;

    public EstimacionPedidoService(
        AppDbContext context,
        IRoutingService routingService,
        IHubContext<LogisticaHub> hubContext,
        ILogger<EstimacionPedidoService> logger)
    {
        _context = context;
        _routingService = routingService;
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task CalcularAsync(Pedido pedido, bool consultarRuta = true)
    {
        var preparacion = pedido.DetallePedidos
            .Select(detalle => detalle.Producto?.Demora ?? 0)
            .DefaultIfEmpty(0)
            .Max();
        var demoras = pedido.Demoras.Sum(demora => demora.DemoraMinutos);

        pedido.DemoraPreparacionAprox = preparacion;
        pedido.DemoraDemorasAprox = demoras;

        if (pedido.MetodoVentaId == DeliveryMetodoVentaId &&
            pedido.LatitudDestino.HasValue && pedido.LongitudDestino.HasValue)
        {
            if (consultarRuta && pedido.DemoraDeliveryAprox is null)
            {
                var config = await _context.Configuracion.FirstOrDefaultAsync();
                if (config?.LatitudPartida is not null && config.LongitudPartida is not null)
                {
                    pedido.DemoraDeliveryAprox = await _routingService.ObtenerDuracionMinutosAsync(
                        config.LatitudPartida.Value,
                        config.LongitudPartida.Value,
                        pedido.LatitudDestino.Value,
                        pedido.LongitudDestino.Value);
                }
                else
                {
                    _logger.LogWarning("No hay coordenadas configuradas para calcular delivery del pedido #{PedidoId}", pedido.Id);
                }
            }
        }
        else
        {
            pedido.DemoraDeliveryAprox = null;
        }

        pedido.DemoraAprox = preparacion + demoras + (pedido.DemoraDeliveryAprox ?? 0);
        pedido.FechaEstimadoFin = pedido.FechaIngreso.AddMinutes(pedido.DemoraAprox.Value);

        if (pedido.Id > 0 && pedido.FechaEstimadoFin.HasValue)
        {
            await _hubContext.Clients.Group($"pedido_{pedido.Id}").SendAsync(
                "EstimacionPedidoActualizada",
                new EstimacionPedidoActualizadaMessage(
                    pedido.Id,
                    pedido.DemoraAprox.Value,
                    pedido.FechaEstimadoFin.Value,
                    pedido.DemoraPreparacionAprox ?? 0,
                    pedido.DemoraDemorasAprox ?? 0,
                    pedido.DemoraDeliveryAprox,
                    DateTime.UtcNow));
        }
    }

    public async Task RecalcularAsync(int pedidoId)
    {
        var pedido = await _context.Pedidos
            .Include(p => p.DetallePedidos)
                .ThenInclude(d => d.Producto)
            .Include(p => p.Demoras)
            .FirstOrDefaultAsync(p => p.Id == pedidoId)
            ?? throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        await CalcularAsync(pedido, consultarRuta: false);
        await _context.SaveChangesAsync();
    }
}

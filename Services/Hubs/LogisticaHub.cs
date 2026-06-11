using Microsoft.AspNetCore.SignalR;
using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Services.Hubs;

/// <summary>
/// Hub de SignalR para comunicación en tiempo real entre cocina, repartidores y clientes.
/// </summary>
public class LogisticaHub : Hub
{
    private readonly ILogger<LogisticaHub> _logger;

    public LogisticaHub(ILogger<LogisticaHub> logger)
    {
        _logger = logger;
    }

    #region Conexión / Desconexión

    public override async Task OnConnectedAsync()
    {
        _logger.LogInformation("Cliente conectado: {ConnectionId}", Context.ConnectionId);
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        _logger.LogInformation("Cliente desconectado: {ConnectionId}", Context.ConnectionId);
        await base.OnDisconnectedAsync(exception);
    }

    #endregion

    #region Grupos

    /// <summary>
    /// Une al cliente al grupo de su rol (cocina, repartidores, etc.).
    /// </summary>
    public async Task UnirseAGrupo(string grupo)
    {
        await Groups.AddToGroupAsync(Context.ConnectionId, grupo);
        _logger.LogInformation("Cliente {ConnectionId} se unió al grupo {Grupo}", Context.ConnectionId, grupo);
    }

    /// <summary>
    /// Une al cliente al grupo específico de un pedido para recibir actualizaciones.
    /// </summary>
    public async Task UnirseAPedido(int pedidoId)
    {
        var grupoPedido = $"pedido_{pedidoId}";
        await Groups.AddToGroupAsync(Context.ConnectionId, grupoPedido);
        _logger.LogInformation("Cliente {ConnectionId} se unió al grupo {Grupo}", Context.ConnectionId, grupoPedido);
    }

    /// <summary>
    /// Sale del grupo de un pedido.
    /// </summary>
    public async Task SalirDePedido(int pedidoId)
    {
        var grupoPedido = $"pedido_{pedidoId}";
        await Groups.RemoveFromGroupAsync(Context.ConnectionId, grupoPedido);
    }

    #endregion

    #region Eventos de Pedido

    /// <summary>
    /// Notifica un cambio de estado en un pedido a todos los clientes suscritos a ese pedido
    /// y al grupo de cocina.
    /// </summary>
    public async Task NotificarCambioEstado(int pedidoId, EstadoPedidoEnum estadoAnterior, EstadoPedidoEnum estadoNuevo)
    {
        await Clients.Group($"pedido_{pedidoId}").SendAsync("EstadoCambiado", new
        {
            PedidoId = pedidoId,
            EstadoAnterior = estadoAnterior.ToString(),
            EstadoNuevo = estadoNuevo.ToString(),
            Fecha = DateTime.UtcNow
        });

        // También notificar a cocina para que actualice su panel
        await Clients.Group("cocina").SendAsync("PedidoActualizado", new
        {
            PedidoId = pedidoId,
            Estado = estadoNuevo.ToString(),
            Fecha = DateTime.UtcNow
        });
    }

    /// <summary>
    /// Notifica un nuevo pedido entrante al grupo de cocina.
    /// Invocado desde el controller cuando se crea un pedido.
    /// </summary>
    public async Task NotificarNuevoPedido(int pedidoId, string cliente, decimal total)
    {
        await Clients.Group("cocina").SendAsync("NuevoPedido", new
        {
            PedidoId = pedidoId,
            Cliente = cliente,
            Total = total,
            Fecha = DateTime.UtcNow
        });
    }

    #endregion

    #region Eventos de Repartidor

    /// <summary>
    /// El repartidor envía su posición GPS actual.
    /// </summary>
    public async Task EnviarPosicionGPS(int repartidorId, double latitud, double longitud)
    {
        await Clients.Group($"pedido_repartidor_{repartidorId}").SendAsync("PosicionGPSActualizada", new
        {
            RepartidorId = repartidorId,
            Latitud = latitud,
            Longitud = longitud,
            Fecha = DateTime.UtcNow
        });
    }

    /// <summary>
    /// Notifica a los clientes suscritos que el repartidor ha sido asignado al pedido.
    /// </summary>
    public async Task NotificarRepartidorAsignado(int pedidoId, int repartidorId, string nombreRepartidor)
    {
        await Clients.Group($"pedido_{pedidoId}").SendAsync("RepartidorAsignado", new
        {
            PedidoId = pedidoId,
            RepartidorId = repartidorId,
            NombreRepartidor = nombreRepartidor,
            Fecha = DateTime.UtcNow
        });
    }

    #endregion

    #region Eventos de Demora

    /// <summary>
    /// Notifica una demora registrada en un pedido.
    /// </summary>
    public async Task NotificarDemora(int pedidoId, string motivo, int tiempoEstimadoMinutos)
    {
        await Clients.Group($"pedido_{pedidoId}").SendAsync("DemoraRegistrada", new
        {
            PedidoId = pedidoId,
            Motivo = motivo,
            TiempoEstimadoMinutos = tiempoEstimadoMinutos,
            Fecha = DateTime.UtcNow
        });
    }

    #endregion
}

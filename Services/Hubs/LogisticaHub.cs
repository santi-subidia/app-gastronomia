using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.AspNetCore.SignalR;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Services.Hubs;

/// <summary>
/// Hub de SignalR para comunicación en tiempo real entre cocina, repartidores y clientes.
/// Requiere autenticación JWT para todas las conexiones.
/// Excluido del rate limiting global — las conexiones WebSocket no deben ser limitadas.
/// </summary>
[Authorize]
[DisableRateLimiting]
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
    /// Validación de roles: "cocina" requiere rol Cajero o Cocina;
    /// "pedido_repartidor_{id}" requiere rol Repartidor; otros grupos son abiertos.
    /// </summary>
    public async Task UnirseAGrupo(string grupo)
    {
        var user = Context.User!;

        if (grupo == "cocina")
        {
            if (!user.IsInRole("Cocina") && !user.IsInRole("Cajero"))
                throw new HubException("No tiene permiso para unirse al grupo cocina.");
        }
        else if (grupo.StartsWith("pedido_repartidor_"))
        {
            if (!user.IsInRole("Repartidor"))
                throw new HubException("No tiene permiso para unirse al grupo de repartidores.");
        }

        await Groups.AddToGroupAsync(Context.ConnectionId, grupo);
        _logger.LogInformation("Cliente {ConnectionId} se unió al grupo {Grupo}", Context.ConnectionId, grupo);
    }

    /// <summary>
    /// Une al cliente al grupo específico de un pedido para recibir actualizaciones.
    /// Disponible para cualquier usuario autenticado.
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

    #region Eventos de Repartidor

    /// <summary>
    /// El repartidor envía su posición GPS actual.
    /// Solo usuarios con rol Repartidor pueden enviar posición GPS.
    /// </summary>
    public async Task EnviarPosicionGPS(int repartidorId, double latitud, double longitud)
    {
        if (!Context.User!.IsInRole("Repartidor"))
            throw new HubException("Solo repartidores pueden enviar posición GPS.");

        await Clients.Group($"pedido_repartidor_{repartidorId}").SendAsync("PosicionGPSActualizada", new PosicionGPSMessage(
            repartidorId, latitud, longitud, DateTime.UtcNow));
    }

    #endregion
}

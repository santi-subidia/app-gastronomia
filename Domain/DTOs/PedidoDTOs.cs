using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTOs para pedidos. Request y response DTOs centralizados aquí por entidad.
/// </summary>

public record PedidoResumenDTO(
    int Id,
    string Estado,
    string? ClienteNombre,
    string? MetodoVenta,
    double TotalEstimado,
    DateTime FechaIngreso
);

public record ProductoDTO(
    int Id,
    string Nombre,
    double Precio,
    int Demora
);

public record DetallePedidoDTO(
    int ProductoId,
    string Nombre,
    int Cantidad,
    double Precio,
    int TiempoMaquina
);

public record CrearPedidoRequest(
    int? CajaId,
    int MetodoPagoId,
    int MetodoVentaId,
    string? ClienteNombre,
    string? ClienteDireccion,
    double? LatitudDestino,
    double? LongitudDestino,
    double TotalEstimado,
    int? DemoraAprox,
    List<CrearDetalleRequest> Detalles
);

public record PedidoDetalleDTO(
    int Id,
    string Estado,
    string? ClienteNombre,
    string? ClienteDireccion,
    string? MetodoVenta,
    string? MetodoPago,
    double TotalEstimado,
    int? DemoraAprox,
    double? LatitudDestino,
    double? LongitudDestino,
    DateTime FechaIngreso,
    DateTime? FechaEstimadoFin,
    DateTime? FechaAsignado,
    DateTime? FechaEnCamino,
    DateTime? FechaFinalizado,
    string? RepartidorNombre,
    int? CajaId,
    int EstadoId,
    List<DetallePedidoDTO> DetallePedidos
);

public record CrearDetalleRequest(
    int ProductoId,
    string Nombre,
    double Precio,
    int Cantidad
);

public record CambiarEstadoRequest(EstadoPedidoEnum NuevoEstado);

public record AsignarRepartidorRequest(int RepartidorId);

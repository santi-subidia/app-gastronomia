namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// SignalR message DTOs. Strongly-typed records replacing anonymous objects
/// for real-time notifications. Property names are PascalCase matching
/// existing JSON keys for backward compatibility (System.Text.Json default).
/// </summary>

/// <summary>
/// Sent to cocina group when a new pedido is created.
/// JSON shape: {"PedidoId":1,"Cliente":"Juan","Total":1500.0,"Fecha":"..."}
/// </summary>
public record NuevoPedidoMessage(int PedidoId, string Cliente, double Total, DateTime Fecha);

/// <summary>
/// Sent to pedido_{id} group when pedido estado changes.
/// JSON shape: {"PedidoId":1,"EstadoAnterior":"Pendiente","EstadoNuevo":"EnPreparacion","Fecha":"..."}
/// </summary>
public record EstadoCambiadoMessage(int PedidoId, string EstadoAnterior, string EstadoNuevo, DateTime Fecha);

/// <summary>
/// Sent to cocina group when pedido estado is updated.
/// JSON shape: {"PedidoId":1,"Estado":"EnPreparacion","Fecha":"..."}
/// </summary>
public record PedidoActualizadoMessage(int PedidoId, string Estado, DateTime Fecha);

/// <summary>
/// Sent to pedido_{id} group when a repartidor is assigned.
/// JSON shape: {"PedidoId":1,"RepartidorId":5,"NombreRepartidor":"Carlos","Fecha":"..."}
/// </summary>
public record RepartidorAsignadoMessage(int PedidoId, int RepartidorId, string NombreRepartidor, DateTime Fecha);

/// <summary>
/// Sent to pedido_{id} group when a demora is registered.
/// JSON shape: {"PedidoId":1,"Motivo":"Cocina","TiempoEstimadoMinutos":15,"Fecha":"..."}
/// </summary>
public record DemoraRegistradaMessage(int PedidoId, string Motivo, int TiempoEstimadoMinutos, DateTime Fecha);

/// <summary>
/// Sent to pedido_repartidor_{id} group from EnviarPosicionGPS.
/// JSON shape: {"RepartidorId":5,"Latitud":-34.6,"Longitud":-58.4,"Fecha":"..."}
/// </summary>
public record PosicionGPSMessage(int RepartidorId, double Latitud, double Longitud, DateTime Fecha);

/// <summary>
/// Sent to pedido_{id} group when pedido reaches a terminal estado
/// (Entregado, Retirado, Cancelado, Devuelto).
/// JSON shape: {"PedidoId":42,"EstadoFinal":"Entregado","Fecha":"..."}
/// </summary>
public record PedidoFinalizadoMessage(int PedidoId, string EstadoFinal, DateTime Fecha);
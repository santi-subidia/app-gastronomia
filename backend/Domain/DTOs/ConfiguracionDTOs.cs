namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTO de respuesta para Configuracion. Proyectado desde la entidad Configuracion
/// con el nombre del metodo de pago por defecto (navegacion MetodoPagoDefault).
/// </summary>
public record ConfiguracionResponse(
    int Id,
    int? MetodoPagoDefaultId,
    string? MetodoPagoDefaultNombre,
    string? NombreGastronomico,
    double? LatitudPartida,
    double? LongitudPartida,
    int? MaxPedidosPorRepartidor = null
);

/// <summary>
/// Request DTO for creating the singleton configuration.
/// </summary>
public record CrearConfiguracionRequest(int? MetodoPagoDefaultId, string? NombreGastronomico, double? LatitudPartida, double? LongitudPartida);

/// <summary>
/// Request DTO for partially updating the singleton configuration. All fields are optional.
/// </summary>
public record ActualizarConfiguracionRequest(int? MetodoPagoDefaultId, string? NombreGastronomico, double? LatitudPartida, double? LongitudPartida, int? MaxPedidosPorRepartidor = null);
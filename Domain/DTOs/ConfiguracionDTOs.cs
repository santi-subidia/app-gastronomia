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
    double? LongitudPartida
);
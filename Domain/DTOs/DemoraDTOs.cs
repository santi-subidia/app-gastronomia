namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTO de respuesta para demoras. Proyectado desde la entidad Demora.
/// </summary>
public record DemoraResponse(
    int Id,
    int PedidoId,
    int UsuarioId,
    int DemoraMinutos,
    string? Sector,
    string? Observaciones
);

/// <summary>
/// Request DTO for creating a demora.
/// </summary>
public record CrearDemoraRequest(
    int PedidoId,
    int DemoraMinutos,
    string? Sector,
    string? Observaciones
);

/// <summary>
/// Request DTO for updating a demora.
/// </summary>
public record ActualizarDemoraRequest(
    int DemoraMinutos,
    string? Sector,
    string? Observaciones
);

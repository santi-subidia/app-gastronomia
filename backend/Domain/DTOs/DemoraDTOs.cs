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
/// Request DTO for creating a demora. Sector is derived from the JWT role server-side.
/// </summary>
public record CrearDemoraRequest(
    int PedidoId,
    int DemoraMinutos,
    string? Observaciones
);

/// <summary>
/// Request DTO for updating a demora. Sector is immutable on update.
/// </summary>
public record ActualizarDemoraRequest(
    int DemoraMinutos,
    string? Observaciones
);

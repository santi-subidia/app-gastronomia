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

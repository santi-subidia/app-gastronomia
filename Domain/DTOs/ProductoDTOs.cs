namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTO de respuesta para productos. Proyectado desde la entidad Producto.
/// </summary>
public record ProductoResponse(
    int Id,
    string Nombre,
    double Precio,
    int Demora,
    bool Activo
);
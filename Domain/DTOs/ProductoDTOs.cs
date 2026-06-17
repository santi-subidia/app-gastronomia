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

/// <summary>
/// Request DTO for creating a product.
/// </summary>
public record CrearProductoRequest(string Nombre, double Precio, int Demora);

/// <summary>
/// Request DTO for updating a product. All fields are optional (partial update).
/// </summary>
public record ActualizarProductoRequest(string? Nombre, double? Precio, int? Demora);
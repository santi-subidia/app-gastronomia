using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Producto CRUD service contract. Returns DTOs to avoid exposing entity internals.
/// Soft delete (Activo = false) is the only way to deactivate products.
/// GetById returns null for inactive products — they are invisible to callers.
/// </summary>
public interface IProductoService
{
    /// <summary>
    /// Returns all active products (Activo == true).
    /// Inactive products are excluded from the result.
    /// </summary>
    Task<IEnumerable<ProductoResponse>> ObtenerProductosAsync();

    /// <summary>
    /// Returns a single active product by ID, or null if not found OR inactive.
    /// </summary>
    Task<ProductoResponse?> ObtenerProductoPorIdAsync(int id);

    /// <summary>
    /// Creates a new product with Activo = true.
    /// Throws InvalidOperationException if Nombre already exists.
    /// </summary>
    Task<ProductoResponse> CrearProductoAsync(string nombre, double precio, int demora);

    /// <summary>
    /// Updates specific product fields. Only non-null parameters are updated.
    /// Returns null if product not found. Throws InvalidOperationException on duplicate Nombre.
    /// </summary>
    Task<ProductoResponse?> ActualizarProductoAsync(int id, string? nombre, double? precio, int? demora);

    /// <summary>
    /// Soft deletes a product by setting Activo = false.
    /// Returns true if the product was found and deactivated, false if not found or already inactive.
    /// </summary>
    Task<bool> EliminarProductoAsync(int id);
}
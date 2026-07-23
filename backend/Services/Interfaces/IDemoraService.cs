using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Demora CRUD service contract. Returns DTOs to avoid exposing entity internals.
/// userId is extracted internally from IHttpContextAccessor — not exposed in the interface.
/// </summary>
public interface IDemoraService
{
    /// <summary>
    /// Returns all demoras for a given pedido. Returns empty collection if no demoras exist.
    /// Throws KeyNotFoundException if the pedido does not exist.
    /// </summary>
    Task<IEnumerable<DemoraResponse>> ObtenerPorPedidoAsync(int pedidoId);

    /// <summary>
    /// Creates a new demora. userId and sector are extracted from JWT claims internally.
    /// Throws KeyNotFoundException if the pedido does not exist.
    /// Throws InvalidOperationException if demoraMinutos &lt;= 0.
    /// Sends SignalR notification to the pedido group and to the Cajeros group on success.
    /// </summary>
    Task<DemoraResponse> CrearAsync(int pedidoId, int demoraMinutos, string? observaciones);

    /// <summary>
    /// Updates demoraMinutos and observaciones of an existing demora.
    /// Returns null if the demora does not exist.
    /// </summary>
    Task<DemoraResponse?> ActualizarAsync(int id, int demoraMinutos, string? observaciones);

    /// <summary>
    /// Hard deletes a demora by ID.
    /// Returns true if deleted, false if not found.
    /// </summary>
    Task<bool> EliminarAsync(int id);
}

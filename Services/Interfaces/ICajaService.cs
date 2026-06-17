using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Caja service contract. Provides CRUD operations for cash register management.
/// Apertura opens a new caja (only one open caja allowed at a time).
/// Cierre closes an existing open caja by ID.
/// </summary>
public interface ICajaService
{
    /// <summary>
    /// Opens a new caja. Throws InvalidOperationException if an open caja already exists
    /// or if MontoApertura is negative. Throws KeyNotFoundException if UsuarioAperturaId doesn't exist.
    /// </summary>
    Task<CajaResponse> AperturaAsync(int usuarioAperturaId, decimal montoApertura);

    /// <summary>
    /// Closes an existing open caja. Throws KeyNotFoundException if caja not found.
    /// Throws InvalidOperationException if caja is already closed or montos are negative.
    /// Throws KeyNotFoundException if UsuarioCierreId doesn't exist.
    /// </summary>
    Task<CajaResponse> CierreAsync(int cajaId, int usuarioCierreId, decimal montoCierreTeorico, decimal montoCierreReal);

    /// <summary>
    /// Returns all cajas, optionally filtered by estado ("abiertas" or "cerradas").
    /// Unknown estado values return all cajas (no filter applied).
    /// Ordered by FechaApertura descending.
    /// </summary>
    Task<IEnumerable<CajaResponse>> ObtenerTodasAsync(string? estado = null);

    /// <summary>
    /// Returns a single caja by ID, or null if not found.
    /// </summary>
    Task<CajaResponse?> ObtenerPorIdAsync(int id);
}
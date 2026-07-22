using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Singleton configuration service contract.
/// ObtenerAsync returns null if no config exists yet.
/// CrearAsync throws InvalidOperationException if config already exists.
/// ActualizarAsync returns null if config not found; partial update (null = don't change).
/// </summary>
public interface IConfiguracionService
{
    /// <summary>
    /// Returns the singleton configuration, or null if not yet created.
    /// </summary>
    Task<ConfiguracionResponse?> ObtenerAsync();

    /// <summary>
    /// Creates the singleton configuration. Throws InvalidOperationException if one already exists.
    /// </summary>
    Task<ConfiguracionResponse> CrearAsync(int? metodoPagoDefaultId, string? nombreGastronomico, double? latitudPartida, double? longitudPartida);

    /// <summary>
    /// Partially updates the singleton configuration. Only non-null parameters are applied.
    /// Returns null if no configuration exists yet.
    /// </summary>
    Task<ConfiguracionResponse?> ActualizarAsync(int? metodoPagoDefaultId, string? nombreGastronomico, double? latitudPartida, double? longitudPartida, int? maxPedidosPorRepartidor = null);
}
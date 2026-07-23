using System.Net.Http.Json;
using System.Text.Json.Serialization;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services.Routing;

public sealed class OsrmRoutingService : IRoutingService
{
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<OsrmRoutingService> _logger;

    public OsrmRoutingService(
        HttpClient httpClient,
        IConfiguration configuration,
        ILogger<OsrmRoutingService> logger)
    {
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
    }

    public async Task<int?> ObtenerDuracionMinutosAsync(
        double origenLatitud,
        double origenLongitud,
        double destinoLatitud,
        double destinoLongitud,
        CancellationToken cancellationToken = default)
    {
        var baseUrl = _configuration["Routing:BaseUrl"] ?? "https://router.project-osrm.org/";
        var uri = $"{baseUrl.TrimEnd('/')}/route/v1/driving/" +
                  $"{origenLongitud.ToString(System.Globalization.CultureInfo.InvariantCulture)}," +
                  $"{origenLatitud.ToString(System.Globalization.CultureInfo.InvariantCulture)};" +
                  $"{destinoLongitud.ToString(System.Globalization.CultureInfo.InvariantCulture)}," +
                  $"{destinoLatitud.ToString(System.Globalization.CultureInfo.InvariantCulture)}?overview=false";

        try
        {
            using var response = await _httpClient.GetAsync(uri, cancellationToken);
            if (!response.IsSuccessStatusCode)
            {
                _logger.LogWarning("OSRM devolvió HTTP {StatusCode}", response.StatusCode);
                return null;
            }

            var result = await response.Content.ReadFromJsonAsync<OsrmResponse>(cancellationToken);
            var durationSeconds = result?.Routes?.FirstOrDefault()?.Duration;
            return durationSeconds is null ? null : Math.Max(1, (int)Math.Ceiling(durationSeconds.Value / 60d));
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException or InvalidOperationException)
        {
            _logger.LogWarning(ex, "No se pudo consultar la duración de la ruta en OSRM");
            return null;
        }
    }

    private sealed record OsrmResponse(
        [property: JsonPropertyName("code")] string? Code,
        [property: JsonPropertyName("routes")] List<OsrmRoute>? Routes);

    private sealed record OsrmRoute(
        [property: JsonPropertyName("duration")] double Duration);
}

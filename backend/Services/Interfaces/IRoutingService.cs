namespace ApiGastronomia.Services.Interfaces;

public interface IRoutingService
{
    Task<int?> ObtenerDuracionMinutosAsync(
        double origenLatitud,
        double origenLongitud,
        double destinoLatitud,
        double destinoLongitud,
        CancellationToken cancellationToken = default);
}

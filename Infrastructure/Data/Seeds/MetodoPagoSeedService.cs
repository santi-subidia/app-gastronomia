using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class MetodoPagoSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<MetodoPagoSeedService> _logger;

    private static readonly string[] SeedMetodosPago = ["Efectivo", "Transferencia", "Tarjeta"];

    public MetodoPagoSeedService(AppDbContext context, ILogger<MetodoPagoSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var nombre in SeedMetodosPago)
            {
                if (await _context.MetodoPago.AnyAsync(mp => mp.Nombre == nombre))
                    continue;

                _context.MetodoPago.Add(new MetodoPago { Nombre = nombre });
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar MetodoPagoSeedService");
        }
    }
}
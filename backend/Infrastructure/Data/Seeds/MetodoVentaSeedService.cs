using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class MetodoVentaSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<MetodoVentaSeedService> _logger;

    private static readonly string[] SeedMetodosVenta = ["Delivery", "Retiro en local"];

    public MetodoVentaSeedService(AppDbContext context, ILogger<MetodoVentaSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var nombre in SeedMetodosVenta)
            {
                if (await _context.MetodosVenta.AnyAsync(mv => mv.Nombre == nombre))
                    continue;

                _context.MetodosVenta.Add(new MetodoVenta { Nombre = nombre });
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar MetodoVentaSeedService");
        }
    }
}
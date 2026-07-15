using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class RoleSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<RoleSeedService> _logger;

    private static readonly string[] SeedRoles = ["Cajero", "Cocina", "Repartidor"];

    public RoleSeedService(AppDbContext context, ILogger<RoleSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var nombre in SeedRoles)
            {
                if (await _context.Roles.AnyAsync(r => r.Nombre == nombre))
                    continue;

                _context.Roles.Add(new Rol { Nombre = nombre });
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar RoleSeedService");
        }
    }
}
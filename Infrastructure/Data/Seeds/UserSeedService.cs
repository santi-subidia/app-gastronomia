using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class UserSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<UserSeedService> _logger;
    private const string SharedPassword = "Gastronomia2026!";

    private static readonly (string UsuarioNombre, string RolNombre)[] SeedUsers =
    [
        ("cajero1", "Cajero"),
        ("cocina1", "Cocina"),
        ("repartidor1", "Repartidor")
    ];

    public UserSeedService(AppDbContext context, ILogger<UserSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var (userName, roleName) in SeedUsers)
            {
                if (await _context.Usuarios.AnyAsync(u => u.UsuarioNombre == userName))
                    continue;

                var rol = await _context.Roles.FirstOrDefaultAsync(r => r.Nombre == roleName);
                if (rol is null)
                {
                    _logger.LogWarning("Rol '{RoleName}' no encontrado, usuario '{UserName}' omitido", roleName, userName);
                    continue;
                }

                _context.Usuarios.Add(new Usuario
                {
                    UsuarioNombre = userName,
                    PasswordHash = BCrypt.Net.BCrypt.HashPassword(SharedPassword),
                    RolId = rol.Id,
                    Disponible = true,
                    Activo = true
                });
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar UserSeedService");
        }
    }
}
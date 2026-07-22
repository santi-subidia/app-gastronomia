using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// CRUD service for Usuario entities. Returns DTOs (never PasswordHash).
/// Uses BCrypt for password hashing on create and update.
/// Soft delete (Activo = false) instead of physical deletion.
/// </summary>
public class UsuarioService : IUsuarioService
{
    private readonly AppDbContext _context;

    public UsuarioService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<IEnumerable<UsuarioResponse>> ObtenerUsuariosAsync(string? role = null)
    {
        var query = _context.Usuarios
            .Include(u => u.Rol)
            .Where(u => u.Activo);

        if (!string.IsNullOrWhiteSpace(role))
        {
            query = query.Where(u => u.Rol.Nombre == role.Trim());
        }

        return await query
            .Select(u => new UsuarioResponse(
                u.Id,
                u.UsuarioNombre,
                u.RolId,
                u.Rol.Nombre,
                u.Disponible,
                u.Activo,
                u.FueraDeServicio))
            .ToListAsync();
    }

    public async Task<UsuarioResponse?> ObtenerUsuarioPorIdAsync(int id)
    {
        var user = await _context.Usuarios
            .Include(u => u.Rol)
            .FirstOrDefaultAsync(u => u.Id == id);

        if (user is null)
            return null;

        return new UsuarioResponse(
            user.Id,
            user.UsuarioNombre,
            user.RolId,
            user.Rol.Nombre,
            user.Disponible,
            user.Activo,
            user.FueraDeServicio);
    }

    public async Task<UsuarioResponse> CrearUsuarioAsync(string usuarioNombre, string password, int rolId)
    {
        // Check for duplicate username
        if (await _context.Usuarios.AnyAsync(u => u.UsuarioNombre == usuarioNombre))
        {
            throw new InvalidOperationException($"Ya existe un usuario con el nombre '{usuarioNombre}'.");
        }

        var user = new Usuario
        {
            UsuarioNombre = usuarioNombre,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(password),
            RolId = rolId,
            Activo = true,
            Disponible = true,
            FueraDeServicio = false
        };

        _context.Usuarios.Add(user);
        await _context.SaveChangesAsync();

        // Load the Rol navigation for the response
        await _context.Entry(user).Reference(u => u.Rol).LoadAsync();

        return new UsuarioResponse(
            user.Id,
            user.UsuarioNombre,
            user.RolId,
            user.Rol.Nombre,
            user.Disponible,
            user.Activo,
            user.FueraDeServicio);
    }

    public async Task<UsuarioResponse?> ActualizarUsuarioAsync(int id, string? usuarioNombre, string? password, int? rolId, bool? disponible, bool? fueraDeServicio = null)
    {
        var user = await _context.Usuarios.Include(u => u.Rol).FirstOrDefaultAsync(u => u.Id == id);

        if (user is null)
            return null;

        if (usuarioNombre is not null)
            user.UsuarioNombre = usuarioNombre;

        if (password is not null)
            user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(password);

        if (rolId.HasValue)
            user.RolId = rolId.Value;

        if (disponible.HasValue)
            user.Disponible = disponible.Value;

        if (fueraDeServicio.HasValue)
        {
            user.FueraDeServicio = fueraDeServicio.Value;
            // Si lo marcan fuera de servicio, forzamos a que no esté disponible
            if (fueraDeServicio.Value)
            {
                user.Disponible = false;
            }
        }

        // Note: Activo is NOT updated here. Use EliminarUsuarioAsync for soft delete.

        await _context.SaveChangesAsync();

        // Reload Rol navigation if RolId changed
        if (rolId.HasValue)
            await _context.Entry(user).Reference(u => u.Rol).LoadAsync();

        return new UsuarioResponse(
            user.Id,
            user.UsuarioNombre,
            user.RolId,
            user.Rol.Nombre,
            user.Disponible,
            user.Activo,
            user.FueraDeServicio);
    }

    public async Task<bool> EliminarUsuarioAsync(int id)
    {
        var user = await _context.Usuarios.FindAsync(id);

        if (user is null)
            return false;

        user.Activo = false;
        await _context.SaveChangesAsync();

        return true;
    }
}

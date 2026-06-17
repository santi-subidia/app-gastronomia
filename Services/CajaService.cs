using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// Service for managing cash register (caja) operations.
/// Enforces business rules: only one open caja at a time, user FK validation,
/// non-negative amounts.
/// </summary>
public class CajaService : ICajaService
{
    private readonly AppDbContext _context;

    public CajaService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<CajaResponse> AperturaAsync(int usuarioAperturaId, decimal montoApertura)
    {
        if (montoApertura < 0)
            throw new InvalidOperationException("El monto de apertura no puede ser negativo.");

        var usuario = await _context.Usuarios.FindAsync(usuarioAperturaId);
        if (usuario == null)
            throw new KeyNotFoundException("Usuario no encontrado.");

        if (await _context.Cajas.AnyAsync(c => c.FechaCierre == null))
            throw new InvalidOperationException("Ya existe una caja abierta.");

        var caja = new Caja
        {
            UsuarioAperturaId = usuarioAperturaId,
            MontoApertura = montoApertura,
            FechaApertura = DateTime.UtcNow
        };

        _context.Cajas.Add(caja);
        await _context.SaveChangesAsync();

        return MapToResponse(caja, usuario.UsuarioNombre, null);
    }

    public async Task<CajaResponse> CierreAsync(int cajaId, int usuarioCierreId, decimal montoCierreTeorico, decimal montoCierreReal)
    {
        var caja = await _context.Cajas
            .Include(c => c.UsuarioApertura)
            .FirstOrDefaultAsync(c => c.Id == cajaId);

        if (caja == null)
            throw new KeyNotFoundException("Caja no encontrada.");

        if (caja.FechaCierre != null)
            throw new InvalidOperationException("La caja ya se encuentra cerrada.");

        if (montoCierreTeorico < 0 || montoCierreReal < 0)
            throw new InvalidOperationException("Los montos de cierre no pueden ser negativos.");

        var usuarioCierre = await _context.Usuarios.FindAsync(usuarioCierreId);
        if (usuarioCierre == null)
            throw new KeyNotFoundException("Usuario no encontrado.");

        caja.UsuarioCierreId = usuarioCierreId;
        caja.MontoCierreTeorico = montoCierreTeorico;
        caja.MontoCierreReal = montoCierreReal;
        caja.FechaCierre = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return MapToResponse(caja, caja.UsuarioApertura.UsuarioNombre, usuarioCierre.UsuarioNombre);
    }

    public async Task<IEnumerable<CajaResponse>> ObtenerTodasAsync(string? estado = null)
    {
        IQueryable<Caja> query = _context.Cajas;

        if (estado == "abiertas")
            query = query.Where(c => c.FechaCierre == null);
        else if (estado == "cerradas")
            query = query.Where(c => c.FechaCierre != null);

        query = query.OrderByDescending(c => c.FechaApertura);

        return await query
            .Select(c => new CajaResponse(
                c.Id,
                c.UsuarioAperturaId,
                c.UsuarioApertura.UsuarioNombre,
                c.UsuarioCierreId,
                c.UsuarioCierre != null ? c.UsuarioCierre.UsuarioNombre : null,
                c.FechaApertura,
                c.FechaCierre,
                c.MontoApertura,
                c.MontoCierreTeorico,
                c.MontoCierreReal
            ))
            .ToListAsync();
    }

    public async Task<CajaResponse?> ObtenerPorIdAsync(int id)
    {
        var caja = await _context.Cajas
            .Include(c => c.UsuarioApertura)
            .Include(c => c.UsuarioCierre)
            .FirstOrDefaultAsync(c => c.Id == id);

        if (caja == null)
            return null;

        return MapToResponse(caja, caja.UsuarioApertura.UsuarioNombre, caja.UsuarioCierre?.UsuarioNombre);
    }

    private static CajaResponse MapToResponse(Caja caja, string usuarioAperturaNombre, string? usuarioCierreNombre) => new(
        caja.Id,
        caja.UsuarioAperturaId,
        usuarioAperturaNombre,
        caja.UsuarioCierreId,
        usuarioCierreNombre,
        caja.FechaApertura,
        caja.FechaCierre,
        caja.MontoApertura,
        caja.MontoCierreTeorico,
        caja.MontoCierreReal
    );
}
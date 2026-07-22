using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// Singleton configuration service. Uses FirstOrDefaultAsync / AnyAsync
/// because Configuracion is a single-row table.
/// CrearAsync guards with AnyAsync to prevent duplicates (409 Conflict).
/// ActualizarAsync returns null if no config exists yet (404 Not Found).
/// Includes MetodoPagoDefault navigation for flattened response.
/// </summary>
public class ConfiguracionService : IConfiguracionService
{
    private readonly AppDbContext _context;

    public ConfiguracionService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<ConfiguracionResponse?> ObtenerAsync()
    {
        var config = await _context.Configuracion
            .Include(c => c.MetodoPagoDefault)
            .FirstOrDefaultAsync();

        if (config is null) return null;

        return Map(config);
    }

    public async Task<ConfiguracionResponse> CrearAsync(int? metodoPagoDefaultId, string? nombreGastronomico, double? latitudPartida, double? longitudPartida)
    {
        if (await _context.Configuracion.AnyAsync())
            throw new InvalidOperationException("La configuración ya existe.");

        var config = new Configuracion
        {
            MetodoPagoDefaultId = metodoPagoDefaultId,
            NombreGastronomico = nombreGastronomico,
            LatitudPartida = latitudPartida,
            LongitudPartida = longitudPartida
        };

        _context.Configuracion.Add(config);
        await _context.SaveChangesAsync();

        // Reload to get navigation property
        await _context.Entry(config)
            .Reference(c => c.MetodoPagoDefault)
            .LoadAsync();

        return Map(config);
    }

    public async Task<ConfiguracionResponse?> ActualizarAsync(int? metodoPagoDefaultId, string? nombreGastronomico, double? latitudPartida, double? longitudPartida, int? maxPedidosPorRepartidor = null)
    {
        var config = await _context.Configuracion
            .Include(c => c.MetodoPagoDefault)
            .FirstOrDefaultAsync();

        if (config is null) return null;

        if (metodoPagoDefaultId is not null) config.MetodoPagoDefaultId = metodoPagoDefaultId;
        if (nombreGastronomico is not null) config.NombreGastronomico = nombreGastronomico;
        if (latitudPartida.HasValue) config.LatitudPartida = latitudPartida.Value;
        if (longitudPartida.HasValue) config.LongitudPartida = longitudPartida.Value;
        if (maxPedidosPorRepartidor.HasValue) config.MaxPedidosPorRepartidor = maxPedidosPorRepartidor.Value;

        await _context.SaveChangesAsync();

        // Reload navigation after potential FK change
        await _context.Entry(config)
            .Reference(c => c.MetodoPagoDefault)
            .LoadAsync();

        return Map(config);
    }

    private ConfiguracionResponse Map(Configuracion c) =>
        new(
            c.Id,
            c.MetodoPagoDefaultId,
            c.MetodoPagoDefault?.Nombre,
            c.NombreGastronomico,
            c.LatitudPartida,
            c.LongitudPartida,
            c.MaxPedidosPorRepartidor);
}
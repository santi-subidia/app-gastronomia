using System.Security.Claims;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// CRUD service for Demora entities. Returns DTOs to avoid exposing entity internals.
/// userId is extracted from JWT claims via IHttpContextAccessor — not passed as a parameter.
/// SignalR notification sent only on POST (CrearAsync).
/// </summary>
public class DemoraService : IDemoraService
{
    private readonly AppDbContext _context;
    private readonly IHubContext<LogisticaHub> _hubContext;
    private readonly IHttpContextAccessor _httpContextAccessor;
    private readonly ILogger<DemoraService> _logger;

    public DemoraService(
        AppDbContext context,
        IHubContext<LogisticaHub> hubContext,
        IHttpContextAccessor httpContextAccessor,
        ILogger<DemoraService> logger)
    {
        _context = context;
        _hubContext = hubContext;
        _httpContextAccessor = httpContextAccessor;
        _logger = logger;
    }

    public async Task<IEnumerable<DemoraResponse>> ObtenerPorPedidoAsync(int pedidoId)
    {
        var pedidoExists = await _context.Pedidos.AnyAsync(p => p.Id == pedidoId);
        if (!pedidoExists)
            throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        return await _context.Demoras
            .Where(d => d.PedidoId == pedidoId)
            .Select(d => new DemoraResponse(
                d.Id,
                d.PedidoId,
                d.UsuarioId,
                d.DemoraMinutos,
                d.Sector,
                d.Observaciones))
            .ToListAsync();
    }

    public async Task<DemoraResponse> CrearAsync(int pedidoId, int demoraMinutos, string? sector, string? observaciones)
    {
        if (demoraMinutos <= 0)
            throw new InvalidOperationException("La demora debe ser mayor que cero.");

        _ = await _context.Pedidos.FindAsync(pedidoId)
            ?? throw new KeyNotFoundException($"Pedido #{pedidoId} no encontrado.");

        var userId = ExtractUserIdFromClaims();

        var demora = new Demora
        {
            PedidoId = pedidoId,
            UsuarioId = userId,
            DemoraMinutos = demoraMinutos,
            Sector = sector,
            Observaciones = observaciones
        };

        _context.Demoras.Add(demora);
        await _context.SaveChangesAsync();

        await _hubContext.Clients.Group($"pedido_{pedidoId}").SendAsync("DemoraRegistrada", new
        {
            PedidoId = pedidoId,
            Motivo = sector ?? "No especificado",
            TiempoEstimadoMinutos = demoraMinutos,
            Fecha = DateTime.UtcNow
        });

        _logger.LogInformation("Demora registrada en pedido #{PedidoId}: {Minutos}min (sector: {Sector})",
            pedidoId, demoraMinutos, sector);

        return new DemoraResponse(
            demora.Id,
            demora.PedidoId,
            demora.UsuarioId,
            demora.DemoraMinutos,
            demora.Sector,
            demora.Observaciones);
    }

    public async Task<DemoraResponse?> ActualizarAsync(int id, int demoraMinutos, string? sector, string? observaciones)
    {
        var demora = await _context.Demoras.FindAsync(id);
        if (demora is null)
            return null;

        demora.DemoraMinutos = demoraMinutos;
        if (sector is not null)
            demora.Sector = sector;
        if (observaciones is not null)
            demora.Observaciones = observaciones;

        await _context.SaveChangesAsync();

        return new DemoraResponse(
            demora.Id,
            demora.PedidoId,
            demora.UsuarioId,
            demora.DemoraMinutos,
            demora.Sector,
            demora.Observaciones);
    }

    public async Task<bool> EliminarAsync(int id)
    {
        var demora = await _context.Demoras.FindAsync(id);
        if (demora is null)
            return false;

        _context.Demoras.Remove(demora);
        await _context.SaveChangesAsync();
        return true;
    }

    private int ExtractUserIdFromClaims()
    {
        var userIdClaim = _httpContextAccessor.HttpContext!.User.FindFirstValue("sub");
        return int.Parse(userIdClaim!);
    }
}
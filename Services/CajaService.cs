using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// Minimal stub for CajaService — all methods throw NotImplementedException.
/// Actual implementation will be provided in T-002 Part B.
/// </summary>
public class CajaService : ICajaService
{
    private readonly AppDbContext _context;

    public CajaService(AppDbContext context)
    {
        _context = context;
    }

    public Task<CajaResponse> AperturaAsync(int usuarioAperturaId, decimal montoApertura)
    {
        throw new NotImplementedException();
    }

    public Task<CajaResponse> CierreAsync(int cajaId, int usuarioCierreId, decimal montoCierreTeorico, decimal montoCierreReal)
    {
        throw new NotImplementedException();
    }

    public Task<IEnumerable<CajaResponse>> ObtenerTodasAsync(string? estado = null)
    {
        throw new NotImplementedException();
    }

    public Task<CajaResponse?> ObtenerPorIdAsync(int id)
    {
        throw new NotImplementedException();
    }
}
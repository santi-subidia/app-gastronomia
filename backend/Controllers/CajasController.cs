using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using System.Security.Claims;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class CajasController : ControllerBase
{
    private readonly ICajaService _cajaService;
    private readonly ILogger<CajasController> _logger;

    public CajasController(ICajaService cajaService, ILogger<CajasController> logger)
    {
        _cajaService = cajaService;
        _logger = logger;
    }

    /// <summary>
    /// Opens a new caja (apertura). Only one open caja can exist at a time.
    /// </summary>
    [HttpPost("apertura")]
    public async Task<ActionResult<CajaResponse>> Apertura([FromBody] AperturaRequest request)
    {
        try
        {
            var userIdStr = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdStr, out var userId))
                return Unauthorized(new { Mensaje = "Usuario inválido en el token." });

            var result = await _cajaService.AperturaAsync(userId, request.MontoApertura);
            return CreatedAtAction(nameof(GetById), new { id = result.Id }, result);
        }
        catch (BusinessRuleException ex)
        {
            return Conflict(new { Codigo = ex.Code, Mensaje = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Closes an existing open caja by ID (cierre).
    /// </summary>
    [HttpPost("{id:int}/cierre")]
    public async Task<ActionResult<CajaResponse>> Cerrar(int id, [FromBody] CierreRequest request)
    {
        try
        {
            var userIdStr = User.FindFirstValue(ClaimTypes.NameIdentifier);
            if (!int.TryParse(userIdStr, out var userId))
                return Unauthorized(new { Mensaje = "Usuario inválido en el token." });

            var result = await _cajaService.CierreAsync(
                id, userId, request.MontoCierreTeorico, request.MontoCierreReal);
            return Ok(result);
        }
        catch (BusinessRuleException ex)
        {
            return Conflict(new { Codigo = ex.Code, Mensaje = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Returns all cajas, optionally filtered by estado (abiertas/cerradas).
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<CajaResponse>>> GetAll([FromQuery] string? estado = null)
    {
        var cajas = await _cajaService.ObtenerTodasAsync(estado);
        return Ok(cajas);
    }

    /// <summary>
    /// Returns a single caja by ID.
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<CajaResponse>> GetById(int id)
    {
        var caja = await _cajaService.ObtenerPorIdAsync(id);
        if (caja is null)
            return NotFound(new { Mensaje = "Caja no encontrada." });

        return Ok(caja);
    }
}

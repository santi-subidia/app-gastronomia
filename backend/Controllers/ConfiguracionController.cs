using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ConfiguracionController : ControllerBase
{
    private readonly IConfiguracionService _service;

    public ConfiguracionController(IConfiguracionService service)
    {
        _service = service;
    }

    /// <summary>
    /// Obtiene la configuración singleton. Accesible para cualquier usuario autenticado.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<ConfiguracionResponse>> Get()
    {
        var config = await _service.ObtenerAsync();
        if (config is null) return NotFound(new { Mensaje = "Configuración no encontrada." });
        return Ok(config);
    }

    /// <summary>
    /// Crea la configuración singleton. Solo accesible para Cajero.
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<ConfiguracionResponse>> Create([FromBody] CrearConfiguracionRequest request)
    {
        try
        {
            var config = await _service.CrearAsync(
                request.MetodoPagoDefaultId,
                request.NombreGastronomico,
                request.LatitudPartida,
                request.LongitudPartida);
            return CreatedAtAction(nameof(Get), null, config);
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Actualiza parcialmente la configuración singleton. Solo accesible para Cajero.
    /// </summary>
    [HttpPut]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<ConfiguracionResponse>> Update([FromBody] ActualizarConfiguracionRequest request)
    {
        var result = await _service.ActualizarAsync(
            request.MetodoPagoDefaultId,
            request.NombreGastronomico,
            request.LatitudPartida,
            request.LongitudPartida,
            request.MaxPedidosPorRepartidor);

        if (result is null)
            return NotFound(new { Mensaje = "Configuración no encontrada." });

        return Ok(result);
    }
}
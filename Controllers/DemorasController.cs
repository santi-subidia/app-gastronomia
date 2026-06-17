using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class DemorasController : ControllerBase
{
    private readonly IDemoraService _demoraService;

    public DemorasController(IDemoraService demoraService)
    {
        _demoraService = demoraService;
    }

    /// <summary>
    /// Obtiene todas las demoras de un pedido. Accesible para cualquier usuario autenticado.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<DemoraResponse>>> GetByPedido([FromQuery] int pedidoId)
    {
        try
        {
            var demoras = await _demoraService.ObtenerPorPedidoAsync(pedidoId);
            return Ok(demoras);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Registra una nueva demora. Solo accesible para Cajero.
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<DemoraResponse>> Create([FromBody] CrearDemoraRequest request)
    {
        try
        {
            var demora = await _demoraService.CrearAsync(
                request.PedidoId, request.DemoraMinutos, request.Sector, request.Observaciones);
            return CreatedAtAction(nameof(GetByPedido), new { pedidoId = demora.PedidoId }, demora);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Actualiza una demora existente. Solo accesible para Cajero.
    /// </summary>
    [HttpPut("{id:int}")]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<DemoraResponse>> Update(int id, [FromBody] ActualizarDemoraRequest request)
    {
        var result = await _demoraService.ActualizarAsync(
            id, request.DemoraMinutos, request.Sector, request.Observaciones);

        if (result is null)
            return NotFound(new { Mensaje = $"Demora #{id} no encontrada." });

        return Ok(result);
    }

    /// <summary>
    /// Elimina permanentemente una demora. Solo accesible para Cajero.
    /// </summary>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult> Delete(int id)
    {
        var deleted = await _demoraService.EliminarAsync(id);
        if (!deleted)
            return NotFound(new { Mensaje = $"Demora #{id} no encontrada." });

        return NoContent();
    }
}
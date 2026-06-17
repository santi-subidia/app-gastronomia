using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;

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

    [HttpPost("apertura")]
    public Task<ActionResult<CajaResponse>> Apertura([FromBody] AperturaRequest request)
    {
        throw new NotImplementedException();
    }

    [HttpPost("{id:int}/cierre")]
    public Task<ActionResult<CajaResponse>> Cerrar(int id, [FromBody] CierreRequest request)
    {
        throw new NotImplementedException();
    }

    [HttpGet]
    public Task<ActionResult<IEnumerable<CajaResponse>>> GetAll([FromQuery] string? estado = null)
    {
        throw new NotImplementedException();
    }

    [HttpGet("{id:int}")]
    public Task<ActionResult<CajaResponse>> GetById(int id)
    {
        throw new NotImplementedException();
    }
}
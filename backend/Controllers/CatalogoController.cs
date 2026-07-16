using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Infrastructure.Data;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[AllowAnonymous]
public class CatalogoController : ControllerBase
{
    private readonly AppDbContext _context;

    public CatalogoController(AppDbContext context)
    {
        _context = context;
    }

    [HttpGet("estados-pedido")]
    public async Task<IActionResult> GetEstadosPedido()
    {
        var list = await _context.EstadosPedidos
            .Select(e => new { e.Id, e.Nombre })
            .ToListAsync();
        return Ok(list);
    }

    [HttpGet("metodos-pago")]
    public async Task<IActionResult> GetMetodosPago()
    {
        var list = await _context.MetodoPago
            .Select(e => new { e.Id, e.Nombre })
            .ToListAsync();
        return Ok(list);
    }

    [HttpGet("metodos-venta")]
    public async Task<IActionResult> GetMetodosVenta()
    {
        var list = await _context.MetodosVenta
            .Select(e => new { e.Id, e.Nombre })
            .ToListAsync();
        return Ok(list);
    }
}

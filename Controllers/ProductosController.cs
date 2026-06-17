using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ProductosController : ControllerBase
{
    private readonly IProductoService _productoService;

    public ProductosController(IProductoService productoService)
    {
        _productoService = productoService;
    }

    /// <summary>
    /// Obtiene todos los productos activos. Accesible para cualquier usuario autenticado.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<ProductoResponse>>> GetAll()
    {
        var productos = await _productoService.ObtenerProductosAsync();
        return Ok(productos);
    }

    /// <summary>
    /// Obtiene un producto por su ID. Retorna 404 si no existe o está inactivo.
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<ProductoResponse>> GetById(int id)
    {
        var producto = await _productoService.ObtenerProductoPorIdAsync(id);
        if (producto is null)
            return NotFound(new { Mensaje = $"Producto #{id} no encontrado." });

        return Ok(producto);
    }

    /// <summary>
    /// Crea un nuevo producto. Solo accesible para Cajero.
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<ProductoResponse>> Create([FromBody] CrearProductoRequest request)
    {
        try
        {
            var producto = await _productoService.CrearProductoAsync(
                request.Nombre, request.Precio, request.Demora);
            return CreatedAtAction(nameof(GetById), new { id = producto.Id }, producto);
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Actualiza un producto parcialmente. Solo accesible para Cajero.
    /// </summary>
    [HttpPut("{id:int}")]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult<ProductoResponse>> Update(int id, [FromBody] ActualizarProductoRequest request)
    {
        try
        {
            var result = await _productoService.ActualizarProductoAsync(
                id, request.Nombre, request.Precio, request.Demora);

            if (result is null)
                return NotFound(new { Mensaje = $"Producto #{id} no encontrado." });

            return Ok(result);
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Elimina lógicamente un producto (soft delete). Solo accesible para Cajero.
    /// </summary>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = "Cajero")]
    public async Task<ActionResult> Delete(int id)
    {
        var deleted = await _productoService.EliminarProductoAsync(id);
        if (!deleted)
            return NotFound(new { Mensaje = $"Producto #{id} no encontrado." });

        return NoContent();
    }
}
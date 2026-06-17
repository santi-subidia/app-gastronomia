using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
public class PedidosController : ControllerBase
{
    private readonly IPedidoService _pedidoService;
    private readonly ILogger<PedidosController> _logger;

    public PedidosController(IPedidoService pedidoService, ILogger<PedidosController> logger)
    {
        _pedidoService = pedidoService;
        _logger = logger;
    }

    /// <summary>
    /// Obtiene todos los pedidos.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<IEnumerable<Pedido>>> GetPedidos()
    {
        var pedidos = await _pedidoService.ObtenerPedidosAsync();
        return Ok(pedidos);
    }

    /// <summary>
    /// Obtiene un pedido por su ID.
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<Pedido>> GetPedido(int id)
    {
        var pedido = await _pedidoService.ObtenerPedidoPorIdAsync(id);
        return pedido is null
            ? NotFound(new { Mensaje = $"Pedido #{id} no encontrado." })
            : Ok(pedido);
    }

    /// <summary>
    /// Obtiene pedidos filtrados por estado.
    /// </summary>
    [HttpGet("estado/{estado}")]
    public async Task<ActionResult<IEnumerable<Pedido>>> GetPedidosPorEstado(EstadoPedidoEnum estado)
    {
        var pedidos = await _pedidoService.ObtenerPedidosPorEstadoAsync(estado);
        return Ok(pedidos);
    }

    /// <summary>
    /// Crea un nuevo pedido.
    /// </summary>
    [HttpPost]
    public async Task<ActionResult<Pedido>> CrearPedido([FromBody] CrearPedidoRequest request)
    {
        try
        {
            var pedido = new Pedido
            {
                CajaId = request.CajaId,
                MetodoPagoId = request.MetodoPagoId,
                MetodoVentaId = request.MetodoVentaId,
                ClienteNombre = request.ClienteNombre,
                ClienteDireccion = request.ClienteDireccion,
                LatitudDestino = request.LatitudDestino,
                LongitudDestino = request.LongitudDestino,
                TotalEstimado = request.TotalEstimado,
                DemoraAprox = request.DemoraAprox,
                DetallePedidos = request.Detalles.Select(d => new DetallePedido
                {
                    ProductoId = d.ProductoId,
                    Nombre = d.Nombre,
                    Precio = d.Precio,
                    Cantidad = d.Cantidad
                }).ToList()
            };

            var creado = await _pedidoService.CrearPedidoAsync(pedido);
            return CreatedAtAction(nameof(GetPedido), new { id = creado.Id }, creado);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al crear pedido");
            return BadRequest(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Cambia el estado de un pedido.
    /// </summary>
    [HttpPatch("{id:int}/estado")]
    public async Task<ActionResult<Pedido>> CambiarEstado(int id, [FromBody] CambiarEstadoRequest request)
    {
        try
        {
            var pedido = await _pedidoService.CambiarEstadoAsync(id, request.NuevoEstado);
            return Ok(pedido);
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
    /// Asigna un repartidor a un pedido.
    /// </summary>
    [HttpPatch("{id:int}/repartidor")]
    public async Task<ActionResult<Pedido>> AsignarRepartidor(int id, [FromBody] AsignarRepartidorRequest request)
    {
        try
        {
            var pedido = await _pedidoService.AsignarRepartidorAsync(id, request.RepartidorId);
            return Ok(pedido);
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
    }
}

// ---- Request DTOs inline ----

public record CrearPedidoRequest(
    int? CajaId,
    int MetodoPagoId,
    int MetodoVentaId,
    string? ClienteNombre,
    string? ClienteDireccion,
    double? LatitudDestino,
    double? LongitudDestino,
    double TotalEstimado,
    int? DemoraAprox,
    List<CrearDetalleRequest> Detalles
);

public record CrearDetalleRequest(
    int ProductoId,
    string Nombre,
    double Precio,
    int Cantidad,
    int TiempoMaquina
);

public record CambiarEstadoRequest(EstadoPedidoEnum NuevoEstado);
public record AsignarRepartidorRequest(int RepartidorId);

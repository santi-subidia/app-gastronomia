using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain;
using ApiGastronomia.Domain.DTOs;
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
    public async Task<ActionResult<IEnumerable<PedidoResumenDTO>>> GetPedidos()
    {
        var pedidos = await _pedidoService.ObtenerPedidosAsync();
        return Ok(pedidos.Select(MapToResumen));
    }

    /// <summary>
    /// Obtiene un pedido por su ID.
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<PedidoDetalleDTO>> GetPedido(int id)
    {
        var pedido = await _pedidoService.ObtenerPedidoPorIdAsync(id);
        return pedido is null
            ? NotFound(new { Mensaje = $"Pedido #{id} no encontrado." })
            : Ok(MapToDetalle(pedido));
    }

    /// <summary>
    /// Obtiene pedidos filtrados por estado.
    /// </summary>
    [HttpGet("estado/{estado}")]
    public async Task<ActionResult<IEnumerable<PedidoResumenDTO>>> GetPedidosPorEstado(EstadoPedidoEnum estado)
    {
        var pedidos = await _pedidoService.ObtenerPedidosPorEstadoAsync(estado);
        return Ok(pedidos.Select(MapToResumen));
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
        catch (BusinessRuleException ex)
        {
            _logger.LogWarning(ex, "Business rule prevented pedido creation");
            return Conflict(new { Codigo = ex.Code, Mensaje = ex.Message });
        }
        catch (InvalidOperationException ex)
        {
            _logger.LogWarning(ex, "Validation error creating pedido");
            return BadRequest(new { Mensaje = ex.Message });
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
    public async Task<ActionResult<Pedido>> CambiarEstado(int id, [FromBody] int nuevoEstadoId)
    {
        try
        {
            var nuevoEstado = (EstadoPedidoEnum)nuevoEstadoId;
            var pedido = await _pedidoService.CambiarEstadoAsync(id, nuevoEstado);
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
        catch (InvalidOperationException ex)
        {
            return BadRequest(new { Mensaje = ex.Message });
        }
    }

    // ================================================================
    // Private mapping helpers — entity → DTO
    // ================================================================

    private static PedidoResumenDTO MapToResumen(Pedido p) => new(
        Id: p.Id,
        Estado: p.Estado.Nombre,
        ClienteNombre: p.ClienteNombre,
        MetodoVenta: p.MetodoVenta?.Nombre,
        TotalEstimado: p.TotalEstimado,
        FechaIngreso: p.FechaIngreso
    );

    private static PedidoDetalleDTO MapToDetalle(Pedido p) => new(
        Id: p.Id,
        Estado: p.Estado.Nombre,
        ClienteNombre: p.ClienteNombre,
        ClienteDireccion: p.ClienteDireccion,
        MetodoVenta: p.MetodoVenta?.Nombre,
        MetodoPago: p.MetodoPago?.Nombre,
        TotalEstimado: p.TotalEstimado,
        DemoraAprox: p.DemoraAprox,
        LatitudDestino: p.LatitudDestino,
        LongitudDestino: p.LongitudDestino,
        FechaIngreso: p.FechaIngreso,
        FechaEstimadoFin: p.FechaEstimadoFin,
        FechaAsignado: p.FechaAsignado,
        FechaEnCamino: p.FechaEnCamino,
        FechaFinalizado: p.FechaFinalizado,
        RepartidorNombre: p.Repartidor?.UsuarioNombre,
        CajaId: p.CajaId,
        EstadoId: p.EstadoId,
        DetallePedidos: p.DetallePedidos.Select(d => new DetallePedidoDTO(
            ProductoId: d.ProductoId,
            Nombre: d.Nombre,
            Cantidad: d.Cantidad,
            Precio: d.Precio,
            TiempoMaquina: d.Producto?.Demora ?? 0
        )).ToList()
    );
}

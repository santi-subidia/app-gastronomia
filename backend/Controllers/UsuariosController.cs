using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using System.Security.Claims;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UsuariosController : ControllerBase
{
    private readonly IUsuarioService _usuarioService;
    private readonly AppDbContext? _context;

    public UsuariosController(IUsuarioService usuarioService, AppDbContext? context = null)
    {
        _usuarioService = usuarioService;
        _context = context;
    }

    /// <summary>
    /// Obtiene todos los usuarios activos. Accesible para Admin y Cajero.
    /// Puede filtrar por rol usando el query string ?role=Repartidor.
    /// </summary>
    [HttpGet]
    [Authorize(Roles = "Admin,Cajero")]
    public async Task<ActionResult<IEnumerable<UsuarioResponse>>> GetAll([FromQuery] string? role = null)
    {
        var usuarios = await _usuarioService.ObtenerUsuariosAsync(role);
        return Ok(usuarios);
    }

    /// <summary>
    /// Obtiene un usuario por su ID. Admin puede ver cualquier usuario;
    /// un usuario no-admin solo puede ver su propio registro.
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<UsuarioResponse>> GetById(int id)
    {
        var usuario = await _usuarioService.ObtenerUsuarioPorIdAsync(id);
        if (usuario is null)
            return NotFound(new { Mensaje = $"Usuario #{id} no encontrado." });

        var currentUserId = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        var currentUserRole = User.FindFirstValue(ClaimTypes.Role) ?? User.FindFirstValue("role");

        if (currentUserRole != "Admin" && currentUserId != id.ToString())
            return Forbid();

        return Ok(usuario);
    }

    /// <summary>
    /// Crea un nuevo usuario. Solo accesible para Admin.
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult<UsuarioResponse>> Create([FromBody] CreateUserRequest request)
    {
        try
        {
            var usuario = await _usuarioService.CrearUsuarioAsync(
                request.UsuarioNombre, request.Password, request.RolId);
            return CreatedAtAction(nameof(GetById), new { id = usuario.Id }, usuario);
        }
        catch (InvalidOperationException ex)
        {
            return Conflict(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Actualiza un usuario. Admin puede actualizar cualquier usuario;
    /// un usuario no-admin solo puede actualizar su propio registro.
    /// </summary>
    [HttpPut("{id:int}")]
    public async Task<ActionResult<UsuarioResponse>> Update(int id, [FromBody] UpdateUserRequest request)
    {
        var currentUserId = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        var currentUserRole = User.FindFirstValue(ClaimTypes.Role) ?? User.FindFirstValue("role");

        if (currentUserRole != "Admin" && currentUserId != id.ToString())
            return Forbid();

        var result = await _usuarioService.ActualizarUsuarioAsync(
            id, request.UsuarioNombre, request.Password, request.RolId, request.Disponible, request.FueraDeServicio);

        if (result is null)
            return NotFound(new { Mensaje = $"Usuario #{id} no encontrado." });

        return Ok(result);
    }

    /// <summary>
    /// Elimina lógicamente un usuario (soft delete). Solo accesible para Admin.
    /// </summary>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult> Delete(int id)
    {
        var deleted = await _usuarioService.EliminarUsuarioAsync(id);
        if (!deleted)
            return NotFound(new { Mensaje = $"Usuario #{id} no encontrado." });

        return NoContent();
    }

    /// <summary>
    /// Permite a un repartidor reportar una contingencia (ej. moto rota).
    /// El repartidor pasa a fuera de servicio y sus pedidos activos van a Contingencia.
    /// </summary>
    [HttpPost("{id:int}/contingencia")]
    [Authorize(Roles = "Repartidor,Cajero")]
    public async Task<ActionResult> ReportarContingencia(int id, [FromBody] ReportarContingenciaRequest request)
    {
        var currentUserIdStr = User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub");
        var currentUserRole = User.FindFirstValue(ClaimTypes.Role) ?? User.FindFirstValue("role");

        if (currentUserRole != "Cajero" && currentUserIdStr != id.ToString())
            return Forbid();

        try
        {
            await _usuarioService.ReportarContingenciaAsync(id, request.Motivo);
            return Ok(new { Mensaje = "Contingencia reportada exitosamente." });
        }
        catch (KeyNotFoundException ex)
        {
            return NotFound(new { Mensaje = ex.Message });
        }
    }

    /// <summary>
    /// Obtiene todos los repartidores disponibles. Accesible para Cajero y Admin.
    /// </summary>
    [HttpGet("repartidores/disponibles")]
    [Authorize(Roles = "Admin,Cajero")]
    public async Task<ActionResult<IEnumerable<UsuarioResponse>>> GetRepartidoresDisponibles()
    {
        var maxPedidos = 1;
        if (_context != null)
        {
            var config = await _context.Configuracion.FirstOrDefaultAsync();
            maxPedidos = config?.MaxPedidosPorRepartidor ?? 1;
        }

        var usuarios = await _usuarioService.ObtenerUsuariosAsync();
        var repartidores = usuarios.Where(u => u.RolNombre == "Repartidor" && u.Disponible);

        if (_context == null)
        {
            return Ok(repartidores);
        }

        var filteredRepartidores = new List<UsuarioResponse>();
        foreach (var repartidor in repartidores)
        {
            var count = await _context.Pedidos.CountAsync(p =>
                p.RepartidorId == repartidor.Id &&
                (p.EstadoId == (int)EstadoPedidoEnum.ListoParaRetirar || 
                 p.EstadoId == (int)EstadoPedidoEnum.EnCamino ||
                 p.EstadoId == 2 ||
                 p.EstadoId == 3));

            if (count < maxPedidos)
            {
                filteredRepartidores.Add(repartidor);
            }
        }

        return Ok(filteredRepartidores);
    }
}

using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using System.ComponentModel.DataAnnotations;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class UsuariosController : ControllerBase
{
    private readonly IUsuarioService _usuarioService;

    public UsuariosController(IUsuarioService usuarioService)
    {
        _usuarioService = usuarioService;
    }

    /// <summary>
    /// Obtiene todos los usuarios activos. Solo accesible para Admin.
    /// </summary>
    [HttpGet]
    [Authorize(Roles = "Admin")]
    public async Task<ActionResult<IEnumerable<UsuarioResponse>>> GetAll()
    {
        var usuarios = await _usuarioService.ObtenerUsuariosAsync();
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

        // Self-access check: non-admin users can only see their own record
        var currentUserId = User.Claims.FirstOrDefault(c => c.Type == "sub")?.Value;
        var currentUserRole = User.Claims.FirstOrDefault(c => c.Type == "role")?.Value;

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
        // Self-access check for non-admin users
        var currentUserId = User.Claims.FirstOrDefault(c => c.Type == "sub")?.Value;
        var currentUserRole = User.Claims.FirstOrDefault(c => c.Type == "role")?.Value;

        if (currentUserRole != "Admin" && currentUserId != id.ToString())
            return Forbid();

        var result = await _usuarioService.ActualizarUsuarioAsync(
            id, request.UsuarioNombre, request.Password, request.RolId, request.Disponible);

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
}

/// <summary>
/// Request DTO for creating a user. Inline in controller following project convention.
/// </summary>
public record CreateUserRequest(string UsuarioNombre, [property: MinLength(6)] string Password, int RolId);

/// <summary>
/// Request DTO for updating a user. All fields are optional (partial update).
/// </summary>
public record UpdateUserRequest(string? UsuarioNombre, [property: MinLength(6)] string? Password, int? RolId, bool? Disponible);
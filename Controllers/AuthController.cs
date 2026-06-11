using Microsoft.AspNetCore.Mvc;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

    /// <summary>
    /// Authenticates a user and returns a JWT token.
    /// Returns 401 if credentials are invalid or user is inactive.
    /// </summary>
    [HttpPost("login")]
    public async Task<ActionResult<LoginResponse>> Login([FromBody] LoginRequest request)
    {
        var result = await _authService.LoginAsync(request.UsuarioNombre, request.Password);

        if (result is null)
            return Unauthorized(new { Mensaje = "Credenciales inválidas o usuario inactivo." });

        return Ok(result);
    }
}

/// <summary>
/// Request DTO for login. Inline in controller following project convention.
/// </summary>
public record LoginRequest(string UsuarioNombre, string Password);
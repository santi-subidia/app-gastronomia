using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
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
    /// Autentica un usuario y devuelve un token JWT.
    /// Devuelve 401 si las credenciales son inválidas o el usuario está inactivo.
    /// Limitado a 10 solicitudes por minuto y por IP.
    /// </summary>
    [EnableRateLimiting("LoginPolicy")]
    [AllowAnonymous]
    [HttpPost("login")]
    public async Task<ActionResult<LoginResponse>> Login([FromBody] LoginRequest request)
    {
        var result = await _authService.LoginAsync(request.UsuarioNombre, request.Password);

        if (result is null)
            return Unauthorized(new { Mensaje = "Credenciales inválidas o usuario inactivo." });

        return Ok(result);
    }
}

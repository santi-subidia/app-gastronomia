using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Authentication service contract. Handles user login and JWT token generation.
/// </summary>
public interface IAuthService
{
    /// <summary>
    /// Validates user credentials and returns a JWT token on success.
    /// Returns null if credentials are invalid, user is inactive, or user not found.
    /// </summary>
    Task<LoginResponse?> LoginAsync(string usuarioNombre, string password);
}
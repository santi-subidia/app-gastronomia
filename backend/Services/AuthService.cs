using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Models;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace ApiGastronomia.Services;

/// <summary>
/// Authentication service that validates credentials against the database
/// and generates JWT tokens for valid users.
/// </summary>
public class AuthService : IAuthService
{
    private readonly AppDbContext _context;
    private readonly JwtSettings _jwtSettings;

    public AuthService(AppDbContext context, JwtSettings jwtSettings)
    {
        _context = context;
        _jwtSettings = jwtSettings;
    }

    public async Task<LoginResponse?> LoginAsync(string usuarioNombre, string password)
    {
        // Find user by username, including their role
        var user = await _context.Usuarios
            .Include(u => u.Rol)
            .FirstOrDefaultAsync(u => u.UsuarioNombre == usuarioNombre);

        // User not found → null (controller returns 401)
        if (user is null)
            return null;

        // Inactive user -> null (controller returns 401)
        if (!user.Activo)
            return null;

        // Verify password against BCrypt hash
        if (!BCrypt.Net.BCrypt.Verify(password, user.PasswordHash))
            return null;

        // Generate JWT token with claims
        var token = GenerateJwtToken(user);

        return new LoginResponse(
            Id: user.Id,
            UsuarioNombre: user.UsuarioNombre,
            RolId: user.RolId,
            RolNombre: user.Rol.Nombre,
            Token: token,
            ExpiraEn: DateTime.UtcNow.AddMinutes(_jwtSettings.ExpiryMinutes)
        );
    }

    private string GenerateJwtToken(Domain.Entities.Usuario user)
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_jwtSettings.SecretKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.Id.ToString()),
            new(JwtRegisteredClaimNames.UniqueName, user.UsuarioNombre),
            new("role", user.Rol.Nombre),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
        };

        var token = new JwtSecurityToken(
            issuer: _jwtSettings.Issuer,
            audience: _jwtSettings.Audience,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(_jwtSettings.ExpiryMinutes),
            signingCredentials: credentials
        );

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
using System.ComponentModel.DataAnnotations;

namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTOs para autenticación y gestión de usuarios.
/// Todos los DTOs (request y response) centralizados aquí por entidad.
/// </summary>

/// <summary>
/// Respuesta exitosa del login. Incluye token JWT y datos del usuario.
/// </summary>
public record LoginResponse(
    int Id,
    string UsuarioNombre,
    int RolId,
    string RolNombre,
    string Token,
    DateTime ExpiraEn
);

/// <summary>
/// DTO de respuesta para usuarios. Nunca expone PasswordHash.
/// </summary>
public record UsuarioResponse(
    int Id,
    string UsuarioNombre,
    int RolId,
    string RolNombre,
    bool Disponible,
    bool Activo,
    bool FueraDeServicio = false
);

/// <summary>
/// Request DTO for login.
/// </summary>
public record LoginRequest(string UsuarioNombre, [MinLength(6)] string Password);

/// <summary>
/// Request DTO for creating a user.
/// </summary>
public record CreateUserRequest(string UsuarioNombre, [MinLength(6)] string Password, int RolId);

/// <summary>
/// Request DTO for updating a user. All fields are optional (partial update).
/// </summary>
public record UpdateUserRequest(string? UsuarioNombre, [MinLength(6)] string? Password, int? RolId, bool? Disponible, bool? FueraDeServicio = null);

namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTOs para autenticación y gestión de usuarios.
/// Los request DTOs específicos de cada endpoint viven inline en su controller.
/// Los DTOs compartidos entre múltiples controllers van aquí.
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
    bool Activo
);
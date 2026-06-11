using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Services.Interfaces;

/// <summary>
/// Usuario CRUD service contract. Returns DTOs to avoid exposing PasswordHash.
/// Soft delete (Activo = false) is the only way to deactivate users.
/// </summary>
public interface IUsuarioService
{
    /// <summary>
    /// Returns all active users (Activo == true), including their Rol navigation.
    /// Inactive users are excluded from the result.
    /// </summary>
    Task<IEnumerable<UsuarioResponse>> ObtenerUsuariosAsync();

    /// <summary>
    /// Returns a single user by ID with Rol navigation, or null if not found.
    /// </summary>
    Task<UsuarioResponse?> ObtenerUsuarioPorIdAsync(int id);

    /// <summary>
    /// Creates a new user with BCrypt-hashed password.
    /// Throws InvalidOperationException if username already exists.
    /// </summary>
    Task<UsuarioResponse> CrearUsuarioAsync(string usuarioNombre, string password, int rolId);

    /// <summary>
    /// Updates specific user fields. Only non-null parameters are updated.
    /// If password is provided, it is re-hashed with BCrypt.
    /// Activo cannot be changed via this method (use EliminarUsuarioAsync for soft delete).
    /// Returns null if user not found.
    /// </summary>
    Task<UsuarioResponse?> ActualizarUsuarioAsync(int id, string? usuarioNombre, string? password, int? rolId, bool? disponible);

    /// <summary>
    /// Soft deletes a user by setting Activo = false.
    /// Returns true if the user was found and deactivated, false if not found.
    /// </summary>
    Task<bool> EliminarUsuarioAsync(int id);
}
namespace ApiGastronomia.Models;

/// <summary>
/// Configuración de autenticación JWT enlazada con la sección "JwtSettings".
/// </summary>
public class JwtSettings
{
    public string Issuer { get; set; } = "ApiGastronomia";
    public string Audience { get; set; } = "ApiGastronomiaClients";
    public string SecretKey { get; set; } = string.Empty;
    public int ExpiryMinutes { get; set; } = 480; // Ocho horas por defecto.
}

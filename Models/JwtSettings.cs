namespace ApiGastronomia.Models;

/// <summary>
/// Configuration model for JWT authentication settings.
/// Binds from the "JwtSettings" section in appsettings.json.
/// </summary>
public class JwtSettings
{
    public string Issuer { get; set; } = "ApiGastronomia";
    public string Audience { get; set; } = "ApiGastronomiaClients";
    public string SecretKey { get; set; } = string.Empty;
    public int ExpiryMinutes { get; set; } = 480; // 8 hours default
}
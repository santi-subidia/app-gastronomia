using System.Net;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text;
using ApiGastronomia.Tests.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.IdentityModel.Tokens;

namespace ApiGastronomia.Tests.Pipeline;

/// <summary>
/// Integration tests for the global authentication gate.
/// Validates that the fallback authorization policy requires authentication
/// on all controller endpoints unless explicitly exempted with [AllowAnonymous].
/// </summary>
public class GlobalAuthGateTests
{
    private const string JwtSecretKey = RateLimitingWebApplicationFactory.JwtSecretKey;

    /// <summary>
    /// Generates a valid JWT token for testing authenticated scenarios.
    /// </summary>
    private static string GenerateJwtToken(string userId, string role)
    {
        var claims = new List<Claim>
        {
            new Claim("sub", userId),
            new Claim("jti", Guid.NewGuid().ToString()),
            new Claim("http://schemas.microsoft.com/ws/2008/06/identity/claims/role", role)
        };

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(JwtSecretKey));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

        var tokenHandler = new System.IdentityModel.Tokens.Jwt.JwtSecurityTokenHandler();
        var token = new System.IdentityModel.Tokens.Jwt.JwtSecurityToken(
            issuer: "ApiGastronomia",
            audience: "ApiGastronomiaClients",
            claims: claims,
            expires: DateTime.UtcNow.AddHours(1),
            signingCredentials: credentials
        );

        return tokenHandler.WriteToken(token);
    }

    // =========================================================
    // Scenario: Unauthenticated GET request receives 401
    // =========================================================
    [Fact]
    public async Task Unauthenticated_GetPedidos_Returns401()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var client = factory.CreateClient();

        // Act: no Authorization header
        var response = await client.GetAsync("/api/pedidos");

        // Assert: fallback policy rejects unauthenticated requests
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    // =========================================================
    // Scenario: Unauthenticated POST request receives 401
    // =========================================================
    [Fact]
    public async Task Unauthenticated_PostPedido_Returns401()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var client = factory.CreateClient();

        // Act: no Authorization header
        var response = await client.PostAsJsonAsync("/api/pedidos", new { });

        // Assert: fallback policy rejects unauthenticated requests on all verbs
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    // =========================================================
    // Scenario: Login endpoint reachable without token ([AllowAnonymous])
    // The controller returns 401 for invalid credentials, but the response
    // body proves the request REACHED the controller — not blocked by the auth gate.
    // =========================================================
    [Fact]
    public async Task Login_WithoutToken_NotBlockedByAuthGate()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var client = factory.CreateClient();
        var loginPayload = new { UsuarioNombre = "nonexistent", Password = "invalid123456" };

        // Act: POST login without Authorization header
        var response = await client.PostAsJsonAsync("/api/auth/login", loginPayload);

        // Assert: the request must reach the controller, not be blocked by the auth gate.
        // The controller returns 401 for invalid credentials with this body:
        // {"mensaje":"Credenciales inválidas o usuario inactivo."}
        // If the fallback policy blocked this endpoint, we'd get an empty 401
        // with WWW-Authenticate header instead.
        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains("Credenciales", body);
    }

    // =========================================================
    // Scenario: Authenticated request passes the fallback policy
    // =========================================================
    [Fact]
    public async Task Authenticated_GetPedidos_NotRejectedByAuthGate()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var client = factory.CreateClient();
        var token = GenerateJwtToken($"user-auth-{Guid.NewGuid():N}", "Cocina");

        // Act: request with valid JWT
        using var request = new HttpRequestMessage(HttpMethod.Get, "/api/pedidos");
        request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        var response = await client.SendAsync(request);

        // Assert: authenticated request should pass the fallback policy (not 401)
        Assert.NotEqual(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    // =========================================================
    // Scenario: Wrong role receives 403 on role-restricted endpoint
    // =========================================================
    [Fact]
    public async Task WrongRole_AdminEndpoint_Returns403()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var client = factory.CreateClient();
        // Use a non-Admin role — UsuariosController DELETE requires Admin
        var token = GenerateJwtToken($"user-repartidor-{Guid.NewGuid():N}", "Repartidor");

        // Act: DELETE /api/usuarios/{id} with Repartidor-role JWT
        using var request = new HttpRequestMessage(HttpMethod.Delete, "/api/usuarios/999");
        request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        var response = await client.SendAsync(request);

        // Assert: authenticated but wrong role → 403 Forbidden (not 401)
        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }
}

/// <summary>
/// WebApplicationFactory that properly configures JWT authentication for integration tests.
/// Extends RateLimitingWebApplicationFactory (InMemory DB, no Redis) and adds
/// PostConfigure for JwtBearerOptions to ensure the test signing key is used
/// for token validation regardless of Program.cs configuration order.
/// </summary>
public class AuthGateWebApplicationFactory : RateLimitingWebApplicationFactory
{
    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        base.ConfigureWebHost(builder);

        builder.ConfigureServices(services =>
        {
            // PostConfigure ensures our test signing key is used for JWT validation,
            // overriding whatever TokenValidationParameters Program.cs configured.
            // This is needed because Program.cs binds JwtSettings from configuration
            // and uses it in AddJwtBearer BEFORE WebApplicationFactory.ConfigureServices runs.
            services.PostConfigure<Microsoft.AspNetCore.Authentication.JwtBearer.JwtBearerOptions>(
                Microsoft.AspNetCore.Authentication.JwtBearer.JwtBearerDefaults.AuthenticationScheme,
                options =>
                {
                    options.TokenValidationParameters = new TokenValidationParameters
                    {
                        ValidateIssuer = true,
                        ValidateAudience = true,
                        ValidateLifetime = true,
                        ValidateIssuerSigningKey = true,
                        ValidIssuer = "ApiGastronomia",
                        ValidAudience = "ApiGastronomiaClients",
                        IssuerSigningKey = new SymmetricSecurityKey(
                            Encoding.UTF8.GetBytes(JwtSecretKey))
                    };
                });
        });
    }
}
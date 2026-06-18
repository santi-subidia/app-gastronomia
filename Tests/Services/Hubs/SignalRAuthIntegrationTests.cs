using System.Net;
using System.Security.Claims;
using System.Text;
using ApiGastronomia.Tests.Pipeline;
using Microsoft.AspNetCore.Http.Connections;
using Microsoft.AspNetCore.SignalR.Client;
using Microsoft.IdentityModel.Tokens;

namespace ApiGastronomia.Tests.Services.Hubs;

/// <summary>
/// Integration tests for SignalR Hub authentication.
/// Validates that the SignalR endpoint enforces JWT authentication
/// for WebSocket connections, consistent with the global auth gate.
/// </summary>
public class SignalRAuthIntegrationTests
{
    private const string JwtSecretKey = RateLimitingWebApplicationFactory.JwtSecretKey;

    /// <summary>
    /// Generates a valid JWT token for testing authenticated SignalR connections.
    /// Uses the same secret key, issuer, and audience as the test server.
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
    // Scenario: Authenticated SignalR connection succeeds
    // GIVEN a valid JWT passed as access token
    // WHEN a connection is made to /hubs/logistica
    // THEN the connection succeeds (HubConnectionState.Connected)
    // =========================================================
    [Fact]
    public async Task Authenticated_SignalRConnection_Succeeds()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();
        var token = GenerateJwtToken($"user-hub-{Guid.NewGuid():N}", "Cocina");

        var connection = new HubConnectionBuilder()
            .WithUrl($"{factory.Server.BaseAddress}hubs/logistica", options =>
            {
                options.HttpMessageHandlerFactory = _ => factory.Server.CreateHandler();
                options.Transports = HttpTransportType.LongPolling;
                options.AccessTokenProvider = () => Task.FromResult(token)!;
            })
            .Build();

        try
        {
            // Act
            await connection.StartAsync();

            // Assert: connection must reach Connected state
            Assert.Equal(HubConnectionState.Connected, connection.State);
        }
        finally
        {
            await connection.StopAsync();
        }
    }

    // =========================================================
    // Scenario: Unauthenticated SignalR connection rejected
    // GIVEN no JWT in the query string
    // WHEN a connection is attempted to /hubs/logistica
    // THEN the connection is rejected with 401 Unauthorized
    // =========================================================
    [Fact]
    public async Task Unauthenticated_SignalRConnection_RejectedWith401()
    {
        // Arrange
        using var factory = new AuthGateWebApplicationFactory();

        var connection = new HubConnectionBuilder()
            .WithUrl($"{factory.Server.BaseAddress}hubs/logistica", options =>
            {
                options.HttpMessageHandlerFactory = _ => factory.Server.CreateHandler();
                options.Transports = HttpTransportType.LongPolling;
                // No AccessTokenProvider — no JWT sent
            })
            .Build();

        try
        {
            // Act + Assert: StartAsync must throw HttpRequestException with 401
            var ex = await Assert.ThrowsAsync<HttpRequestException>(() => connection.StartAsync());

            Assert.Equal(HttpStatusCode.Unauthorized, ex.StatusCode);
        }
        finally
        {
            await connection.StopAsync();
        }
    }
}
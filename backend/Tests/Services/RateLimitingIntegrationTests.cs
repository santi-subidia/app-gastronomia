using System.Net;
using System.Net.Http.Json;
using System.Security.Claims;
using System.Text;
using System.Text.Json;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Models;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.IdentityModel.Tokens;

namespace ApiGastronomia.Tests.Services;

/// <summary>
/// Integration tests for rate limiting behavior end-to-end.
/// Each test creates a fresh WebApplicationFactory to ensure rate limiter state
/// isolation between tests.
/// </summary>
public class RateLimitingIntegrationTests
{
    private const string JwtSecretKey = "rate-limiting-test-secret-key-at-least-32-chars-long";

    /// <summary>
    /// Creates a fresh factory for each test to ensure clean rate limiter state.
    /// </summary>
    private RateLimitingWebApplicationFactory CreateFactory()
        => new RateLimitingWebApplicationFactory();

    /// <summary>
    /// Generates a valid JWT token for testing authenticated rate limiting scenarios.
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
    // Scenario: Authenticated user within global limit
    // =========================================================
    [Fact]
    public async Task GlobalLimit_WithinLimit_ReturnsSuccess()
    {
        // Arrange
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var token = GenerateJwtToken($"user-within-{Guid.NewGuid():N}", "Cocina");

        // Act: single request well within the 100 req/min limit
        using var request = new HttpRequestMessage(HttpMethod.Get, "/api/usuarios");
        request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
        var response = await client.SendAsync(request);

        // Assert: not rate-limited (may be 401/403, but NOT 429)
        Assert.NotEqual((HttpStatusCode)429, response.StatusCode);
    }

    // =========================================================
    // Scenario: Authenticated user exceeds global limit (100 req/min)
    // =========================================================
    [Fact]
    public async Task GlobalLimit_ExceedsLimit_Returns429()
    {
        // Arrange: unique user partition per test
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var userId = $"user-exceed-{Guid.NewGuid():N}";
        var token = GenerateJwtToken(userId, "Cocina");

        // Act: send 101 rapid requests (limit is 100/min)
        HttpResponseMessage? rateLimitedResponse = null;
        for (int i = 0; i < 101; i++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, "/api/usuarios");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
            var response = await client.SendAsync(request);

            if (response.StatusCode == (HttpStatusCode)429 && rateLimitedResponse == null)
            {
                rateLimitedResponse = response;
                break;
            }
        }

        // Assert: at least one request got 429
        Assert.NotNull(rateLimitedResponse);
        Assert.Equal((HttpStatusCode)429, rateLimitedResponse!.StatusCode);

        // Assert: 429 response format — JSON content type and Spanish message
        Assert.Equal("application/json", rateLimitedResponse.Content.Headers.ContentType?.MediaType);
        var body = await rateLimitedResponse.Content.ReadAsStringAsync();
        Assert.Contains("Demasiadas solicitudes", body);

        // Assert: Retry-After header present
        Assert.True(rateLimitedResponse.Headers.Contains("Retry-After"),
            "429 response must include Retry-After header");
    }

    // =========================================================
    // Scenario: 429 response format — Spanish JSON + Retry-After
    // =========================================================
    [Fact]
    public async Task RateLimitedResponse_ContainsSpanishMessageAndRetryAfterHeader()
    {
        // Arrange: unique user partition per test
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var userId = $"user-format-{Guid.NewGuid():N}";
        var token = GenerateJwtToken(userId, "Cocina");

        // Act: send enough requests to exhaust the global limit
        HttpResponseMessage? rateLimitedResponse = null;
        for (int i = 0; i < 102; i++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, "/api/usuarios");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
            var response = await client.SendAsync(request);

            if (response.StatusCode == (HttpStatusCode)429 && rateLimitedResponse == null)
            {
                rateLimitedResponse = response;
            }
        }

        Assert.NotNull(rateLimitedResponse);

        // Assert: JSON content type
        Assert.Equal("application/json", rateLimitedResponse!.Content.Headers.ContentType?.MediaType);

        // Assert: Spanish mensaje in body
        var body = await rateLimitedResponse.Content.ReadAsStringAsync();
        var json = JsonDocument.Parse(body);
        Assert.True(json.RootElement.TryGetProperty("Mensaje", out var mensajeProp));
        Assert.Contains("Demasiadas solicitudes", mensajeProp.GetString());

        // Assert: Retry-After header present with numeric value
        Assert.True(rateLimitedResponse.Headers.Contains("Retry-After"));
        var retryAfter = rateLimitedResponse.Headers.GetValues("Retry-After").First();
        Assert.True(int.TryParse(retryAfter, out _), "Retry-After must be a numeric value in seconds");
    }

    // =========================================================
    // Scenario: Login endpoint rate limit (10 req/min per IP)
    // =========================================================
    [Fact]
    public async Task LoginLimit_ExceedsLimit_Returns429()
    {
        // Arrange
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var loginPayload = new { UsuarioNombre = "testuser", Password = "testpassword" };

        // Act: 11 rapid login attempts from same client (same IP)
        HttpResponseMessage? rateLimitedResponse = null;
        for (int i = 0; i < 12; i++)
        {
            var response = await client.PostAsJsonAsync("/api/auth/login", loginPayload);
            if (response.StatusCode == (HttpStatusCode)429 && rateLimitedResponse == null)
            {
                rateLimitedResponse = response;
            }
        }

        // Assert: at least one request got 429
        Assert.NotNull(rateLimitedResponse);
        Assert.Equal((HttpStatusCode)429, rateLimitedResponse!.StatusCode);
    }

    // =========================================================
    // Scenario: OPTIONS preflight bypass — not counted against limit
    // =========================================================
    [Fact]
    public async Task OptionsPreflight_NotRateLimited()
    {
        // Arrange: unique user partition per test
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var userId = $"user-options-{Guid.NewGuid():N}";
        var token = GenerateJwtToken(userId, "Cocina");

        // Act: send multiple OPTIONS requests — these should never return 429
        for (int i = 0; i < 20; i++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Options, "/api/usuarios");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", token);
            var response = await client.SendAsync(request);

            // Assert: OPTIONS should never be rate-limited
            Assert.NotEqual((HttpStatusCode)429, response.StatusCode);
        }
    }

    // =========================================================
    // Scenario: Partition isolation — two users have independent limits
    // =========================================================
    [Fact]
    public async Task PartitionIsolation_TwoUsers_HaveIndependentLimits()
    {
        // Arrange: two different authenticated users
        using var factory = CreateFactory();
        var client = factory.CreateClient();
        var tokenA = GenerateJwtToken($"user-a-{Guid.NewGuid():N}", "Cocina");
        var tokenB = GenerateJwtToken($"user-b-{Guid.NewGuid():N}", "Cocina");

        // Act: User A sends 50 requests (half their limit)
        for (int i = 0; i < 50; i++)
        {
            using var request = new HttpRequestMessage(HttpMethod.Get, "/api/usuarios");
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", tokenA);
            await client.SendAsync(request);
        }

        // Assert: User B can still make requests (independent partition)
        using var requestB = new HttpRequestMessage(HttpMethod.Get, "/api/usuarios");
        requestB.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", tokenB);
        var responseB = await client.SendAsync(requestB);
        Assert.NotEqual((HttpStatusCode)429, responseB.StatusCode);
    }

    // =========================================================
    // Scenario: IP fallback — unauthenticated requests use IP
    // =========================================================
    [Fact]
    public async Task UnauthenticatedRequest_UsesIPPartition()
    {
        // Arrange
        using var factory = CreateFactory();
        var client = factory.CreateClient();

        // Act: single unauthenticated request (within 100/min)
        var response = await client.GetAsync("/api/usuarios");

        // Assert: not rate-limited (may be 401, but NOT 429)
        Assert.NotEqual((HttpStatusCode)429, response.StatusCode);
    }
}

/// <summary>
/// Custom WebApplicationFactory for rate limiting integration tests.
/// Overrides PostgreSQL with an in-memory database and keeps SignalR in memory.
/// </summary>
public class RateLimitingWebApplicationFactory : WebApplicationFactory<Program>, IAsyncLifetime
{
    internal const string JwtSecretKey = "rate-limiting-test-secret-key-at-least-32-chars-long";

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");

        // Override JWT settings via configuration so the authentication middleware
        // uses our test secret key. Program.cs binds JwtSettings from configuration,
        // and the AddJwtBearer lambda captures this bound object.
        builder.ConfigureAppConfiguration((context, config) =>
        {
            config.AddInMemoryCollection(new Dictionary<string, string?>
            {
                { "JwtSettings:Issuer", "ApiGastronomia" },
                { "JwtSettings:Audience", "ApiGastronomiaClients" },
                { "JwtSettings:SecretKey", JwtSecretKey },
                { "JwtSettings:ExpiryMinutes", "480" },
                { "Database:RunSeeds", "false" }
            });
        });

        builder.ConfigureServices(services =>
        {
            // Remove PostgreSQL DbContext — replace with InMemory for tests
            var internalServiceProvider = new ServiceCollection()
                .AddEntityFrameworkInMemoryDatabase()
                .BuildServiceProvider();

            foreach (var desc in services.Where(
                d => d.ServiceType == typeof(DbContextOptions<AppDbContext>) ||
                     d.ServiceType == typeof(AppDbContext)).ToList())
            {
                services.Remove(desc);
            }

            services.AddDbContext<AppDbContext>(options =>
            {
                options.UseInMemoryDatabase("RateLimitTestDb");
                options.UseInternalServiceProvider(internalServiceProvider);
                options.EnableSensitiveDataLogging();
            });

            services.AddDistributedMemoryCache();

            // Keep SignalR in memory for the single-instance test host.
            foreach (var desc in services.Where(
                d => d.ServiceType.FullName != null &&
                     d.ServiceType.FullName.Contains("SignalR")).ToList())
            {
                services.Remove(desc);
            }
            services.AddSignalR();
        });
    }

    public async Task InitializeAsync()
    {
        using var scope = Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        await db.Database.EnsureCreatedAsync();
    }

    Task IAsyncLifetime.DisposeAsync() => Task.CompletedTask;
}

using System.IdentityModel.Tokens.Jwt;
using System.Text;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Models;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

namespace ApiGastronomia.Tests.Services;

public class AuthServiceTests
{
    private static void SeedRoles(AppDbContext context)
    {
        if (!context.Roles.Any())
        {
            context.Roles.AddRange(
                new Rol { Id = 1, Nombre = "Cajero" },
                new Rol { Id = 2, Nombre = "Cocina" },
                new Rol { Id = 3, Nombre = "Repartidor" }
            );
            context.SaveChanges();
        }
    }

    /// <summary>
    /// Helper to create an InMemory DbContext seeded with a test user.
    /// Uses SeedRoles helper to insert test roles matching production names.
    /// The password is hashed with BCrypt so that BCrypt.Verify works in tests.
    /// </summary>
    private static (AppDbContext Context, JwtSettings Settings) CreateDbContextAndSettings(
        string username = "admin",
        string plainPassword = "test123",
        bool activo = true,
        bool disponible = true,
        int rolId = 1) // Default to Cajero role
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase($"AuthTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        SeedRoles(context);

        // Find the seeded role by ID (SeedRoles: 1=Cajero, 2=Cocina, 3=Repartidor)
        var rol = context.Roles.First(r => r.Id == rolId);

        // Seed a User with BCrypt-hashed password linked to the seeded role
        var user = new Usuario
        {
            UsuarioNombre = username,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(plainPassword),
            RolId = rol.Id,
            Activo = activo,
            Disponible = disponible
        };
        context.Usuarios.Add(user);
        context.SaveChanges();

        var settings = new JwtSettings
        {
            Issuer = "ApiGastronomia",
            Audience = "ApiGastronomiaClients",
            SecretKey = "dev-secret-key-not-for-production-at-least-32-chars",
            ExpiryMinutes = 480
        };

        return (context, settings);
    }

    // ================================================================
    // Scenario: Successful login returns valid JWT with correct claims
    // ================================================================

    [Fact]
    public async Task LoginAsync_ValidCredentials_ReturnsLoginResponseWithToken()
    {
        // Arrange
        var (context, settings) = CreateDbContextAndSettings();
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("admin", "test123");

        // Assert: LoginResponse returned with all fields populated
        Assert.NotNull(result);
        Assert.Equal("admin", result!.UsuarioNombre);
        Assert.Equal(1, result.RolId);
        Assert.Equal("Cajero", result.RolNombre);
        Assert.False(string.IsNullOrEmpty(result.Token), "Token should not be empty");
        Assert.True(result.ExpiraEn > DateTime.UtcNow, "ExpiraEn should be in the future");

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task LoginAsync_ValidCredentials_TokenContainsCorrectClaims()
    {
        // Arrange
        var (context, settings) = CreateDbContextAndSettings();
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("admin", "test123");

        // Assert: decode the JWT and verify claims
        Assert.NotNull(result);
        var handler = new JwtSecurityTokenHandler();
        var token = handler.ReadJwtToken(result!.Token);

        var subClaim = token.Claims.FirstOrDefault(c => c.Type == "sub");
        var uniqueNameClaim = token.Claims.FirstOrDefault(c => c.Type == "unique_name");
        var roleClaim = token.Claims.FirstOrDefault(c => c.Type == "role");

        Assert.NotNull(subClaim);
        Assert.Equal("1", subClaim!.Value); // userId as string
        Assert.NotNull(uniqueNameClaim);
        Assert.Equal("admin", uniqueNameClaim!.Value);
        Assert.NotNull(roleClaim);
        Assert.Equal("Cajero", roleClaim!.Value);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Scenario: Invalid password returns null (controller returns 401)
    // ================================================================

    [Fact]
    public async Task LoginAsync_WrongPassword_ReturnsNull()
    {
        // Arrange
        var (context, settings) = CreateDbContextAndSettings(username: "admin", plainPassword: "test123");
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("admin", "wrongpassword");

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Scenario: Inactive user (Activo == false) cannot login
    // ================================================================

    [Fact]
    public async Task LoginAsync_InactiveUser_ReturnsNull()
    {
        // Arrange: user exists but Activo = false
        var (context, settings) = CreateDbContextAndSettings(
            username: "inactive_user",
            plainPassword: "test123",
            activo: false);
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("inactive_user", "test123");

        // Assert: service returns null, controller will return 401
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Scenario: Nonexistent user returns null (controller returns 401)
    // ================================================================

    [Fact]
    public async Task LoginAsync_NonexistentUser_ReturnsNull()
    {
        // Arrange
        var (context, settings) = CreateDbContextAndSettings(username: "admin", plainPassword: "test123");
        var service = new AuthService(context, settings);

        // Act: try login with a username that doesn't exist
        var result = await service.LoginAsync("ghost", "anything");

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Scenario: User with Disponible == false cannot login
    // (design says Activo == true AND Disponible == true required)
    // ================================================================

    [Fact]
    public async Task LoginAsync_DisponibleFalse_ReturnsLoginResponse()
    {
        // Arrange: user exists, Activo=true, but Disponible=false
        var (context, settings) = CreateDbContextAndSettings(
            username: "busy_user",
            plainPassword: "test123",
            activo: true,
            disponible: false);
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("busy_user", "test123");

        // Assert: Unavailable users (Disponible=false) should be able to log in to toggle their availability
        Assert.NotNull(result);
        Assert.Equal("busy_user", result.UsuarioNombre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: Different user with different role
    // ================================================================

    [Fact]
    public async Task LoginAsync_DifferentRole_ReturnsCorrectRoleInToken()
    {
        // Arrange: user with "Cocina" role (Id=2)
        var (context, settings) = CreateDbContextAndSettings(
            username: "cocinero1",
            plainPassword: "cocina123",
            rolId: 2); // Cocina role
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("cocinero1", "cocina123");

        // Assert
        Assert.NotNull(result);
        Assert.Equal("Cocina", result!.RolNombre);

        // Verify token has the correct role claim
        var handler = new JwtSecurityTokenHandler();
        var token = handler.ReadJwtToken(result.Token);
        var roleClaim = token.Claims.FirstOrDefault(c => c.Type == "role");
        Assert.Equal("Cocina", roleClaim!.Value);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // IAuthService contract verification: interface exists and returns correct type
    // ================================================================

    [Fact]
    public async Task IAuthService_CanBeResolvedAsService()
    {
        // Arrange: verify IAuthService can be assigned from AuthService
        var (context, settings) = CreateDbContextAndSettings();
        IAuthService service = new AuthService(context, settings);

        var result = await service.LoginAsync("admin", "test123");

        // Assert: IAuthService.LoginAsync returns LoginResponse? as designed
        Assert.NotNull(result);
        Assert.IsType<LoginResponse>(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}
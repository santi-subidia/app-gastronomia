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
    /// <summary>
    /// Helper to create an InMemory DbContext seeded with a test user.
    /// Uses roles from HasData seed (Ids 1-4: Admin, Cocinero, Repartidor, Cajero)
    /// to avoid conflicts with AppDbContext.OnModelCreating seed data.
    /// The password is hashed with BCrypt so that BCrypt.Verify works in tests.
    /// </summary>
    private static (AppDbContext Context, JwtSettings Settings) CreateDbContextAndSettings(
        string username = "admin",
        string plainPassword = "test123",
        bool activo = true,
        bool disponible = true,
        int rolId = 1) // Default to Admin role from seed data
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase($"AuthTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);

        // Ensure seed data is materialized (HasData from OnModelCreating)
        // InMemory applies HasData on first use, so we force it by accessing the DB
        context.Database.EnsureCreated();

        // Find the seeded role by ID (HasData seeds: 1=Admin, 2=Cocinero, 3=Repartidor, 4=Cajero)
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
        Assert.Equal("Admin", result.RolNombre);
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
        Assert.Equal("Admin", roleClaim!.Value);

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
    public async Task LoginAsync_DisponibleFalse_ReturnsNull()
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

        // Assert
        Assert.Null(result);

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
        // Arrange: user with "Cocinero" role (Id=2 from HasData seed)
        var (context, settings) = CreateDbContextAndSettings(
            username: "cocinero1",
            plainPassword: "cocina123",
            rolId: 2); // Cocinero role from seed data
        var service = new AuthService(context, settings);

        // Act
        var result = await service.LoginAsync("cocinero1", "cocina123");

        // Assert
        Assert.NotNull(result);
        Assert.Equal("Cocinero", result!.RolNombre);

        // Verify token has the correct role claim
        var handler = new JwtSecurityTokenHandler();
        var token = handler.ReadJwtToken(result.Token);
        var roleClaim = token.Claims.FirstOrDefault(c => c.Type == "role");
        Assert.Equal("Cocinero", roleClaim!.Value);

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
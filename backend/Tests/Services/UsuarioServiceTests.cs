using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Moq;

namespace ApiGastronomia.Tests.Services;

public class UsuarioServiceTests
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
    /// Helper to create an InMemory DbContext with seeded roles.
    /// Uses SeedRoles helper to insert test roles matching production names.
    /// </summary>
    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"UsuarioTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        SeedRoles(context);
        return context;
    }

    /// <summary>
    /// Seeds a user with BCrypt-hashed password and a valid role from SeedRoles.
    /// Returns (context, user) so tests can reference the seeded entity.
    /// </summary>
    private static (AppDbContext Context, Usuario User) SeedUser(
        AppDbContext context,
        string username = "testuser",
        string plainPassword = "test123",
        int rolId = 1,
        bool activo = true,
        bool disponible = true)
    {
        var rol = context.Roles.First(r => r.Id == rolId);

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

        return (context, user);
    }

    private static Mock<IHubContext<LogisticaHub>> CreateMockHubContext()
    {
        var mockClients = new Mock<IHubClients>();
        var mockProxy = new Mock<IClientProxy>();
        mockProxy
            .Setup(p => p.SendCoreAsync(It.IsAny<string>(), It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        mockClients.Setup(c => c.Group(It.IsAny<string>())).Returns(mockProxy.Object);
        var mockHub = new Mock<IHubContext<LogisticaHub>>();
        mockHub.Setup(h => h.Clients).Returns(mockClients.Object);
        return mockHub;
    }

    // ================================================================
    // IUsuarioService contract: interface can be resolved from implementation
    // ================================================================

    [Fact]
    public async Task IUsuarioService_CanBeResolvedFromImplementation()
    {
        // Arrange: verify IUsuarioService can be assigned from UsuarioService
        var context = CreateDbContext();
        IUsuarioService service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act: call ObtenerUsuariosAsync through the interface
        var result = await service.ObtenerUsuariosAsync();

        // Assert: interface contract works
        Assert.NotNull(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerUsuariosAsync: returns all ACTIVE users with Rol, excludes inactive
    // ================================================================

    [Fact]
    public async Task ObtenerUsuariosAsync_ReturnsOnlyActiveUsers()
    {
        // Arrange: seed one active and one inactive user
        var context = CreateDbContext();
        SeedUser(context, username: "active_user", plainPassword: "pass1", activo: true);
        SeedUser(context, username: "inactive_user", plainPassword: "pass2", activo: false);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.ObtenerUsuariosAsync();

        // Assert: only the active user is returned
        var list = result.ToList();
        Assert.Single(list);
        Assert.Equal("active_user", list[0].UsuarioNombre);
        Assert.True(list[0].Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerUsuariosAsync_IncludesRolNavigation()
    {
        // Arrange: seed user with Cajero role (Id=1)
        var context = CreateDbContext();
        SeedUser(context, username: "admin_user", plainPassword: "pass123", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.ObtenerUsuariosAsync();

        // Assert: RolNombre is populated from the navigation property
        var user = result.First();
        Assert.Equal("Cajero", user.RolNombre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerUsuariosAsync_WithRoleFilter_ReturnsOnlyMatchingRole()
    {
        var context = CreateDbContext();
        SeedUser(context, username: "cashier", plainPassword: "pass123", rolId: 1);
        SeedUser(context, username: "driver", plainPassword: "pass123", rolId: 3);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        var result = await service.ObtenerUsuariosAsync("Repartidor");

        var users = result.ToList();
        Assert.Single(users);
        Assert.Equal("driver", users[0].UsuarioNombre);
        Assert.Equal("Repartidor", users[0].RolNombre);

        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerUsuarioPorIdAsync: returns user when found, null when not
    // ================================================================

    [Fact]
    public async Task ObtenerUsuarioPorIdAsync_ExistingUser_ReturnsUsuarioResponse()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, user) = SeedUser(context, username: "findme", plainPassword: "pass123", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.ObtenerUsuarioPorIdAsync(user.Id);

        // Assert: user found with correct fields and no PasswordHash
        Assert.NotNull(result);
        Assert.Equal(user.Id, result!.Id);
        Assert.Equal("findme", result.UsuarioNombre);
        Assert.Equal(1, result.RolId);
        Assert.Equal("Cajero", result.RolNombre);
        Assert.True(result.Disponible);
        Assert.True(result.Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerUsuarioPorIdAsync_NonexistentUser_ReturnsNull()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act: search for an ID that doesn't exist
        var result = await service.ObtenerUsuarioPorIdAsync(999);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // CrearUsuarioAsync: hashes password, creates user, returns DTO without PasswordHash
    // ================================================================

    [Fact]
    public async Task CrearUsuarioAsync_ValidData_CreatesUserAndHashesPassword()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.CrearUsuarioAsync("newuser", "securepass123", rolId: 1);

        // Assert: response DTO has correct fields
        Assert.Equal("newuser", result.UsuarioNombre);
        Assert.Equal(1, result.RolId);
        Assert.Equal("Cajero", result.RolNombre);
        Assert.True(result.Activo);
        Assert.True(result.Disponible);

        // Assert: password was hashed with BCrypt in the database
        var savedUser = await context.Usuarios.FirstAsync(u => u.UsuarioNombre == "newuser");
        Assert.True(BCrypt.Net.BCrypt.Verify("securepass123", savedUser.PasswordHash));
        Assert.StartsWith("$2", savedUser.PasswordHash); // BCrypt format

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearUsuarioAsync_DuplicateUsername_ThrowsInvalidOperationException()
    {
        // Arrange: seed existing user
        var context = CreateDbContext();
        SeedUser(context, username: "duplicate_user", plainPassword: "pass1", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act + Assert: creating user with same username throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearUsuarioAsync("duplicate_user", "different_pass", rolId: 2));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ActualizarUsuarioAsync: updates only provided fields, re-hashes password
    // ================================================================

    [Fact]
    public async Task ActualizarUsuarioAsync_UpdatePassword_RehashesWithBCrypt()
    {
        // Arrange: seed user with known password
        var context = CreateDbContext();
        var (_, user) = SeedUser(context, username: "updateme", plainPassword: "oldpass", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act: update only the password
        var result = await service.ActualizarUsuarioAsync(
            user.Id,
            usuarioNombre: null,
            password: "newpass456",
            rolId: null,
            disponible: null);

        // Assert: response DTO reflects updated user
        Assert.NotNull(result);
        Assert.Equal(user.Id, result!.Id);
        Assert.Equal("updateme", result.UsuarioNombre); // unchanged

        // Assert: password was re-hashed
        var updatedUser = await context.Usuarios.FindAsync(user.Id);
        Assert.True(BCrypt.Net.BCrypt.Verify("newpass456", updatedUser!.PasswordHash));
        Assert.False(BCrypt.Net.BCrypt.Verify("oldpass", updatedUser.PasswordHash)); // old hash gone

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarUsuarioAsync_PartialUpdate_OnlyChangesProvidedFields()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, user) = SeedUser(context, username: "patchme", plainPassword: "pass123", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act: update only UsuarioNombre and Disponible, leave password and rolId unchanged
        var result = await service.ActualizarUsuarioAsync(
            user.Id,
            usuarioNombre: "patched_name",
            password: null,
            rolId: null,
            disponible: false);

        // Assert: only the specified fields changed
        Assert.NotNull(result);
        Assert.Equal("patched_name", result!.UsuarioNombre);
        Assert.False(result.Disponible);
        Assert.Equal(1, result.RolId); // unchanged
        Assert.True(result.Activo); // unchanged

        // Assert: password was NOT changed
        var updatedUser = await context.Usuarios.FindAsync(user.Id);
        Assert.True(BCrypt.Net.BCrypt.Verify("pass123", updatedUser!.PasswordHash));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarUsuarioAsync_NonexistentUser_ReturnsNull()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.ActualizarUsuarioAsync(
            999, usuarioNombre: "ghost", password: null, rolId: null, disponible: null);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // EliminarUsuarioAsync: soft delete sets Activo = false
    // ================================================================

    [Fact]
    public async Task EliminarUsuarioAsync_ExistingUser_SetsActivoFalse()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, user) = SeedUser(context, username: "deleteme", plainPassword: "pass123", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.EliminarUsuarioAsync(user.Id);

        // Assert: soft delete returns true
        Assert.True(result);

        // Assert: user still exists in DB but Activo = false
        var deletedUser = await context.Usuarios.FindAsync(user.Id);
        Assert.NotNull(deletedUser);
        Assert.False(deletedUser!.Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarUsuarioAsync_NonexistentUser_ReturnsFalse()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.EliminarUsuarioAsync(999);

        // Assert
        Assert.False(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: Multiple scenarios exercising different code paths
    // ================================================================

    [Fact]
    public async Task CrearUsuarioAsync_WithDifferentRole_ReturnsCorrectRolNombre()
    {
        // Arrange: create user with Cocina role (Id=2)
        var context = CreateDbContext();
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act
        var result = await service.CrearUsuarioAsync("cocinero1", "chefpass", rolId: 2);

        // Assert: role name matches from SeedRoles helper
        Assert.Equal("Cocina", result.RolNombre);
        Assert.Equal(2, result.RolId);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarUsuarioAsync_SoftDeletedUser_ExcludedFromGetAll()
    {
        // Arrange: seed two users, delete one
        var context = CreateDbContext();
        SeedUser(context, username: "visible_user", plainPassword: "pass1", rolId: 1);
        var (_, inactiveUser) = SeedUser(context, username: "to_delete", plainPassword: "pass2", rolId: 1);
        var service = new UsuarioService(context, CreateMockHubContext().Object, new LoggerFactory().CreateLogger<UsuarioService>());

        // Act: soft delete one user
        await service.EliminarUsuarioAsync(inactiveUser.Id);

        // Assert: GetAll only returns the active user
        var allUsers = await service.ObtenerUsuariosAsync();
        var list = allUsers.ToList();
        Assert.Single(list);
        Assert.Equal("visible_user", list[0].UsuarioNombre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}

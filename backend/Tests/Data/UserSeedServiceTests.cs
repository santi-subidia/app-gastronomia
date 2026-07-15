using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class UserSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    /// <summary>
    /// Pre-seeds the three required roles into the InMemory context.
    /// UserSeed depends on roles existing for FK resolution.
    /// </summary>
    private static async Task SeedRolesAsync(AppDbContext context)
    {
        context.Roles.AddRange(
            new Rol { Nombre = "Cajero" },
            new Rol { Nombre = "Cocina" },
            new Rol { Nombre = "Repartidor" }
        );
        await context.SaveChangesAsync();
    }

    // ---- Scenario: Fresh database — 3 users inserted with hashed passwords ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsThreeUsers()
    {
        // Arrange
        var dbName = $"UserSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var users = await context.Usuarios.ToListAsync();
        Assert.Equal(3, users.Count);

        var cajero = users.First(u => u.UsuarioNombre == "cajero1");
        Assert.True(BCrypt.Net.BCrypt.Verify("Gastronomia2026!", cajero.PasswordHash));
        Assert.True(cajero.Disponible);
        Assert.True(cajero.Activo);

        var cocina = users.First(u => u.UsuarioNombre == "cocina1");
        Assert.True(BCrypt.Net.BCrypt.Verify("Gastronomia2026!", cocina.PasswordHash));

        var repartidor = users.First(u => u.UsuarioNombre == "repartidor1");
        Assert.True(BCrypt.Net.BCrypt.Verify("Gastronomia2026!", repartidor.PasswordHash));
    }

    // ---- Scenario: Hash is valid BCrypt and NOT plaintext ----

    [Fact]
    public async Task SeedAsync_PasswordHash_IsBcryptNotPlaintext()
    {
        // Arrange
        var dbName = $"UserSeed_Hash_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var user = await context.Usuarios.FirstAsync(u => u.UsuarioNombre == "cajero1");
        Assert.NotEqual("Gastronomia2026!", user.PasswordHash);
        Assert.StartsWith("$2", user.PasswordHash); // BCrypt hash prefix
    }

    // ---- Scenario: Pre-existing users — no duplicates ----

    [Fact]
    public async Task SeedAsync_AllUsersExist_NoNewInserts()
    {
        // Arrange
        var dbName = $"UserSeed_PreExisting_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        // Pre-seed all three users
        var cajeroRole = await context.Roles.FirstAsync(r => r.Nombre == "Cajero");
        var cocinaRole = await context.Roles.FirstAsync(r => r.Nombre == "Cocina");
        var repartidorRole = await context.Roles.FirstAsync(r => r.Nombre == "Repartidor");
        context.Usuarios.AddRange(
            new Usuario { UsuarioNombre = "cajero1", PasswordHash = "hash1", RolId = cajeroRole.Id, Disponible = true, Activo = true },
            new Usuario { UsuarioNombre = "cocina1", PasswordHash = "hash2", RolId = cocinaRole.Id, Disponible = true, Activo = true },
            new Usuario { UsuarioNombre = "repartidor1", PasswordHash = "hash3", RolId = repartidorRole.Id, Disponible = true, Activo = true }
        );
        await context.SaveChangesAsync();

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var users = await context.Usuarios.ToListAsync();
        Assert.Equal(3, users.Count); // no new inserts
        Assert.Equal("hash1", users.First(u => u.UsuarioNombre == "cajero1").PasswordHash); // not overwritten
    }

    // ---- Scenario: Partial users — missing inserted ----

    [Fact]
    public async Task SeedAsync_PartialUsers_OnlyMissingInserted()
    {
        // Arrange
        var dbName = $"UserSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        var cajeroRole = await context.Roles.FirstAsync(r => r.Nombre == "Cajero");
        context.Usuarios.Add(
            new Usuario { UsuarioNombre = "cajero1", PasswordHash = "existing", RolId = cajeroRole.Id, Disponible = true, Activo = true }
        );
        await context.SaveChangesAsync();

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var users = await context.Usuarios.ToListAsync();
        Assert.Equal(3, users.Count);
        Assert.Equal("existing", users.First(u => u.UsuarioNombre == "cajero1").PasswordHash); // not overwritten
        Assert.Contains(users, u => u.UsuarioNombre == "cocina1");
        Assert.Contains(users, u => u.UsuarioNombre == "repartidor1");
    }

    // ---- Scenario: User references non-existent role — skipped with no crash ----

    [Fact]
    public async Task SeedAsync_MissingRole_SkipsUser()
    {
        // Arrange: only "Cajero" role exists — cocina and repartidor are missing
        var dbName = $"UserSeed_MissingRole_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Roles.Add(new Rol { Nombre = "Cajero" });
        await context.SaveChangesAsync();

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act — should not crash
        await seeder.SeedAsync();

        // Assert: only cajero1 was inserted
        var users = await context.Usuarios.ToListAsync();
        Assert.Single(users);
        Assert.Equal("cajero1", users[0].UsuarioNombre);
    }

    // ---- Scenario: Roles FK correctness ----

    [Fact]
    public async Task SeedAsync_UsersHaveCorrectRoleFK()
    {
        // Arrange
        var dbName = $"UserSeed_FK_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var cajeroRole = await context.Roles.FirstAsync(r => r.Nombre == "Cajero");
        var cocinaRole = await context.Roles.FirstAsync(r => r.Nombre == "Cocina");
        var repartidorRole = await context.Roles.FirstAsync(r => r.Nombre == "Repartidor");

        var cajero1 = await context.Usuarios.FirstAsync(u => u.UsuarioNombre == "cajero1");
        Assert.Equal(cajeroRole.Id, cajero1.RolId);

        var cocina1 = await context.Usuarios.FirstAsync(u => u.UsuarioNombre == "cocina1");
        Assert.Equal(cocinaRole.Id, cocina1.RolId);

        var repartidor1 = await context.Usuarios.FirstAsync(u => u.UsuarioNombre == "repartidor1");
        Assert.Equal(repartidorRole.Id, repartidor1.RolId);
    }

    // ---- Scenario: Idempotency — running twice doesn't duplicate ----

    [Fact]
    public async Task SeedAsync_RunTwice_NoDuplicates()
    {
        // Arrange
        var dbName = $"UserSeed_Idempotent_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        await SeedRolesAsync(context);

        var seeder = new UserSeedService(context, NullLogger<UserSeedService>.Instance);

        // Act
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert
        var users = await context.Usuarios.ToListAsync();
        Assert.Equal(3, users.Count);
    }
}
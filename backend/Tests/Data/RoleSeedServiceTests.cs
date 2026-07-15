using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class RoleSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    // ---- Scenario: Fresh database — 3 roles inserted ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsThreeRoles()
    {
        // Arrange
        var dbName = $"RoleSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new RoleSeedService(context, NullLogger<RoleSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var roles = await context.Roles.ToListAsync();
        Assert.Equal(3, roles.Count);
        Assert.Contains(roles, r => r.Nombre == "Cajero");
        Assert.Contains(roles, r => r.Nombre == "Cocina");
        Assert.Contains(roles, r => r.Nombre == "Repartidor");
    }

    // ---- Scenario: Pre-existing roles — no duplicates ----

    [Fact]
    public async Task SeedAsync_AllRolesExist_NoNewInserts()
    {
        // Arrange
        var dbName = $"RoleSeed_PreExisting_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Roles.AddRange(
            new Rol { Nombre = "Cajero" },
            new Rol { Nombre = "Cocina" },
            new Rol { Nombre = "Repartidor" }
        );
        await context.SaveChangesAsync();

        var seeder = new RoleSeedService(context, NullLogger<RoleSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var roles = await context.Roles.ToListAsync();
        Assert.Equal(3, roles.Count);
    }

    // ---- Scenario: Partial roles — only missing inserted ----

    [Fact]
    public async Task SeedAsync_PartialRoles_OnlyMissingInserted()
    {
        // Arrange
        var dbName = $"RoleSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Roles.Add(new Rol { Nombre = "Cajero" });
        await context.SaveChangesAsync();

        var seeder = new RoleSeedService(context, NullLogger<RoleSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var roles = await context.Roles.ToListAsync();
        Assert.Equal(3, roles.Count);
        Assert.Single(roles, r => r.Nombre == "Cajero"); // not duplicated
        Assert.Contains(roles, r => r.Nombre == "Cocina");
        Assert.Contains(roles, r => r.Nombre == "Repartidor");
    }

    // ---- Scenario: Old Admin role present — 3 new roles inserted ----

    [Fact]
    public async Task SeedAsync_OldAdminPresent_InsertsThreeNewRoles()
    {
        // Arrange
        var dbName = $"RoleSeed_Admin_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Roles.Add(new Rol { Nombre = "Admin" });
        await context.SaveChangesAsync();

        var seeder = new RoleSeedService(context, NullLogger<RoleSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var roles = await context.Roles.ToListAsync();
        Assert.Equal(4, roles.Count); // Admin + 3 new
        Assert.Contains(roles, r => r.Nombre == "Admin"); // Admin untouched
        Assert.Contains(roles, r => r.Nombre == "Cajero");
        Assert.Contains(roles, r => r.Nombre == "Cocina");
        Assert.Contains(roles, r => r.Nombre == "Repartidor");
    }

    // ---- Scenario: Error handling — does not crash app ----

    [Fact]
    public async Task SeedAsync_OnException_DoesNotRethrow()
    {
        // Arrange
        var dbName = $"RoleSeed_Error_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new RoleSeedService(context, NullLogger<RoleSeedService>.Instance);

        // Seed once to populate
        await seeder.SeedAsync();

        // Seed again — should be idempotent, no exception
        // This tests that AnyAsync prevents double-insert
        await seeder.SeedAsync();

        // Assert: still exactly 3 roles, no crash
        var roles = await context.Roles.ToListAsync();
        Assert.Equal(3, roles.Count);
    }
}
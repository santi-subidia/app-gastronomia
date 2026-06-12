using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class MetodoVentaSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    // ---- Scenario: Fresh DB — both methods inserted ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsTwo()
    {
        // Arrange
        var dbName = $"MetodoVentaSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new MetodoVentaSeedService(context, NullLogger<MetodoVentaSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodosVenta.ToListAsync();
        Assert.Equal(2, metodos.Count);
        Assert.Contains(metodos, m => m.Nombre == "Delivery");
        Assert.Contains(metodos, m => m.Nombre == "Retiro en local");
    }

    // ---- Scenario: All exist — no duplicates ----

    [Fact]
    public async Task SeedAsync_AllExist_NoDuplicates()
    {
        // Arrange
        var dbName = $"MetodoVentaSeed_PreExisting_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.MetodosVenta.AddRange(
            new MetodoVenta { Nombre = "Delivery" },
            new MetodoVenta { Nombre = "Retiro en local" }
        );
        await context.SaveChangesAsync();

        var seeder = new MetodoVentaSeedService(context, NullLogger<MetodoVentaSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodosVenta.ToListAsync();
        Assert.Equal(2, metodos.Count);
    }

    // ---- Scenario: Partial — only missing inserted ----

    [Fact]
    public async Task SeedAsync_Partial_InsertsMissing()
    {
        // Arrange
        var dbName = $"MetodoVentaSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.MetodosVenta.Add(new MetodoVenta { Nombre = "Delivery" });
        await context.SaveChangesAsync();

        var seeder = new MetodoVentaSeedService(context, NullLogger<MetodoVentaSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodosVenta.ToListAsync();
        Assert.Equal(2, metodos.Count);
        Assert.Single(metodos, m => m.Nombre == "Delivery");
        Assert.Contains(metodos, m => m.Nombre == "Retiro en local");
    }

    // ---- Scenario: Idempotent — running twice doesn't duplicate ----

    [Fact]
    public async Task SeedAsync_Idempotent_DoubleRun()
    {
        // Arrange
        var dbName = $"MetodoVentaSeed_Idempotent_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new MetodoVentaSeedService(context, NullLogger<MetodoVentaSeedService>.Instance);

        // Act
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodosVenta.ToListAsync();
        Assert.Equal(2, metodos.Count);
    }
}
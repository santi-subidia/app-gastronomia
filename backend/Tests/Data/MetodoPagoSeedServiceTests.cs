using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class MetodoPagoSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    // ---- Scenario: Fresh DB — all 3 payment methods inserted ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsThree()
    {
        // Arrange
        var dbName = $"MetodoPagoSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new MetodoPagoSeedService(context, NullLogger<MetodoPagoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodoPago.ToListAsync();
        Assert.Equal(3, metodos.Count);
        Assert.Contains(metodos, m => m.Nombre == "Efectivo");
        Assert.Contains(metodos, m => m.Nombre == "Transferencia");
        Assert.Contains(metodos, m => m.Nombre == "Tarjeta");
    }

    // ---- Scenario: All exist — no duplicates ----

    [Fact]
    public async Task SeedAsync_AllExist_NoDuplicates()
    {
        // Arrange
        var dbName = $"MetodoPagoSeed_PreExisting_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.MetodoPago.AddRange(
            new MetodoPago { Nombre = "Efectivo" },
            new MetodoPago { Nombre = "Transferencia" },
            new MetodoPago { Nombre = "Tarjeta" }
        );
        await context.SaveChangesAsync();

        var seeder = new MetodoPagoSeedService(context, NullLogger<MetodoPagoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodoPago.ToListAsync();
        Assert.Equal(3, metodos.Count);
    }

    // ---- Scenario: Partial — only missing inserted ----

    [Fact]
    public async Task SeedAsync_Partial_InsertsMissing()
    {
        // Arrange
        var dbName = $"MetodoPagoSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.MetodoPago.Add(new MetodoPago { Nombre = "Efectivo" });
        await context.SaveChangesAsync();

        var seeder = new MetodoPagoSeedService(context, NullLogger<MetodoPagoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodoPago.ToListAsync();
        Assert.Equal(3, metodos.Count);
        Assert.Single(metodos, m => m.Nombre == "Efectivo");
        Assert.Contains(metodos, m => m.Nombre == "Transferencia");
        Assert.Contains(metodos, m => m.Nombre == "Tarjeta");
    }

    // ---- Scenario: Idempotent — running twice doesn't duplicate ----

    [Fact]
    public async Task SeedAsync_Idempotent_DoubleRun()
    {
        // Arrange
        var dbName = $"MetodoPagoSeed_Idempotent_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new MetodoPagoSeedService(context, NullLogger<MetodoPagoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert
        var metodos = await context.MetodoPago.ToListAsync();
        Assert.Equal(3, metodos.Count);
    }
}
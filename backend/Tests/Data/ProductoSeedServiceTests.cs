using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class ProductoSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    // ---- Scenario: Fresh DB — all 25 products inserted ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsAll25()
    {
        // Arrange
        var dbName = $"ProductoSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var productos = await context.Productos.ToListAsync();
        Assert.Equal(25, productos.Count);

        // Cocina (Demora > 0)
        Assert.Equal(15, productos.Count(p => p.Demora > 0));
        // Bebidas (Demora == 0)
        Assert.Equal(10, productos.Count(p => p.Demora == 0));

        Assert.Contains(productos, p => p.Nombre == "Milanesa con Papas Fritas" && p.Precio == 8500 && p.Demora == 25);
        Assert.Contains(productos, p => p.Nombre == "Coca-Cola 500ml" && p.Precio == 1800 && p.Demora == 0);
    }

    // ---- Scenario: Cocina products have Demora, Bebidas have 0 ----

    [Fact]
    public async Task SeedAsync_CocinaHasDemora_BebidasSinDemora()
    {
        // Arrange
        var dbName = $"ProductoSeed_Demoras_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var cocina = await context.Productos.Where(p => p.Demora > 0).ToListAsync();
        var bebidas = await context.Productos.Where(p => p.Demora == 0).ToListAsync();

        Assert.Equal(15, cocina.Count);
        Assert.Equal(10, bebidas.Count);
        Assert.All(cocina, p => Assert.True(p.Demora >= 10));
        Assert.All(bebidas, p => Assert.Equal(0, p.Demora));
    }

    // ---- Scenario: All exist — no duplicates ----

    [Fact]
    public async Task SeedAsync_AllExist_NoDuplicates()
    {
        // Arrange
        var dbName = $"ProductoSeed_PreExisting_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Productos.Add(new Producto { Nombre = "Milanesa con Papas Fritas", Precio = 8500, Demora = 25 });
        context.Productos.Add(new Producto { Nombre = "Coca-Cola 500ml", Precio = 1800, Demora = 0 });
        await context.SaveChangesAsync();

        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var productos = await context.Productos.ToListAsync();
        Assert.Equal(25, productos.Count);
        Assert.Single(productos, p => p.Nombre == "Milanesa con Papas Fritas");
        Assert.Single(productos, p => p.Nombre == "Coca-Cola 500ml");
    }

    // ---- Scenario: Partial — only missing inserted ----

    [Fact]
    public async Task SeedAsync_Partial_InsertsMissing()
    {
        // Arrange
        var dbName = $"ProductoSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.Productos.Add(new Producto { Nombre = "Pizza Muzzarella Grande", Precio = 6500, Demora = 20 });
        await context.SaveChangesAsync();

        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var productos = await context.Productos.ToListAsync();
        Assert.Equal(25, productos.Count);
        Assert.Single(productos, p => p.Nombre == "Pizza Muzzarella Grande");
    }

    // ---- Scenario: Idempotent — running twice doesn't duplicate ----

    [Fact]
    public async Task SeedAsync_Idempotent_DoubleRun()
    {
        // Arrange
        var dbName = $"ProductoSeed_Idempotent_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert
        var productos = await context.Productos.ToListAsync();
        Assert.Equal(25, productos.Count);
    }

    // ---- Scenario: All products are active by default ----

    [Fact]
    public async Task SeedAsync_AllProductsActive()
    {
        // Arrange
        var dbName = $"ProductoSeed_Active_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new ProductoSeedService(context, NullLogger<ProductoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var productos = await context.Productos.ToListAsync();
        Assert.All(productos, p => Assert.True(p.Activo));
    }
}

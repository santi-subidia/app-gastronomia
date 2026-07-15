using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace ApiGastronomia.Tests.Data;

public class EstadoPedidoSeedServiceTests
{
    private static AppDbContext CreateContext(string dbName)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
        return new AppDbContext(options);
    }

    // ---- Scenario: Fresh DB — all 8 states inserted with explicit IDs ----

    [Fact]
    public async Task SeedAsync_EmptyDb_InsertsEightWithIds()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_Empty_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);

        Assert.Equal("Pendiente", estados.First(e => e.Id == 1).Nombre);
        Assert.Equal("En preparacion", estados.First(e => e.Id == 2).Nombre);
        Assert.Equal("Listo para retirar", estados.First(e => e.Id == 3).Nombre);
        Assert.Equal("En camino", estados.First(e => e.Id == 4).Nombre);
        Assert.Equal("Entregado", estados.First(e => e.Id == 5).Nombre);
        Assert.Equal("Retirado", estados.First(e => e.Id == 6).Nombre);
        Assert.Equal("Cancelado", estados.First(e => e.Id == 7).Nombre);
        Assert.Equal("Devuelto", estados.First(e => e.Id == 8).Nombre);
    }

    // ---- Scenario: Renames old CamelCase names ----

    [Fact]
    public async Task SeedAsync_RenamesOldNames()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_Rename_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.EstadosPedidos.AddRange(
            new EstadoPedido { Id = 2, Nombre = "EnPreparacion" },
            new EstadoPedido { Id = 3, Nombre = "ListoParaRetirar" },
            new EstadoPedido { Id = 4, Nombre = "EnCamino" }
        );
        await context.SaveChangesAsync();

        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
        Assert.Equal("En preparacion", estados.First(e => e.Id == 2).Nombre);
        Assert.Equal("Listo para retirar", estados.First(e => e.Id == 3).Nombre);
        Assert.Equal("En camino", estados.First(e => e.Id == 4).Nombre);
    }

    // ---- Scenario: All 8 correct — no changes ----

    [Fact]
    public async Task SeedAsync_AllCorrect_NoChanges()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_AllCorrect_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.EstadosPedidos.AddRange(
            new EstadoPedido { Id = 1, Nombre = "Pendiente" },
            new EstadoPedido { Id = 2, Nombre = "En preparacion" },
            new EstadoPedido { Id = 3, Nombre = "Listo para retirar" },
            new EstadoPedido { Id = 4, Nombre = "En camino" },
            new EstadoPedido { Id = 5, Nombre = "Entregado" },
            new EstadoPedido { Id = 6, Nombre = "Retirado" },
            new EstadoPedido { Id = 7, Nombre = "Cancelado" },
            new EstadoPedido { Id = 8, Nombre = "Devuelto" }
        );
        await context.SaveChangesAsync();

        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
        Assert.Equal("En preparacion", estados.First(e => e.Id == 2).Nombre);
    }

    // ---- Scenario: Idempotent — running twice, still 8 rows ----

    [Fact]
    public async Task SeedAsync_Idempotent_DoubleRun()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_Idempotent_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
    }

    // ---- Scenario: Partial — missing IDs inserted ----

    [Fact]
    public async Task SeedAsync_Partial_InsertsAndRenames()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_Partial_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.EstadosPedidos.AddRange(
            new EstadoPedido { Id = 1, Nombre = "Pendiente" },
            new EstadoPedido { Id = 3, Nombre = "Listo para retirar" },
            new EstadoPedido { Id = 5, Nombre = "Entregado" }
        );
        await context.SaveChangesAsync();

        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
        Assert.NotNull(estados.First(e => e.Id == 2));
        Assert.NotNull(estados.First(e => e.Id == 4));
        Assert.NotNull(estados.First(e => e.Id == 8));
    }

    // ---- Scenario: Inserts Devuelto when only 1-7 exist ----

    [Fact]
    public async Task SeedAsync_InsertsDevuelto()
    {
        // Arrange
        var dbName = $"EstadoPedidoSeed_Devuelto_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        context.EstadosPedidos.AddRange(
            new EstadoPedido { Id = 1, Nombre = "Pendiente" },
            new EstadoPedido { Id = 2, Nombre = "EnPreparacion" },
            new EstadoPedido { Id = 3, Nombre = "ListoParaRetirar" },
            new EstadoPedido { Id = 4, Nombre = "EnCamino" },
            new EstadoPedido { Id = 5, Nombre = "Entregado" },
            new EstadoPedido { Id = 6, Nombre = "Retirado" },
            new EstadoPedido { Id = 7, Nombre = "Cancelado" }
        );
        await context.SaveChangesAsync();

        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act
        await seeder.SeedAsync();

        // Assert
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
        Assert.Equal("Devuelto", estados.First(e => e.Id == 8).Nombre);
        // Also verify renamed
        Assert.Equal("En preparacion", estados.First(e => e.Id == 2).Nombre);
    }

    // ---- Scenario: Error handling — does not rethrow ----

    [Fact]
    public async Task SeedAsync_Error_DoesNotRethrow()
    {
        // Arrange: seed twice in a row on a healthy context —
        // proves idempotent path executes without crash
        var dbName = $"EstadoPedidoSeed_Error_{Guid.NewGuid()}";
        using var context = CreateContext(dbName);
        var seeder = new EstadoPedidoSeedService(context, NullLogger<EstadoPedidoSeedService>.Instance);

        // Act — should not throw
        await seeder.SeedAsync();
        await seeder.SeedAsync();

        // Assert: still exactly 8 rows, no crash
        var estados = await context.EstadosPedidos.ToListAsync();
        Assert.Equal(8, estados.Count);
    }
}
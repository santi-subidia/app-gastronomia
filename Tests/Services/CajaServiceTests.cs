using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Tests.Services;

public class CajaServiceTests
{
    /// <summary>
    /// Helper to create an InMemory DbContext for testing.
    /// Each test gets a fresh database with a unique name.
    /// </summary>
    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"CajaTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

    /// <summary>
    /// Seeds a single Usuario into the context and returns (context, usuario).
    /// </summary>
    private static (AppDbContext Context, Usuario Usuario) SeedUsuario(
        AppDbContext context,
        string nombre = "Admin Test",
        int rolId = 1)
    {
        // Seed a Rol if needed for FK constraint
        if (!context.Roles.Any())
        {
            context.Roles.Add(new Rol { Nombre = "Admin" });
            context.SaveChanges();
        }

        var usuario = new Usuario
        {
            UsuarioNombre = nombre,
            PasswordHash = "hash",
            Disponible = true,
            Activo = true,
            RolId = rolId
        };

        context.Usuarios.Add(usuario);
        context.SaveChanges();

        return (context, usuario);
    }

    /// <summary>
    /// Seeds a single open Caja into the context and returns (context, caja).
    /// </summary>
    private static (AppDbContext Context, Caja Caja) SeedCaja(
        AppDbContext context,
        int usuarioAperturaId,
        decimal montoApertura = 10000m,
        DateTime? fechaApertura = null,
        DateTime? fechaCierre = null,
        int? usuarioCierreId = null,
        decimal? montoCierreTeorico = null,
        decimal? montoCierreReal = null)
    {
        var caja = new Caja
        {
            UsuarioAperturaId = usuarioAperturaId,
            MontoApertura = montoApertura,
            FechaApertura = fechaApertura ?? DateTime.UtcNow,
            FechaCierre = fechaCierre,
            UsuarioCierreId = usuarioCierreId,
            MontoCierreTeorico = montoCierreTeorico,
            MontoCierreReal = montoCierreReal
        };

        context.Cajas.Add(caja);
        context.SaveChanges();

        return (context, caja);
    }

    // ================================================================
    // ICajaService contract: interface can be resolved from implementation
    // ================================================================

    [Fact]
    public async Task ICajaService_CanBeResolvedFromImplementation()
    {
        // Arrange: verify ICajaService can be assigned from CajaService
        var context = CreateDbContext();
        ICajaService service = new CajaService(context);

        // Assert: interface contract is valid (compilation check)
        Assert.NotNull(service);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // AperturaAsync (4 tests)
    // ================================================================

    [Fact]
    public async Task AperturaAsync_Success_ReturnsCajaResponseWithEstadoAbierta()
    {
        // Arrange: seed a Usuario so the FK is valid
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Apertura");
        var service = new CajaService(context);

        // Act
        var result = await service.AperturaAsync(usuario.Id, montoApertura: 5000m);

        // Assert: CajaResponse has correct fields
        Assert.Equal(usuario.Id, result.UsuarioAperturaId);
        Assert.Equal("abierta", result.Estado);
        Assert.Equal(5000m, result.MontoApertura);
        Assert.Null(result.FechaCierre);
        Assert.NotEqual(default, result.FechaApertura);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AperturaAsync_OpenCajaExists_ThrowsInvalidOperationException()
    {
        // Arrange: seed a Usuario and an open caja (FechaCierre == null)
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Existente");
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 10000m);
        var service = new CajaService(context);

        // Act + Assert: opening another caja when one is already open throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.AperturaAsync(usuario.Id, montoApertura: 2000m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AperturaAsync_NegativeMonto_ThrowsInvalidOperationException()
    {
        // Arrange: seed a valid Usuario, but pass negative MontoApertura
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Negativo");
        var service = new CajaService(context);

        // Act + Assert: negative monto throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.AperturaAsync(usuario.Id, montoApertura: -100m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AperturaAsync_UserNotFound_ThrowsKeyNotFoundException()
    {
        // Arrange: no Usuario seeded, so the FK doesn't exist
        var context = CreateDbContext();
        var service = new CajaService(context);

        // Act + Assert: non-existent UsuarioAperturaId throws
        var ex = await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.AperturaAsync(usuarioAperturaId: 9999, montoApertura: 500m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // CierreAsync (5 tests)
    // ================================================================

    [Fact]
    public async Task CierreAsync_Success_ReturnsCajaResponseWithEstadoCerrada()
    {
        // Arrange: seed a Usuario, open a caja, then close it with another Usuario
        var context = CreateDbContext();
        var (_, usuarioApertura) = SeedUsuario(context, nombre: "Cajero Apertura");
        var (_, usuarioCierre) = SeedUsuario(context, nombre: "Cajero Cierre");
        SeedCaja(context, usuarioAperturaId: usuarioApertura.Id, montoApertura: 10000m);
        var service = new CajaService(context);

        // Get the caja ID that was just seeded
        var caja = context.Cajas.First();

        // Act
        var result = await service.CierreAsync(
            cajaId: caja.Id,
            usuarioCierreId: usuarioCierre.Id,
            montoCierreTeorico: 10500m,
            montoCierreReal: 10300m);

        // Assert: caja is now closed
        Assert.Equal("cerrada", result.Estado);
        Assert.NotNull(result.FechaCierre);
        Assert.Equal(usuarioCierre.Id, result.UsuarioCierreId);
        Assert.Equal(10500m, result.MontoCierreTeorico);
        Assert.Equal(10300m, result.MontoCierreReal);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CierreAsync_CajaNotFound_ThrowsKeyNotFoundException()
    {
        // Arrange: no caja seeded
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Cierre");
        var service = new CajaService(context);

        // Act + Assert: non-existent cajaId throws
        var ex = await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.CierreAsync(cajaId: 9999, usuarioCierreId: usuario.Id, montoCierreTeorico: 100m, montoCierreReal: 100m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CierreAsync_CajaAlreadyClosed_ThrowsInvalidOperationException()
    {
        // Arrange: seed a caja that is already closed
        var context = CreateDbContext();
        var (_, usuarioApertura) = SeedUsuario(context, nombre: "Cajero Apertura");
        var (_, usuarioCierre) = SeedUsuario(context, nombre: "Cajero Cierre");
        SeedCaja(context,
            usuarioAperturaId: usuarioApertura.Id,
            montoApertura: 10000m,
            fechaCierre: DateTime.UtcNow,
            usuarioCierreId: usuarioCierre.Id,
            montoCierreTeorico: 10000m,
            montoCierreReal: 9500m);
        var service = new CajaService(context);

        var caja = context.Cajas.First();

        // Act + Assert: closing an already-closed caja throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CierreAsync(cajaId: caja.Id, usuarioCierreId: usuarioCierre.Id, montoCierreTeorico: 500m, montoCierreReal: 500m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CierreAsync_NegativeMontos_ThrowsInvalidOperationException()
    {
        // Arrange: seed an open caja with valid user
        var context = CreateDbContext();
        var (_, usuarioApertura) = SeedUsuario(context, nombre: "Cajero Apertura");
        var (_, usuarioCierre) = SeedUsuario(context, nombre: "Cajero Cierre");
        SeedCaja(context, usuarioAperturaId: usuarioApertura.Id, montoApertura: 10000m);
        var service = new CajaService(context);

        var caja = context.Cajas.First();

        // Act + Assert: negative montoCierreTeorico throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CierreAsync(cajaId: caja.Id, usuarioCierreId: usuarioCierre.Id, montoCierreTeorico: -100m, montoCierreReal: 500m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CierreAsync_UserNotFound_ThrowsKeyNotFoundException()
    {
        // Arrange: seed an open caja but use a non-existent UsuarioCierreId
        var context = CreateDbContext();
        var (_, usuarioApertura) = SeedUsuario(context, nombre: "Cajero Apertura");
        SeedCaja(context, usuarioAperturaId: usuarioApertura.Id, montoApertura: 10000m);
        var service = new CajaService(context);

        var caja = context.Cajas.First();

        // Act + Assert: non-existent UsuarioCierreId throws
        var ex = await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.CierreAsync(cajaId: caja.Id, usuarioCierreId: 9999, montoCierreTeorico: 100m, montoCierreReal: 100m));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerTodasAsync (4 tests)
    // ================================================================

    [Fact]
    public async Task ObtenerTodasAsync_NoFilter_ReturnsAll()
    {
        // Arrange: seed 2 open and 1 closed caja
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Test");
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 1000m);
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 2000m);
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 3000m,
            fechaCierre: DateTime.UtcNow, usuarioCierreId: usuario.Id,
            montoCierreTeorico: 3000m, montoCierreReal: 2900m);
        var service = new CajaService(context);

        // Act
        var result = await service.ObtenerTodasAsync();

        // Assert: all 3 cajas returned
        var list = result.ToList();
        Assert.Equal(3, list.Count);

        // Assert: ordered by FechaApertura descending (last seeded has latest date)
        // (In seed helper, default FechaApertura = DateTime.UtcNow, so ordering matters)
        // Verify it's not empty and contains all entries
        Assert.Contains(list, c => c.MontoApertura == 1000m);
        Assert.Contains(list, c => c.MontoApertura == 2000m);
        Assert.Contains(list, c => c.MontoApertura == 3000m);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerTodasAsync_EstadoAbiertas_ReturnsOnlyOpen()
    {
        // Arrange: seed 1 open and 1 closed caja
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Test");
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 5000m); // open
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 10000m,
            fechaCierre: DateTime.UtcNow, usuarioCierreId: usuario.Id,
            montoCierreTeorico: 10000m, montoCierreReal: 9500m); // closed
        var service = new CajaService(context);

        // Act
        var result = await service.ObtenerTodasAsync("abiertas");

        // Assert: only the open caja is returned
        var list = result.ToList();
        Assert.Single(list);
        Assert.Equal("abierta", list[0].Estado);
        Assert.Null(list[0].FechaCierre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerTodasAsync_EstadoCerradas_ReturnsOnlyClosed()
    {
        // Arrange: seed 1 open and 1 closed caja
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Test");
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 5000m); // open
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 10000m,
            fechaCierre: DateTime.UtcNow, usuarioCierreId: usuario.Id,
            montoCierreTeorico: 10000m, montoCierreReal: 9500m); // closed
        var service = new CajaService(context);

        // Act
        var result = await service.ObtenerTodasAsync("cerradas");

        // Assert: only the closed caja is returned
        var list = result.ToList();
        Assert.Single(list);
        Assert.Equal("cerrada", list[0].Estado);
        Assert.NotNull(list[0].FechaCierre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerTodasAsync_EstadoInvalido_ReturnsAll()
    {
        // Arrange: seed 2 cajas (1 open, 1 closed) with known state
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Test");
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 5000m); // open
        SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 10000m,
            fechaCierre: DateTime.UtcNow, usuarioCierreId: usuario.Id,
            montoCierreTeorico: 10000m, montoCierreReal: 9500m); // closed
        var service = new CajaService(context);

        // Act: invalid estado value returns all cajas (no filter)
        var result = await service.ObtenerTodasAsync("invalido");

        // Assert: both cajas are returned (no filter applied)
        var list = result.ToList();
        Assert.Equal(2, list.Count);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerPorIdAsync (2 tests)
    // ================================================================

    [Fact]
    public async Task ObtenerPorIdAsync_Found_ReturnsCajaResponse()
    {
        // Arrange: seed a Usuario and an open caja
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Test");
        var (_, caja) = SeedCaja(context, usuarioAperturaId: usuario.Id, montoApertura: 7500m);
        var service = new CajaService(context);

        // Act
        var result = await service.ObtenerPorIdAsync(caja.Id);

        // Assert: CajaResponse found with correct fields and computed Estado
        Assert.NotNull(result);
        Assert.Equal(caja.Id, result!.Id);
        Assert.Equal(usuario.Id, result.UsuarioAperturaId);
        Assert.Equal("abierta", result.Estado);
        Assert.Equal(7500m, result.MontoApertura);
        Assert.Null(result.FechaCierre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerPorIdAsync_NotFound_ReturnsNull()
    {
        // Arrange: no cajas seeded
        var context = CreateDbContext();
        var service = new CajaService(context);

        // Act: search for an ID that doesn't exist
        var result = await service.ObtenerPorIdAsync(9999);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: AperturaAsync with different montos
    // ================================================================

    [Fact]
    public async Task AperturaAsync_Triangulation_DifferentMontos()
    {
        // Arrange: create a caja with 5000, close it, then create another with 15000
        // This verifies that different MontoApertura values are stored correctly
        var context = CreateDbContext();
        var (_, usuario) = SeedUsuario(context, nombre: "Cajero Triang");

        // First apertura with 5000m
        var service = new CajaService(context);
        var primera = await service.AperturaAsync(usuario.Id, montoApertura: 5000m);

        Assert.Equal(5000m, primera.MontoApertura);
        Assert.Equal("abierta", primera.Estado);

        // Close the first caja to allow a second apertura
        var primeraCaja = context.Cajas.First();
        primeraCaja.FechaCierre = DateTime.UtcNow;
        primeraCaja.UsuarioCierreId = usuario.Id;
        primeraCaja.MontoCierreTeorico = 5000m;
        primeraCaja.MontoCierreReal = 4800m;
        await context.SaveChangesAsync();

        // Second apertura with 15000m — triangulation: different input, different output
        var segunda = await service.AperturaAsync(usuario.Id, montoApertura: 15000m);

        Assert.Equal(15000m, segunda.MontoApertura);
        Assert.Equal("abierta", segunda.Estado);
        Assert.NotEqual(primera.Id, segunda.Id); // Different caja

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}
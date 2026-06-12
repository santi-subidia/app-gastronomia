using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Tests.Services;

public class ConfiguracionServiceTests
{
    /// <summary>
    /// Helper to create an InMemory DbContext for testing.
    /// Each test gets a fresh database with a unique name.
    /// </summary>
    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"ConfiguracionTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

    /// <summary>
    /// Seeds a MetodoVenta into the context and returns (context, metodoVenta).
    /// Needed for testing FK relationships.
    /// </summary>
    private static (AppDbContext Context, MetodoVenta MetodoVenta) SeedMetodoVenta(
        AppDbContext context, string nombre = "Efectivo")
    {
        var metodo = new MetodoVenta { Nombre = nombre };
        context.MetodosVenta.Add(metodo);
        context.SaveChanges();
        return (context, metodo);
    }

    /// <summary>
    /// Seeds a Configuracion into the context and returns (context, configuracion).
    /// </summary>
    private static (AppDbContext Context, Configuracion Configuracion) SeedConfiguracion(
        AppDbContext context, int? metodoPagoDefaultId = null,
        string? nombreGastronomico = "Mi Local",
        double? latitudPartida = -34.6037,
        double? longitudPartida = -58.3816)
    {
        var config = new Configuracion
        {
            MetodoPagoDefaultId = metodoPagoDefaultId,
            NombreGastronomico = nombreGastronomico,
            LatitudPartida = latitudPartida,
            LongitudPartida = longitudPartida
        };
        context.Configuracion.Add(config);
        context.SaveChanges();
        return (context, config);
    }

    // ================================================================
    // IConfiguracionService contract: interface can be resolved
    // ================================================================

    [Fact]
    public async Task IConfiguracionService_CanBeResolvedFromImplementation()
    {
        // Arrange
        var context = CreateDbContext();
        IConfiguracionService service = new ConfiguracionService(context);

        // Act: call ObtenerAsync through the interface
        var result = await service.ObtenerAsync();

        // Assert: interface contract works (no config yet → null)
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerAsync: returns config when exists, null when not
    // ================================================================

    [Fact]
    public async Task ObtenerAsync_ConfigExists_ReturnsConfiguracionResponse()
    {
        // Arrange: seed a metodo venta and configuracion
        var context = CreateDbContext();
        var (_, metodo) = SeedMetodoVenta(context, "Efectivo");
        var (_, config) = SeedConfiguracion(context,
            metodoPagoDefaultId: metodo.Id,
            nombreGastronomico: "El Palenque",
            latitudPartida: -34.6037,
            longitudPartida: -58.3816);
        var service = new ConfiguracionService(context);

        // Act
        var result = await service.ObtenerAsync();

        // Assert: response has correct fields including MetodoPagoDefaultNombre
        Assert.NotNull(result);
        Assert.Equal(config.Id, result!.Id);
        Assert.Equal(metodo.Id, result.MetodoPagoDefaultId);
        Assert.Equal("Efectivo", result.MetodoPagoDefaultNombre);
        Assert.Equal("El Palenque", result.NombreGastronomico);
        Assert.Equal(-34.6037, result.LatitudPartida);
        Assert.Equal(-58.3816, result.LongitudPartida);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerAsync_NoConfig_ReturnsNull()
    {
        // Arrange: empty database
        var context = CreateDbContext();
        var service = new ConfiguracionService(context);

        // Act
        var result = await service.ObtenerAsync();

        // Assert: no config exists → null
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // CrearAsync: creates singleton, rejects duplicates
    // ================================================================

    [Fact]
    public async Task CrearAsync_NoExistingConfig_CreatesAndReturnsResponse()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ConfiguracionService(context);

        // Act: create the singleton config
        var result = await service.CrearAsync(
            metodoPagoDefaultId: null,
            nombreGastronomico: "Mi Local",
            latitudPartida: -34.6037,
            longitudPartida: -58.3816);

        // Assert: response DTO has correct fields
        Assert.Equal("Mi Local", result.NombreGastronomico);
        Assert.Null(result.MetodoPagoDefaultId);
        Assert.Equal(-34.6037, result.LatitudPartida);
        Assert.Equal(-58.3816, result.LongitudPartida);
        Assert.NotEqual(0, result.Id); // Id assigned by DB
        Assert.Null(result.MetodoPagoDefaultNombre); // no FK set

        // Assert: config is persisted
        var saved = await context.Configuracion.FirstAsync();
        Assert.Equal("Mi Local", saved.NombreGastronomico);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_AlreadyExists_ThrowsInvalidOperationException()
    {
        // Arrange: seed existing config
        var context = CreateDbContext();
        SeedConfiguracion(context);
        var service = new ConfiguracionService(context);

        // Act + Assert: creating again throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearAsync(null, "Otro Local", null, null));

        Assert.Contains("ya existe", ex.Message, StringComparison.OrdinalIgnoreCase);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ActualizarAsync: partial update, not found returns null
    // ================================================================

    [Fact]
    public async Task ActualizarAsync_PartialUpdate_OnlyChangesProvidedFields()
    {
        // Arrange: seed a config with known values
        var context = CreateDbContext();
        var (_, metodo) = SeedMetodoVenta(context, "Mercado Pago");
        var (_, config) = SeedConfiguracion(context,
            metodoPagoDefaultId: null,
            nombreGastronomico: "Viejo Nombre",
            latitudPartida: -34.0,
            longitudPartida: -58.0);
        var service = new ConfiguracionService(context);

        // Act: update only NombreGastronomico, leave other fields unchanged
        var result = await service.ActualizarAsync(
            metodoPagoDefaultId: metodo.Id,
            nombreGastronomico: "Nuevo Nombre",
            latitudPartida: null,
            longitudPartida: null);

        // Assert: only NombreGastronomico and MetodoPagoDefaultId changed
        Assert.NotNull(result);
        Assert.Equal("Nuevo Nombre", result!.NombreGastronomico);
        Assert.Equal(metodo.Id, result.MetodoPagoDefaultId);
        Assert.Equal("Mercado Pago", result.MetodoPagoDefaultNombre);
        Assert.Equal(-34.0, result.LatitudPartida);   // unchanged
        Assert.Equal(-58.0, result.LongitudPartida);    // unchanged

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarAsync_NoConfig_ReturnsNull()
    {
        // Arrange: empty database, no config exists
        var context = CreateDbContext();
        var service = new ConfiguracionService(context);

        // Act
        var result = await service.ActualizarAsync(null, "Ghost", null, null);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: additional scenarios exercising different code paths
    // ================================================================

    [Fact]
    public async Task CrearAsync_WithMetodoPagoDefaultId_IncludesNavigationNombre()
    {
        // Arrange: seed a MetodoVenta, then create config referencing it
        var context = CreateDbContext();
        var (_, metodo) = SeedMetodoVenta(context, "Tarjeta");
        var service = new ConfiguracionService(context);

        // Act: create config with MetodoPagoDefaultId
        var result = await service.CrearAsync(
            metodoPagoDefaultId: metodo.Id,
            nombreGastronomico: "Local Con Pago",
            latitudPartida: -31.5,
            longitudPartida: -60.2);

        // Assert: response includes the navigation property name
        Assert.Equal(metodo.Id, result.MetodoPagoDefaultId);
        Assert.Equal("Tarjeta", result.MetodoPagoDefaultNombre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerAsync_AfterCrear_ReturnsSameConfiguracion()
    {
        // Arrange: create config first, then fetch it
        var context = CreateDbContext();
        var service = new ConfiguracionService(context);

        var created = await service.CrearAsync(null, "Creado Local", -33.0, -57.0);

        // Act: fetch back via ObtenerAsync
        var fetched = await service.ObtenerAsync();

        // Assert: same data returned
        Assert.NotNull(fetched);
        Assert.Equal(created.Id, fetched!.Id);
        Assert.Equal("Creado Local", fetched.NombreGastronomico);
        Assert.Equal(-33.0, fetched.LatitudPartida);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarAsync_UpdatesOnlyLatitud_NotOthers()
    {
        // Arrange: seed config with all fields set
        var context = CreateDbContext();
        var (_, config) = SeedConfiguracion(context,
            nombreGastronomico: "Original",
            latitudPartida: -34.0,
            longitudPartida: -58.0);
        var service = new ConfiguracionService(context);

        // Act: update only LatitudPartida
        var result = await service.ActualizarAsync(
            metodoPagoDefaultId: null,
            nombreGastronomico: null,
            latitudPartida: -35.5,
            longitudPartida: null);

        // Assert: only LatitudPartida changed
        Assert.NotNull(result);
        Assert.Equal("Original", result!.NombreGastronomico);    // unchanged
        Assert.Equal(-35.5, result.LatitudPartida);                // changed
        Assert.Equal(-58.0, result.LongitudPartida);               // unchanged

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}
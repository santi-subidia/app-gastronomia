using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Tests.Services;

public class ProductoServiceTests
{
    /// <summary>
    /// Helper to create an InMemory DbContext for testing.
    /// Each test gets a fresh database with a unique name.
    /// </summary>
    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"ProductoTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

    /// <summary>
    /// Seeds a single Producto into the context and returns (context, producto).
    /// </summary>
    private static (AppDbContext Context, Producto Producto) SeedProducto(
        AppDbContext context,
        string nombre = "Pizza Napolitana",
        double precio = 5500.0,
        int demora = 20,
        bool activo = true)
    {
        var producto = new Producto
        {
            Nombre = nombre,
            Precio = precio,
            Demora = demora,
            Activo = activo
        };

        context.Productos.Add(producto);
        context.SaveChanges();

        return (context, producto);
    }

    // ================================================================
    // IProductoService contract: interface can be resolved from implementation
    // ================================================================

    [Fact]
    public async Task IProductoService_CanBeResolvedFromImplementation()
    {
        // Arrange: verify IProductoService can be assigned from ProductoService
        var context = CreateDbContext();
        IProductoService service = new ProductoService(context);

        // Act: call ObtenerProductosAsync through the interface
        var result = await service.ObtenerProductosAsync();

        // Assert: interface contract works
        Assert.NotNull(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerProductosAsync: returns only active products
    // ================================================================

    [Fact]
    public async Task ObtenerProductosAsync_ReturnsOnlyActiveProducts()
    {
        // Arrange: seed one active and one inactive product
        var context = CreateDbContext();
        SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10, activo: true);
        SeedProducto(context, nombre: "Pizza Vieja", precio: 3000.0, demora: 15, activo: false);
        var service = new ProductoService(context);

        // Act
        var result = await service.ObtenerProductosAsync();

        // Assert: only the active product is returned
        var list = result.ToList();
        Assert.Single(list);
        Assert.Equal("Empanada", list[0].Nombre);
        Assert.True(list[0].Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerProductoPorIdAsync: returns product when found and active
    // ================================================================

    [Fact]
    public async Task ObtenerProductoPorIdAsync_ActiveProduct_ReturnsProductoResponse()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var service = new ProductoService(context);

        // Act
        var result = await service.ObtenerProductoPorIdAsync(producto.Id);

        // Assert: product found with correct fields
        Assert.NotNull(result);
        Assert.Equal(producto.Id, result!.Id);
        Assert.Equal("Empanada", result.Nombre);
        Assert.Equal(1500.0, result.Precio);
        Assert.Equal(10, result.Demora);
        Assert.True(result.Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerProductoPorIdAsync_NonexistentProduct_ReturnsNull()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ProductoService(context);

        // Act: search for an ID that doesn't exist
        var result = await service.ObtenerProductoPorIdAsync(999);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // KEY BEHAVIOR: inactive product returns null (not the entity)
    [Fact]
    public async Task ObtenerProductoPorIdAsync_InactiveProduct_ReturnsNull()
    {
        // Arrange: seed an inactive product
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Pizza Vieja", precio: 3000.0, demora: 15, activo: false);
        var service = new ProductoService(context);

        // Act: request an inactive product by ID
        var result = await service.ObtenerProductoPorIdAsync(producto.Id);

        // Assert: inactive product returns null (treated as not found)
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // CrearProductoAsync: creates product, rejects duplicates
    // ================================================================

    [Fact]
    public async Task CrearProductoAsync_ValidData_CreatesProductWithActivoTrue()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ProductoService(context);

        // Act
        var result = await service.CrearProductoAsync("Pizza Napolitana", 5500.0, 20);

        // Assert: response DTO has correct fields
        Assert.Equal("Pizza Napolitana", result.Nombre);
        Assert.Equal(5500.0, result.Precio);
        Assert.Equal(20, result.Demora);
        Assert.True(result.Activo);

        // Assert: product is persisted with Activo = true
        var saved = await context.Productos.FirstAsync(p => p.Nombre == "Pizza Napolitana");
        Assert.True(saved.Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearProductoAsync_DuplicateNombre_ThrowsInvalidOperationException()
    {
        // Arrange: seed existing product
        var context = CreateDbContext();
        SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var service = new ProductoService(context);

        // Act + Assert: creating product with same name throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearProductoAsync("Empanada", 2000.0, 15));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ActualizarProductoAsync: partial update, null means unchanged
    // ================================================================

    [Fact]
    public async Task ActualizarProductoAsync_PartialUpdate_OnlyChangesProvidedFields()
    {
        // Arrange: seed product with known values
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var service = new ProductoService(context);

        // Act: update only Precio, leave Nombre and Demora unchanged
        var result = await service.ActualizarProductoAsync(producto.Id, nombre: null, precio: 2000.0, demora: null);

        // Assert: only Precio changed
        Assert.NotNull(result);
        Assert.Equal("Empanada", result!.Nombre); // unchanged
        Assert.Equal(2000.0, result.Precio); // changed
        Assert.Equal(10, result.Demora); // unchanged

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarProductoAsync_NonexistentProduct_ReturnsNull()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ProductoService(context);

        // Act
        var result = await service.ActualizarProductoAsync(999, nombre: "Ghost", precio: null, demora: null);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarProductoAsync_DuplicateNombre_ThrowsInvalidOperationException()
    {
        // Arrange: seed two products with different names
        var context = CreateDbContext();
        SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var (_, product2) = SeedProducto(context, nombre: "Pizza", precio: 5000.0, demora: 25);
        var service = new ProductoService(context);

        // Act + Assert: updating product2's name to "Empanada" (which already exists) throws
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.ActualizarProductoAsync(product2.Id, nombre: "Empanada", precio: null, demora: null));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // EliminarProductoAsync: soft delete, inactive returns false
    // ================================================================

    [Fact]
    public async Task EliminarProductoAsync_ActiveProduct_SetsActivoFalse()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var service = new ProductoService(context);

        // Act
        var result = await service.EliminarProductoAsync(producto.Id);

        // Assert: soft delete returns true
        Assert.True(result);

        // Assert: product still exists in DB but Activo = false
        var deletedProduct = await context.Productos.FindAsync(producto.Id);
        Assert.NotNull(deletedProduct);
        Assert.False(deletedProduct!.Activo);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarProductoAsync_NonexistentProduct_ReturnsFalse()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ProductoService(context);

        // Act
        var result = await service.EliminarProductoAsync(999);

        // Assert
        Assert.False(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // KEY BEHAVIOR: deleting an already-inactive product returns false (404 at controller level)
    [Fact]
    public async Task EliminarProductoAsync_AlreadyInactiveProduct_ReturnsFalse()
    {
        // Arrange: seed an inactive product
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Pizza Vieja", precio: 3000.0, demora: 15, activo: false);
        var service = new ProductoService(context);

        // Act
        var result = await service.EliminarProductoAsync(producto.Id);

        // Assert: already inactive → returns false
        Assert.False(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: additional scenarios exercising different code paths
    // ================================================================

    [Fact]
    public async Task ObtenerProductosAsync_NoActiveProducts_ReturnsEmptyCollection()
    {
        // Arrange: seed only inactive products
        var context = CreateDbContext();
        SeedProducto(context, nombre: "Pizza Vieja", precio: 3000.0, demora: 15, activo: false);
        var service = new ProductoService(context);

        // Act
        var result = await service.ObtenerProductosAsync();

        // Assert: empty collection (the Where clause filters all out)
        var list = result.ToList();
        Assert.Empty(list);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearProductoAsync_WithDifferentData_ReturnsCorrectFields()
    {
        // Arrange
        var context = CreateDbContext();
        var service = new ProductoService(context);

        // Act: create a product with different values from the previous test
        var result = await service.CrearProductoAsync("Hamburguesa", 8000.0, 30);

        // Assert: triangulation — different inputs produce different outputs
        Assert.Equal("Hamburguesa", result.Nombre);
        Assert.Equal(8000.0, result.Precio);
        Assert.Equal(30, result.Demora);
        Assert.True(result.Activo);
        Assert.NotEqual(0, result.Id); // Id assigned by DB

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarProductoAsync_UpdatesNombreToUniqueValue_Succeeds()
    {
        // Arrange: seed product, update its name to a unique value
        var context = CreateDbContext();
        var (_, producto) = SeedProducto(context, nombre: "Empanada", precio: 1500.0, demora: 10);
        var service = new ProductoService(context);

        // Act
        var result = await service.ActualizarProductoAsync(producto.Id, nombre: "Empanada Carne", precio: null, demora: null);

        // Assert: name updated, other fields unchanged
        Assert.NotNull(result);
        Assert.Equal("Empanada Carne", result!.Nombre);
        Assert.Equal(1500.0, result.Precio);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarProductoAsync_DeletedProductExcludedFromGetAll()
    {
        // Arrange: seed two products, delete one
        var context = CreateDbContext();
        SeedProducto(context, nombre: "Visible Pizza", precio: 5500.0, demora: 20);
        var (_, toDelete) = SeedProducto(context, nombre: "To Delete", precio: 1000.0, demora: 5);
        var service = new ProductoService(context);

        // Act: soft delete one product
        await service.EliminarProductoAsync(toDelete.Id);

        // Assert: GetAll only returns the active product
        var allProducts = await service.ObtenerProductosAsync();
        var list = allProducts.ToList();
        Assert.Single(list);
        Assert.Equal("Visible Pizza", list[0].Nombre);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}
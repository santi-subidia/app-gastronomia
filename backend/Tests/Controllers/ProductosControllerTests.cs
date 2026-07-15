using System.Security.Claims;
using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class ProductosControllerTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static readonly ProductoResponse PizzaResponse = new(
        Id: 1, Nombre: "Pizza Napolitana", Precio: 5500.0, Demora: 20, Activo: true);

    private static readonly ProductoResponse EmpanadaResponse = new(
        Id: 2, Nombre: "Empanada", Precio: 1500.0, Demora: 10, Activo: true);

    private List<ProductoResponse> GetAllProducts() => [PizzaResponse, EmpanadaResponse];

    /// <summary>
    /// Creates a controller with an authenticated user (claims-based).
    /// This simulates [Authorize] without the middleware pipeline.
    /// </summary>
    private static ProductosController CreateControllerWithUser(
        IProductoService service, int userId = 1, string role = "Cajero")
    {
        var controller = new ProductosController(service);
        var claims = new List<Claim>
        {
            new("sub", userId.ToString()),
            new("role", role)
        };
        var identity = new ClaimsIdentity(claims, "TestAuth");
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext
            {
                User = new ClaimsPrincipal(identity)
            }
        };
        return controller;
    }

    // ================================================================
    // GET /api/productos — List all active products
    // ================================================================

    [Fact]
    public async Task GetAll_ReturnsOkWithProductList()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ObtenerProductosAsync())
            .ReturnsAsync(GetAllProducts());

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with list of 2 products
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var products = Assert.IsAssignableFrom<IEnumerable<ProductoResponse>>(okResult.Value!);
        Assert.Equal(2, products.Count());
    }

    // ================================================================
    // GET /api/productos/{id} — Get product by ID
    // ================================================================

    [Fact]
    public async Task GetById_ActiveProduct_ReturnsOk()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ObtenerProductoPorIdAsync(1))
            .ReturnsAsync(PizzaResponse);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetById(1);

        // Assert: 200 OK with product
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ProductoResponse>(okResult.Value!);
        Assert.Equal("Pizza Napolitana", response.Nombre);
    }

    [Fact]
    public async Task GetById_NonexistentProduct_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ObtenerProductoPorIdAsync(999))
            .ReturnsAsync((ProductoResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetById(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // POST /api/productos — Create product (Cajero only)
    // ================================================================

    [Fact]
    public async Task Create_ValidRequest_ReturnsCreatedAtAction()
    {
        // Arrange
        var createdProduct = new ProductoResponse(
            Id: 5, Nombre: "Pizza Napolitana", Precio: 5500.0, Demora: 20, Activo: true);
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.CrearProductoAsync("Pizza Napolitana", 5500.0, 20))
            .ReturnsAsync(createdProduct);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearProductoRequest(Nombre: "Pizza Napolitana", Precio: 5500.0, Demora: 20);

        // Act
        var result = await controller.Create(request);

        // Assert: 201 CreatedAtAction
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        Assert.Equal(nameof(ProductosController.GetById), createdResult.ActionName);
        var response = Assert.IsType<ProductoResponse>(createdResult.Value!);
        Assert.Equal("Pizza Napolitana", response.Nombre);
        Assert.True(response.Activo);
    }

    [Fact]
    public async Task Create_DuplicateNombre_ReturnsConflict()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.CrearProductoAsync("Empanada", 2000.0, 15))
            .ThrowsAsync(new InvalidOperationException("Ya existe un producto con el nombre 'Empanada'."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearProductoRequest(Nombre: "Empanada", Precio: 2000.0, Demora: 15);

        // Act
        var result = await controller.Create(request);

        // Assert: 409 Conflict
        Assert.IsType<ConflictObjectResult>(result.Result);
    }

    // ================================================================
    // PUT /api/productos/{id} — Update product (Cajero only)
    // ================================================================

    [Fact]
    public async Task Update_ValidPartialUpdate_ReturnsOk()
    {
        // Arrange: update only Precio
        var updatedProduct = PizzaResponse with { Precio = 6000.0 };
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ActualizarProductoAsync(1, null, 6000.0, null))
            .ReturnsAsync(updatedProduct);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarProductoRequest(Nombre: null, Precio: 6000.0, Demora: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert: 200 OK with updated product
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ProductoResponse>(okResult.Value!);
        Assert.Equal(6000.0, response.Precio);
    }

    [Fact]
    public async Task Update_NonexistentProduct_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ActualizarProductoAsync(999, "Ghost", null, null))
            .ReturnsAsync((ProductoResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarProductoRequest(Nombre: "Ghost", Precio: null, Demora: null);

        // Act
        var result = await controller.Update(999, request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    [Fact]
    public async Task Update_DuplicateNombre_ReturnsConflict()
    {
        // Arrange: update product name to one that already exists
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ActualizarProductoAsync(1, "Empanada", null, null))
            .ThrowsAsync(new InvalidOperationException("Ya existe un producto con el nombre 'Empanada'."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarProductoRequest(Nombre: "Empanada", Precio: null, Demora: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert: 409 Conflict
        Assert.IsType<ConflictObjectResult>(result.Result);
    }

    // ================================================================
    // DELETE /api/productos/{id} — Soft delete (Cajero only)
    // ================================================================

    [Fact]
    public async Task Delete_ActiveProduct_ReturnsNoContent()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.EliminarProductoAsync(1))
            .ReturnsAsync(true);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(1);

        // Assert: 204 NoContent
        Assert.IsType<NoContentResult>(result);
    }

    [Fact]
    public async Task Delete_NonexistentProduct_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.EliminarProductoAsync(999))
            .ReturnsAsync(false);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result);
    }

    // ================================================================
    // Triangulation
    // ================================================================

    [Fact]
    public async Task GetAll_NoActiveProducts_ReturnsEmptyCollection()
    {
        // Arrange
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ObtenerProductosAsync())
            .ReturnsAsync([]);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with empty collection
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var products = Assert.IsAssignableFrom<IEnumerable<ProductoResponse>>(okResult.Value!);
        Assert.Empty(products);
    }

    [Fact]
    public async Task Create_ProductWithDifferentData_ReturnsCorrectFields()
    {
        // Arrange: triangulation with different product data
        var createdProduct = new ProductoResponse(
            Id: 10, Nombre: "Hamburguesa", Precio: 8000.0, Demora: 30, Activo: true);
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.CrearProductoAsync("Hamburguesa", 8000.0, 30))
            .ReturnsAsync(createdProduct);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearProductoRequest(Nombre: "Hamburguesa", Precio: 8000.0, Demora: 30);

        // Act
        var result = await controller.Create(request);

        // Assert
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        var response = Assert.IsType<ProductoResponse>(createdResult.Value!);
        Assert.Equal("Hamburguesa", response.Nombre);
        Assert.Equal(8000.0, response.Precio);
        Assert.Equal(30, response.Demora);
    }

    [Fact]
    public async Task Update_UpdatesNombreToUniqueValue_Succeeds()
    {
        // Arrange: update only Nombre
        var updatedProduct = PizzaResponse with { Nombre = "Pizza Margherita" };
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.ActualizarProductoAsync(1, "Pizza Margherita", null, null))
            .ReturnsAsync(updatedProduct);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarProductoRequest(Nombre: "Pizza Margherita", Precio: null, Demora: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert: 200 OK, name updated
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ProductoResponse>(okResult.Value!);
        Assert.Equal("Pizza Margherita", response.Nombre);
    }

    [Fact]
    public async Task Delete_AlreadyInactiveProduct_ReturnsNotFound()
    {
        // Arrange: service returns false for already-inactive product
        var mockService = new Mock<IProductoService>();
        mockService
            .Setup(s => s.EliminarProductoAsync(5))
            .ReturnsAsync(false);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(5);

        // Assert: 404 NotFound (already inactive treated same as not found)
        Assert.IsType<NotFoundObjectResult>(result);
    }
}
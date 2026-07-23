using System.Security.Claims;
using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class DemorasControllerTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static readonly DemoraResponse Demora1Response = new(
        Id: 1, PedidoId: 10, UsuarioId: 5, DemoraMinutos: 15, Sector: "cocina", Observaciones: "falta stock");

    private static readonly DemoraResponse Demora2Response = new(
        Id: 2, PedidoId: 10, UsuarioId: 5, DemoraMinutos: 30, Sector: "reparto", Observaciones: null);

    private List<DemoraResponse> GetDemorasByPedido() => [Demora1Response, Demora2Response];

    /// <summary>
    /// Creates a DemorasController with an authenticated user (claims-based).
    /// This simulates [Authorize] without the middleware pipeline.
    /// </summary>
    private static DemorasController CreateControllerWithUser(
        IDemoraService service, int userId = 1, string role = "Cajero")
    {
        var controller = new DemorasController(service);
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
    // GET /api/demoras?pedidoId={id} â€” List demoras by pedido
    // ================================================================

    [Fact]
    public async Task GetByPedido_PedidoConDemoras_ReturnsOkWithDemoraList()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ObtenerPorPedidoAsync(10))
            .ReturnsAsync(GetDemorasByPedido());

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetByPedido(10);

        // Assert: 200 OK with list of 2 demoras
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var demoras = Assert.IsAssignableFrom<IEnumerable<DemoraResponse>>(okResult.Value!);
        Assert.Equal(2, demoras.Count());
    }

    [Fact]
    public async Task GetByPedido_PedidoSinDemoras_ReturnsOkWithEmptyList()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ObtenerPorPedidoAsync(10))
            .ReturnsAsync([]);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetByPedido(10);

        // Assert: 200 OK with empty collection
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var demoras = Assert.IsAssignableFrom<IEnumerable<DemoraResponse>>(okResult.Value!);
        Assert.Empty(demoras);
    }

    [Fact]
    public async Task GetByPedido_PedidoInexistente_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ObtenerPorPedidoAsync(999))
            .ThrowsAsync(new KeyNotFoundException("Pedido #999 no encontrado."));

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.GetByPedido(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // POST /api/demoras â€” Create demora (Cajero only)
    // ================================================================

    [Fact]
    public async Task Create_ValidRequest_ReturnsCreatedAtAction()
    {
        // Arrange
        var createdDemora = new DemoraResponse(
            Id: 5, PedidoId: 10, UsuarioId: 1, DemoraMinutos: 20, Sector: "cocina", Observaciones: "falta stock");
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.CrearAsync(10, 20, "falta stock"))
            .ReturnsAsync(createdDemora);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearDemoraRequest(PedidoId: 10, DemoraMinutos: 20, Observaciones: "falta stock");

        // Act
        var result = await controller.Create(request);

        // Assert: 201 CreatedAtAction
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        Assert.Equal(nameof(DemorasController.GetByPedido), createdResult.ActionName);
        var response = Assert.IsType<DemoraResponse>(createdResult.Value!);
        Assert.Equal(10, response.PedidoId);
        Assert.Equal(20, response.DemoraMinutos);
        Assert.Equal("cocina", response.Sector);
    }

    [Fact]
    public async Task Create_DemoraMinutosZero_ReturnsBadRequest()
    {
        // Arrange: service throws InvalidOperationException for demoraMinutos <= 0
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.CrearAsync(10, 0, null))
            .ThrowsAsync(new InvalidOperationException("La demora debe ser mayor que cero."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearDemoraRequest(PedidoId: 10, DemoraMinutos: 0, Observaciones: null);

        // Act
        var result = await controller.Create(request);

        // Assert: 400 BadRequest
        Assert.IsType<BadRequestObjectResult>(result.Result);
    }

    [Fact]
    public async Task Create_PedidoInexistente_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.CrearAsync(999, 10, null))
            .ThrowsAsync(new KeyNotFoundException("Pedido #999 no encontrado."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearDemoraRequest(PedidoId: 999, DemoraMinutos: 10, Observaciones: null);

        // Act
        var result = await controller.Create(request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // PUT /api/demoras/{id} â€” Update demora (Cajero only)
    // ================================================================

    [Fact]
    public async Task Update_ValidRequest_ReturnsOk()
    {
        // Arrange
        var updatedDemora = Demora1Response with { DemoraMinutos = 30 };
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ActualizarAsync(1, 30, null))
            .ReturnsAsync(updatedDemora);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarDemoraRequest(DemoraMinutos: 30, Observaciones: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert: 200 OK with updated demora
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<DemoraResponse>(okResult.Value!);
        Assert.Equal(30, response.DemoraMinutos);
        Assert.Equal("cocina", response.Sector);
    }

    [Fact]
    public async Task Update_NonexistentDemora_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ActualizarAsync(999, 10, null))
            .ReturnsAsync((DemoraResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarDemoraRequest(DemoraMinutos: 10, Observaciones: null);

        // Act
        var result = await controller.Update(999, request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // DELETE /api/demoras/{id} â€” Hard delete (Cajero only)
    // ================================================================

    [Fact]
    public async Task Delete_ExistingDemora_ReturnsNoContent()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.EliminarAsync(1))
            .ReturnsAsync(true);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(1);

        // Assert: 204 NoContent
        Assert.IsType<NoContentResult>(result);
    }

    [Fact]
    public async Task Delete_NonexistentDemora_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.EliminarAsync(999))
            .ReturnsAsync(false);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result);
    }

    // ================================================================
    // Triangulation: additional scenarios exercising different code paths
    // ================================================================

    [Fact]
    public async Task Create_WithDifferentData_ReturnsCorrectFields()
    {
        // Arrange: triangulation â€” different inputs produce different outputs
        var createdDemora = new DemoraResponse(
            Id: 10, PedidoId: 20, UsuarioId: 3, DemoraMinutos: 45, Sector: "reparto", Observaciones: "trÃ¡fico");
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.CrearAsync(20, 45, "trÃ¡fico"))
            .ReturnsAsync(createdDemora);

        var controller = CreateControllerWithUser(mockService.Object, userId: 3, role: "Cajero");
        var request = new CrearDemoraRequest(PedidoId: 20, DemoraMinutos: 45, Observaciones: "trÃ¡fico");

        // Act
        var result = await controller.Create(request);

        // Assert
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        var response = Assert.IsType<DemoraResponse>(createdResult.Value!);
        Assert.Equal(20, response.PedidoId);
        Assert.Equal(45, response.DemoraMinutos);
        Assert.Equal("reparto", response.Sector);
        Assert.Equal("trÃ¡fico", response.Observaciones);
    }

    [Fact]
    public async Task Update_PartialUpdate_ReturnsUpdatedDemora()
    {
        // Arrange: update only DemoraMinutos, sector and observaciones stay the same
        var updatedDemora = Demora1Response with { DemoraMinutos = 60 };
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.ActualizarAsync(1, 60, null))
            .ReturnsAsync(updatedDemora);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarDemoraRequest(DemoraMinutos: 60, Observaciones: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<DemoraResponse>(okResult.Value!);
        Assert.Equal(60, response.DemoraMinutos);
    }

    [Fact]
    public async Task Delete_AlreadyDeletedDemora_ReturnsNotFound()
    {
        // Arrange: service returns false for already-deleted demora
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.EliminarAsync(5))
            .ReturnsAsync(false);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");

        // Act
        var result = await controller.Delete(5);

        // Assert: 404 (same as nonexistent)
        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task Create_DemoraMinutosNegativo_ReturnsBadRequest()
    {
        // Arrange: triangulation â€” negative also triggers 400
        var mockService = new Mock<IDemoraService>();
        mockService
            .Setup(s => s.CrearAsync(10, -5, null))
            .ThrowsAsync(new InvalidOperationException("La demora debe ser mayor que cero."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearDemoraRequest(PedidoId: 10, DemoraMinutos: -5, Observaciones: null);

        // Act
        var result = await controller.Create(request);

        // Assert: 400 BadRequest
        Assert.IsType<BadRequestObjectResult>(result.Result);
    }

    // ================================================================
    // Auth scenarios: [Authorize] on class, [Authorize(Roles = "Cajero")] on Create/Update/Delete
    // These tests verify that the authz attribute IS present, not that
    // middleware enforces it (middleware is integration-level).
    // ================================================================

    [Fact]
    public void Controller_HasAuthorizeOnClass()
    {
        // Assert: class-level [Authorize]
        var attr = typeof(DemorasController)
            .GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
    }

    [Fact]
    public void GetByPedido_DoesNotHaveRoleSpecificAuthorize()
    {
        // Assert: GET only has class-level [Authorize], no role-specific
        var method = typeof(DemorasController).GetMethod("GetByPedido");
        var attrs = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .ToList();

        Assert.Empty(attrs!);
    }

    [Fact]
    public void Create_DoesNotHaveCajeroRoleRestriction()
    {
        // Assert: POST no longer has [Authorize(Roles = "Cajero")]
        var method = typeof(DemorasController).GetMethod("Create");
        var attr = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        if (attr != null) {
            Assert.NotEqual("Cajero", attr.Roles);
        }
    }

    [Fact]
    public void Update_HasAuthorizeWithCajeroRole()
    {
        // Assert: PUT has [Authorize(Roles = "Cajero")]
        var method = typeof(DemorasController).GetMethod("Update");
        var attr = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
        Assert.Equal("Cajero", attr!.Roles);
    }

    [Fact]
    public void Delete_HasAuthorizeWithCajeroRole()
    {
        // Assert: DELETE has [Authorize(Roles = "Cajero")]
        var method = typeof(DemorasController).GetMethod("Delete");
        var attr = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
        Assert.Equal("Cajero", attr!.Roles);
    }
}

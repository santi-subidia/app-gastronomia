using System.Security.Claims;
using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class ConfiguracionControllerTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static readonly ConfiguracionResponse ConfigResponse = new(
        Id: 1,
        MetodoPagoDefaultId: 10,
        MetodoPagoDefaultNombre: "Efectivo",
        NombreGastronomico: "El Palenque",
        LatitudPartida: -34.6037,
        LongitudPartida: -58.3816
    );

    /// <summary>
    /// Creates a controller with an authenticated user (claims-based).
    /// This simulates [Authorize] without the middleware pipeline.
    /// </summary>
    private static ConfiguracionController CreateControllerWithUser(
        IConfiguracionService service, int userId = 1, string role = "Cajero")
    {
        var controller = new ConfiguracionController(service);
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
    // GET /api/configuracion — ObtenerAsync
    // ================================================================

    [Fact]
    public async Task Get_ConfigExists_ReturnsOkWithResponse()
    {
        // Arrange
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.ObtenerAsync())
            .ReturnsAsync(ConfigResponse);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.Get();

        // Assert: 200 OK with ConfiguracionResponse
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ConfiguracionResponse>(okResult.Value!);
        Assert.Equal("El Palenque", response.NombreGastronomico);
        Assert.Equal("Efectivo", response.MetodoPagoDefaultNombre);
    }

    [Fact]
    public async Task Get_ConfigDoesNotExist_ReturnsNotFound()
    {
        // Arrange: service returns null → 404
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.ObtenerAsync())
            .ReturnsAsync((ConfiguracionResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object);

        // Act
        var result = await controller.Get();

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // POST /api/configuracion — CrearAsync (Cajero only)
    // ================================================================

    [Fact]
    public async Task Create_ValidRequest_ReturnsCreatedAtAction()
    {
        // Arrange
        var createdConfig = ConfigResponse with { Id = 5 };
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.CrearAsync(null, "El Palenque", -34.6037, -58.3816))
            .ReturnsAsync(createdConfig);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearConfiguracionRequest(
            MetodoPagoDefaultId: null, NombreGastronomico: "El Palenque",
            LatitudPartida: -34.6037, LongitudPartida: -58.3816);

        // Act
        var result = await controller.Create(request);

        // Assert: 201 CreatedAtAction
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        Assert.Equal(nameof(ConfiguracionController.Get), createdResult.ActionName);
        var response = Assert.IsType<ConfiguracionResponse>(createdResult.Value!);
        Assert.Equal("El Palenque", response.NombreGastronomico);
    }

    [Fact]
    public async Task Create_AlreadyExists_ReturnsConflict()
    {
        // Arrange: service throws InvalidOperationException for duplicate
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.CrearAsync(null, "Otro", null, null))
            .ThrowsAsync(new InvalidOperationException("La configuración ya existe."));

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearConfiguracionRequest(null, "Otro", null, null);

        // Act
        var result = await controller.Create(request);

        // Assert: 409 Conflict
        Assert.IsType<ConflictObjectResult>(result.Result);
    }

    // ================================================================
    // PUT /api/configuracion — ActualizarAsync (Cajero only)
    // ================================================================

    [Fact]
    public async Task Update_ValidPartialUpdate_ReturnsOk()
    {
        // Arrange: update only NombreGastronomico
        var updatedConfig = ConfigResponse with { NombreGastronomico = "Nuevo Nombre" };
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.ActualizarAsync(null, "Nuevo Nombre", null, null))
            .ReturnsAsync(updatedConfig);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarConfiguracionRequest(
            MetodoPagoDefaultId: null, NombreGastronomico: "Nuevo Nombre",
            LatitudPartida: null, LongitudPartida: null);

        // Act
        var result = await controller.Update(request);

        // Assert: 200 OK with updated response
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ConfiguracionResponse>(okResult.Value!);
        Assert.Equal("Nuevo Nombre", response.NombreGastronomico);
    }

    [Fact]
    public async Task Update_ConfigDoesNotExist_ReturnsNotFound()
    {
        // Arrange: service returns null → 404
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.ActualizarAsync(null, "Ghost", null, null))
            .ReturnsAsync((ConfiguracionResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarConfiguracionRequest(null, "Ghost", null, null);

        // Act
        var result = await controller.Update(request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // Auth scenarios: [Authorize(Roles = "Cajero")] on POST/PUT
    // These tests verify that the authz attribute IS present, not that
    // middleware enforces it (middleware is integration-level).
    // ================================================================

    [Fact]
    public void Create_HasAuthorizeWithCajeroRole()
    {
        // Arrange: verify POST has [Authorize(Roles = "Cajero")]
        var method = typeof(ConfiguracionController).GetMethod("Create");
        var attr = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        // Assert: AuthorizeAttribute with Roles = "Cajero"
        Assert.NotNull(attr);
        Assert.Equal("Cajero", attr!.Roles);
    }

    [Fact]
    public void Update_HasAuthorizeWithCajeroRole()
    {
        // Arrange: verify PUT has [Authorize(Roles = "Cajero")]
        var method = typeof(ConfiguracionController).GetMethod("Update");
        var attr = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        // Assert: AuthorizeAttribute with Roles = "Cajero"
        Assert.NotNull(attr);
        Assert.Equal("Cajero", attr!.Roles);
    }

    [Fact]
    public void Get_DoesNotHaveRoleSpecificAuthorize()
    {
        // Arrange: verify GET only has class-level [Authorize], no role-specific
        var method = typeof(ConfiguracionController).GetMethod("Get");
        var attrs = method?.GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .ToList();

        // Assert: no role-specific Authorize on Get (only class-level)
        Assert.Empty(attrs!);
    }

    [Fact]
    public void Controller_HasAuthorizeOnClass()
    {
        // Assert: class-level [Authorize]
        var attr = typeof(ConfiguracionController)
            .GetCustomAttributes(true)
            .OfType<Microsoft.AspNetCore.Authorization.AuthorizeAttribute>()
            .FirstOrDefault();

        Assert.NotNull(attr);
    }

    // ================================================================
    // Triangulation: additional scenarios
    // ================================================================

    [Fact]
    public async Task Create_WithMetodoPagoDefaultId_ReturnsCreatedAtAction()
    {
        // Arrange: create with MetodoPagoDefaultId set
        var createdConfig = new ConfiguracionResponse(
            Id: 2, MetodoPagoDefaultId: 5, MetodoPagoDefaultNombre: "Tarjeta",
            NombreGastronomico: "Local Test", LatitudPartida: -31.5, LongitudPartida: -60.2);
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.CrearAsync(5, "Local Test", -31.5, -60.2))
            .ReturnsAsync(createdConfig);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new CrearConfiguracionRequest(
            MetodoPagoDefaultId: 5, NombreGastronomico: "Local Test",
            LatitudPartida: -31.5, LongitudPartida: -60.2);

        // Act
        var result = await controller.Create(request);

        // Assert
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        var response = Assert.IsType<ConfiguracionResponse>(createdResult.Value!);
        Assert.Equal(5, response.MetodoPagoDefaultId);
        Assert.Equal("Tarjeta", response.MetodoPagoDefaultNombre);
    }

    [Fact]
    public async Task Update_UpdatesOnlyLatitud_ReturnsOk()
    {
        // Arrange: update only LatitudPartida
        var updatedConfig = ConfigResponse with { LatitudPartida = -35.0 };
        var mockService = new Mock<IConfiguracionService>();
        mockService
            .Setup(s => s.ActualizarAsync(null, null, -35.0, null))
            .ReturnsAsync(updatedConfig);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Cajero");
        var request = new ActualizarConfiguracionRequest(
            MetodoPagoDefaultId: null, NombreGastronomico: null,
            LatitudPartida: -35.0, LongitudPartida: null);

        // Act
        var result = await controller.Update(request);

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<ConfiguracionResponse>(okResult.Value!);
        Assert.Equal(-35.0, response.LatitudPartida);
    }
}
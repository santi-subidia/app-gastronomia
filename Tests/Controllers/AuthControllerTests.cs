using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class AuthControllerTests
{
    /// <summary>
    /// Helper to unwrap ActionResult&lt;T&gt; to ObjectResult for unit testing.
    /// When a controller returns ActionResult&lt;T&gt;, the inner result is wrapped.
    /// </summary>
    private static TResult ExtractResult<TResult>(ActionResult<LoginResponse> actionResult) where TResult : class
    {
        // If Result is set, it contains the IActionResult (OkObjectResult, UnauthorizedObjectResult, etc.)
        // If Value is set directly (non-error path at the MVC pipeline level), it's the typed value.
        return Assert.IsType<TResult>(actionResult.Result);
    }

    // ================================================================
    // POST /api/auth/login — Success: returns 200 with LoginResponse
    // ================================================================

    [Fact]
    public async Task Login_ValidCredentials_ReturnsOkWithLoginResponse()
    {
        // Arrange
        var mockService = new Mock<IAuthService>();
        var expectedResponse = new LoginResponse(
            Id: 1,
            UsuarioNombre: "admin",
            RolId: 1,
            RolNombre: "Admin",
            Token: "jwt-token-here",
            ExpiraEn: DateTime.UtcNow.AddHours(8)
        );
        mockService
            .Setup(s => s.LoginAsync("admin", "test123"))
            .ReturnsAsync(expectedResponse);

        var controller = new AuthController(mockService.Object);
        var request = new LoginRequest(UsuarioNombre: "admin", Password: "test123");

        // Act
        var actionResult = await controller.Login(request);

        // Assert: 200 OK with correct LoginResponse
        var okResult = ExtractResult<OkObjectResult>(actionResult);
        var response = Assert.IsType<LoginResponse>(okResult.Value!);
        Assert.Equal("admin", response.UsuarioNombre);
        Assert.Equal("Admin", response.RolNombre);
        Assert.Equal("jwt-token-here", response.Token);
    }

    // ================================================================
    // POST /api/auth/login — Invalid credentials: returns 401
    // ================================================================

    [Fact]
    public async Task Login_InvalidCredentials_ReturnsUnauthorized()
    {
        // Arrange
        var mockService = new Mock<IAuthService>();
        mockService
            .Setup(s => s.LoginAsync("admin", "wrong"))
            .ReturnsAsync((LoginResponse?)null);

        var controller = new AuthController(mockService.Object);
        var request = new LoginRequest(UsuarioNombre: "admin", Password: "wrong");

        // Act
        var actionResult = await controller.Login(request);

        // Assert: 401 Unauthorized
        var unauthorizedResult = ExtractResult<UnauthorizedObjectResult>(actionResult);
        Assert.NotNull(unauthorizedResult);
    }

    // ================================================================
    // POST /api/auth/login — Inactive user: returns 401
    // ================================================================

    [Fact]
    public async Task Login_InactiveUser_ReturnsUnauthorized()
    {
        // Arrange: service returns null for inactive user
        var mockService = new Mock<IAuthService>();
        mockService
            .Setup(s => s.LoginAsync("inactive", "pass123"))
            .ReturnsAsync((LoginResponse?)null);

        var controller = new AuthController(mockService.Object);
        var request = new LoginRequest(UsuarioNombre: "inactive", Password: "pass123");

        // Act
        var actionResult = await controller.Login(request);

        // Assert: 401 Unauthorized (service already returns null for inactive users)
        var unauthorizedResult = ExtractResult<UnauthorizedObjectResult>(actionResult);
        Assert.NotNull(unauthorizedResult);
    }

    // ================================================================
    // Triangulation: Different user, different role
    // ================================================================

    [Fact]
    public async Task Login_DifferentRole_ReturnsCorrectRolNombre()
    {
        // Arrange
        var mockService = new Mock<IAuthService>();
        var cocineroResponse = new LoginResponse(
            Id: 5,
            UsuarioNombre: "chef1",
            RolId: 2,
            RolNombre: "Cocinero",
            Token: "cocinero-jwt-token",
            ExpiraEn: DateTime.UtcNow.AddHours(8)
        );
        mockService
            .Setup(s => s.LoginAsync("chef1", "cocina123"))
            .ReturnsAsync(cocineroResponse);

        var controller = new AuthController(mockService.Object);
        var request = new LoginRequest(UsuarioNombre: "chef1", Password: "cocina123");

        // Act
        var actionResult = await controller.Login(request);

        // Assert: 200 OK with Cocinero role
        var okResult = ExtractResult<OkObjectResult>(actionResult);
        var response = Assert.IsType<LoginResponse>(okResult.Value!);
        Assert.Equal("Cocinero", response.RolNombre);
        Assert.Equal(2, response.RolId);
        Assert.Equal("chef1", response.UsuarioNombre);
    }
}
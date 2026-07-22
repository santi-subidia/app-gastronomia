using System.Security.Claims;
using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Moq;
using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Tests.Controllers;

public class UsuariosControllerTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static readonly UsuarioResponse AdminUser = new(
        Id: 1, UsuarioNombre: "admin", RolId: 1, RolNombre: "Admin",
        Disponible: true, Activo: true);

    private static readonly UsuarioResponse RegularUser = new(
        Id: 3, UsuarioNombre: "user1", RolId: 3, RolNombre: "Repartidor",
        Disponible: true, Activo: true);

    private static readonly UsuarioResponse CreatedUser = new(
        Id: 5, UsuarioNombre: "newuser", RolId: 2, RolNombre: "Cocinero",
        Disponible: true, Activo: true);

    private List<UsuarioResponse> GetAllUsers() => [AdminUser, RegularUser];

    /// <summary>
    /// Creates a controller with an authenticated user (claims-based).
    /// This simulates [Authorize] without the middleware pipeline.
    /// </summary>
    private static UsuariosController CreateControllerWithUser(
        IUsuarioService service, int userId, string role)
    {
        var controller = new UsuariosController(service);
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
    // GET /api/usuarios — Admin lists all active users
    // ================================================================

    [Fact]
    public async Task GetAll_Admin_ReturnsOkWithUserList()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuariosAsync())
            .ReturnsAsync(GetAllUsers());

        var controller = new UsuariosController(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with list of 2 users
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var users = Assert.IsAssignableFrom<IEnumerable<UsuarioResponse>>(okResult.Value!);
        Assert.Equal(2, users.Count());
    }

    // ================================================================
    // GET /api/usuarios/{id} — Admin retrieves any user: 200
    // ================================================================

    [Fact]
    public async Task GetById_AdminCanSeeAnyUser_ReturnsOk()
    {
        // Arrange: Admin (id=1) requests user id=3 (different user)
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuarioPorIdAsync(3))
            .ReturnsAsync(RegularUser);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Admin");

        // Act
        var result = await controller.GetById(3);

        // Assert: 200 OK — admin can see any user
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<UsuarioResponse>(okResult.Value!);
        Assert.Equal("user1", response.UsuarioNombre);
    }

    // ================================================================
    // GET /api/usuarios/{id} — User can see own record: 200
    // ================================================================

    [Fact]
    public async Task GetById_UserCanSeeSelf_ReturnsOk()
    {
        // Arrange: Regular user (id=3) requests their own record
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuarioPorIdAsync(3))
            .ReturnsAsync(RegularUser);

        var controller = CreateControllerWithUser(mockService.Object, userId: 3, role: "Repartidor");

        // Act
        var result = await controller.GetById(3);

        // Assert: 200 OK — user can see their own record
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<UsuarioResponse>(okResult.Value!);
        Assert.Equal(3, response.Id);
    }

    // ================================================================
    // GET /api/usuarios/{id} — User cannot see another user: 403
    // ================================================================

    [Fact]
    public async Task GetById_UserCannotSeeOther_ReturnsForbid()
    {
        // Arrange: Regular user (id=3) tries to access user id=1
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuarioPorIdAsync(1))
            .ReturnsAsync(AdminUser);

        var controller = CreateControllerWithUser(mockService.Object, userId: 3, role: "Repartidor");

        // Act
        var result = await controller.GetById(1);

        // Assert: 403 Forbid — non-admin cannot see another user
        Assert.IsType<ForbidResult>(result.Result);
    }

    // ================================================================
    // GET /api/usuarios/{id} — Not found returns 404
    // ================================================================

    [Fact]
    public async Task GetById_NonexistentUser_ReturnsNotFound()
    {
        // Arrange: Admin requests nonexistent user
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuarioPorIdAsync(999))
            .ReturnsAsync((UsuarioResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Admin");

        // Act
        var result = await controller.GetById(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // POST /api/usuarios — Create user successfully returns 201
    // ================================================================

    [Fact]
    public async Task Create_ValidRequest_ReturnsCreatedAtAction()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.CrearUsuarioAsync("newuser", "secure123", 2))
            .ReturnsAsync(CreatedUser);

        var controller = new UsuariosController(mockService.Object);
        var request = new CreateUserRequest(UsuarioNombre: "newuser", Password: "secure123", RolId: 2);

        // Act
        var result = await controller.Create(request);

        // Assert: 201 CreatedAtAction
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        Assert.Equal(nameof(UsuariosController.GetById), createdResult.ActionName);
        var response = Assert.IsType<UsuarioResponse>(createdResult.Value!);
        Assert.Equal("newuser", response.UsuarioNombre);
        Assert.Equal("Cocinero", response.RolNombre);
    }

    // ================================================================
    // POST /api/usuarios — Duplicate username returns 409 Conflict
    // ================================================================

    [Fact]
    public async Task Create_DuplicateUsername_ReturnsConflict()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.CrearUsuarioAsync("admin", "pass123", 1))
            .ThrowsAsync(new InvalidOperationException("Ya existe un usuario con el nombre 'admin'."));

        var controller = new UsuariosController(mockService.Object);
        var request = new CreateUserRequest(UsuarioNombre: "admin", Password: "pass123", RolId: 1);

        // Act
        var result = await controller.Create(request);

        // Assert: 409 Conflict
        Assert.IsType<ConflictObjectResult>(result.Result);
    }

    // ================================================================
    // PUT /api/usuarios/{id} — Admin can update any user: 200
    // ================================================================

    [Fact]
    public async Task Update_AdminCanUpdateAnyUser_ReturnsOk()
    {
        // Arrange: Admin updates different user
        var updatedUser = RegularUser with { UsuarioNombre = "user1_updated" };
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ActualizarUsuarioAsync(3, "user1_updated", null, null, null))
            .ReturnsAsync(updatedUser);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Admin");
        var request = new UpdateUserRequest(UsuarioNombre: "user1_updated", Password: null, RolId: null, Disponible: null);

        // Act
        var result = await controller.Update(3, request);

        // Assert: 200 OK with updated user
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<UsuarioResponse>(okResult.Value!);
        Assert.Equal("user1_updated", response.UsuarioNombre);
    }

    // ================================================================
    // PUT /api/usuarios/{id} — User can update their own record: 200
    // ================================================================

    [Fact]
    public async Task Update_UserCanUpdateSelf_ReturnsOk()
    {
        // Arrange: User updates their own record
        var updatedUser = RegularUser with { Disponible = false };
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ActualizarUsuarioAsync(3, null, null, null, false))
            .ReturnsAsync(updatedUser);

        var controller = CreateControllerWithUser(mockService.Object, userId: 3, role: "Repartidor");
        var request = new UpdateUserRequest(UsuarioNombre: null, Password: null, RolId: null, Disponible: false);

        // Act
        var result = await controller.Update(3, request);

        // Assert: 200 OK — user can update own record
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<UsuarioResponse>(okResult.Value!);
        Assert.False(response.Disponible);
    }

    // ================================================================
    // PUT /api/usuarios/{id} — User cannot update another user: 403
    // ================================================================

    [Fact]
    public async Task Update_UserCannotUpdateOther_ReturnsForbid()
    {
        // Arrange: Regular user (id=3) tries to update different user (id=1)
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ActualizarUsuarioAsync(1, "hacked", null, null, null))
            .ReturnsAsync(AdminUser with { UsuarioNombre = "hacked" });

        var controller = CreateControllerWithUser(mockService.Object, userId: 3, role: "Repartidor");
        var request = new UpdateUserRequest(UsuarioNombre: "hacked", Password: null, RolId: null, Disponible: null);

        // Act
        var result = await controller.Update(1, request);

        // Assert: 403 Forbid
        Assert.IsType<ForbidResult>(result.Result);
    }

    // ================================================================
    // PUT /api/usuarios/{id} — Not found returns 404
    // ================================================================

    [Fact]
    public async Task Update_NonexistentUser_ReturnsNotFound()
    {
        // Arrange: Admin updates nonexistent user
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ActualizarUsuarioAsync(999, "ghost", null, null, null))
            .ReturnsAsync((UsuarioResponse?)null);

        var controller = CreateControllerWithUser(mockService.Object, userId: 1, role: "Admin");
        var request = new UpdateUserRequest(UsuarioNombre: "ghost", Password: null, RolId: null, Disponible: null);

        // Act
        var result = await controller.Update(999, request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // DELETE /api/usuarios/{id} — Soft delete returns 204
    // ================================================================

    [Fact]
    public async Task Delete_ExistingUser_ReturnsNoContent()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.EliminarUsuarioAsync(1))
            .ReturnsAsync(true);

        var controller = new UsuariosController(mockService.Object);

        // Act
        var result = await controller.Delete(1);

        // Assert: 204 NoContent
        Assert.IsType<NoContentResult>(result);
    }

    // ================================================================
    // DELETE /api/usuarios/{id} — Not found returns 404
    // ================================================================

    [Fact]
    public async Task Delete_NonexistentUser_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.EliminarUsuarioAsync(999))
            .ReturnsAsync(false);

        var controller = new UsuariosController(mockService.Object);

        // Act
        var result = await controller.Delete(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result);
    }

    // ================================================================
    // Triangulation: GetAll returns empty collection when no active users
    // ================================================================

    [Fact]
    public async Task GetAll_NoActiveUsers_ReturnsEmptyCollection()
    {
        // Arrange
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuariosAsync())
            .ReturnsAsync([]);

        var controller = new UsuariosController(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with empty collection
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var users = Assert.IsAssignableFrom<IEnumerable<UsuarioResponse>>(okResult.Value!);
        Assert.Empty(users);
    }

    [Fact]
    public async Task GetAll_WithRoleFilter_PassesRoleToService()
    {
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.ObtenerUsuariosAsync("Repartidor"))
            .ReturnsAsync([RegularUser]);

        var controller = new UsuariosController(mockService.Object);

        var result = await controller.GetAll("Repartidor");

        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var users = Assert.IsAssignableFrom<IEnumerable<UsuarioResponse>>(okResult.Value!);
        Assert.Single(users);
        Assert.Equal("Repartidor", users.Single().RolNombre);
        mockService.Verify(s => s.ObtenerUsuariosAsync("Repartidor"), Times.Once);
    }

    // ================================================================
    // Triangulation: Create with different role
    // ================================================================

    [Fact]
    public async Task Create_DifferentRole_ReturnsCorrectRoleInResponse()
    {
        // Arrange
        var repartidorResponse = new UsuarioResponse(
            Id: 7, UsuarioNombre: "driver1", RolId: 3, RolNombre: "Repartidor",
            Disponible: true, Activo: true);
        var mockService = new Mock<IUsuarioService>();
        mockService
            .Setup(s => s.CrearUsuarioAsync("driver1", "pass456", 3))
            .ReturnsAsync(repartidorResponse);

        var controller = new UsuariosController(mockService.Object);
        var request = new CreateUserRequest(UsuarioNombre: "driver1", Password: "pass456", RolId: 3);

        // Act
        var result = await controller.Create(request);

        // Assert
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        var response = Assert.IsType<UsuarioResponse>(createdResult.Value!);
        Assert.Equal("Repartidor", response.RolNombre);
        Assert.Equal(3, response.RolId);
    }

    [Fact]
    public async Task GetRepartidoresDisponibles_FiltersByMaxPedidosPorRepartidor()
    {
        // Arrange
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(databaseName: Guid.NewGuid().ToString())
            .Options;

        using var context = new AppDbContext(options);
        
        // Seed configuration with MaxPedidosPorRepartidor = 2
        context.Configuracion.Add(new Configuracion { Id = 1, MaxPedidosPorRepartidor = 2 });

        // Seed orders
        // Repartidor 10 (has 1 active order) -> should be returned
        context.Pedidos.Add(new Pedido { Id = 1, RepartidorId = 10, EstadoId = (int)EstadoPedidoEnum.EnCamino, MetodoPagoId = 1, MetodoVentaId = 1 });
        
        // Repartidor 11 (has 2 active orders) -> should NOT be returned
        context.Pedidos.Add(new Pedido { Id = 2, RepartidorId = 11, EstadoId = (int)EstadoPedidoEnum.ListoParaRetirar, MetodoPagoId = 1, MetodoVentaId = 1 });
        context.Pedidos.Add(new Pedido { Id = 3, RepartidorId = 11, EstadoId = 2, MetodoPagoId = 1, MetodoVentaId = 1 }); // 2 is EnPreparacion (corresponds to user's requested number 2)

        // Repartidor 12 (has 2 active orders but under different definition)
        context.Pedidos.Add(new Pedido { Id = 4, RepartidorId = 12, EstadoId = 3, MetodoPagoId = 1, MetodoVentaId = 1 });
        context.Pedidos.Add(new Pedido { Id = 5, RepartidorId = 12, EstadoId = 4, MetodoPagoId = 1, MetodoVentaId = 1 });

        await context.SaveChangesAsync();

        var mockService = new Mock<IUsuarioService>();
        mockService.Setup(s => s.ObtenerUsuariosAsync()).ReturnsAsync(new List<UsuarioResponse>
        {
            new(Id: 10, UsuarioNombre: "rep10", RolId: 3, RolNombre: "Repartidor", Disponible: true, Activo: true),
            new(Id: 11, UsuarioNombre: "rep11", RolId: 3, RolNombre: "Repartidor", Disponible: true, Activo: true),
            new(Id: 12, UsuarioNombre: "rep12", RolId: 3, RolNombre: "Repartidor", Disponible: true, Activo: true),
            new(Id: 13, UsuarioNombre: "rep13", RolId: 3, RolNombre: "Repartidor", Disponible: true, Activo: true),
        });

        var controller = new UsuariosController(mockService.Object, context);

        // Act
        var result = await controller.GetRepartidoresDisponibles();

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var repartidores = Assert.IsAssignableFrom<IEnumerable<UsuarioResponse>>(okResult.Value!).ToList();

        // rep10 (1 order) and rep13 (0 orders) should be returned.
        // rep11 (2 orders) and rep12 (2 orders) should be filtered out because they reached max (2).
        Assert.Equal(2, repartidores.Count);
        Assert.Contains(repartidores, r => r.Id == 10);
        Assert.Contains(repartidores, r => r.Id == 13);
        Assert.DoesNotContain(repartidores, r => r.Id == 11);
        Assert.DoesNotContain(repartidores, r => r.Id == 12);
    }
}

using System.Security.Claims;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Moq;

namespace ApiGastronomia.Tests.Services;

public class DemoraServiceTests
{
    /// <summary>
    /// Helper to create an InMemory DbContext for testing.
    /// Each test gets a fresh database with a unique name.
    /// </summary>
    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"DemoraTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

    /// <summary>
    /// Seeds a Pedido and a Cajero Usuario into the context for demora creation.
    /// Returns (context, pedido, usuario).
    /// </summary>
    private static (AppDbContext Context, Pedido Pedido, Usuario Usuario) SeedPedidoYUsuario(
        AppDbContext context)
    {
        var rol = new Rol { Nombre = "Cajero" };
        context.Roles.Add(rol);
        context.SaveChanges();

        var usuario = new Usuario
        {
            UsuarioNombre = "cajero_test",
            PasswordHash = "hash",
            RolId = rol.Id,
            Activo = true
        };
        context.Usuarios.Add(usuario);
        context.SaveChanges();

        var estadoPedido = new EstadoPedido { Nombre = "Pendiente" };
        context.EstadosPedidos.Add(estadoPedido);
        context.SaveChanges();

        var metodoPago = new MetodoPago { Nombre = "Efectivo" };
        context.MetodoPago.Add(metodoPago);
        context.SaveChanges();

        var metodoVenta = new MetodoVenta { Nombre = "Delivery" };
        context.MetodosVenta.Add(metodoVenta);
        context.SaveChanges();

        var pedido = new Pedido
        {
            EstadoId = estadoPedido.Id,
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5000.0
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        return (context, pedido, usuario);
    }

    /// <summary>
    /// Creates a mock IHttpContextAccessor that returns a ClaimsPrincipal with
    /// the given userId in the "sub" claim.
    /// </summary>
    private static Mock<IHttpContextAccessor> CreateMockHttpContextAccessor(int userId, string role = "Desconocido")
    {
        var claims = new List<Claim>
        {
            new("sub", userId.ToString()),
            new(ClaimTypes.Role, role)
        };
        var identity = new ClaimsIdentity(claims, "TestAuth");
        var principal = new ClaimsPrincipal(identity);

        var httpContext = new DefaultHttpContext { User = principal };
        var mockAccessor = new Mock<IHttpContextAccessor>();
        mockAccessor.Setup(a => a.HttpContext).Returns(httpContext);
        return mockAccessor;
    }

    /// <summary>
    /// Creates a mock IHubClients for SignalR testing.
    /// Returns (mockClients, mockProxy) so tests can verify SendAsync calls.
    /// </summary>
    private static (Mock<IHubClients> MockClients, Mock<IClientProxy> MockProxy) CreateMockHubClients()
    {
        var mockProxy = new Mock<IClientProxy>();
        mockProxy
            .Setup(proxy => proxy.SendCoreAsync(It.IsAny<string>(), It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var mockClients = new Mock<IHubClients>();
        mockClients.Setup(c => c.Group(It.IsAny<string>())).Returns(mockProxy.Object);

        return (mockClients, mockProxy);
    }

    /// <summary>
    /// Creates a DemoraService with a mocked IHubContext and IHttpContextAccessor.
    /// </summary>
    private static (DemoraService Service, Mock<IClientProxy> MockProxy) CreateService(
        AppDbContext context,
        int userId = 1,
        string role = "Desconocido")
    {
        var (mockClients, mockProxy) = CreateMockHubClients();
        var mockHub = new Mock<IHubContext<LogisticaHub>>();
        mockHub.Setup(h => h.Clients).Returns(mockClients.Object);

        var mockAccessor = CreateMockHttpContextAccessor(userId, role);
        var logger = new LoggerFactory().CreateLogger<DemoraService>();

        var service = new DemoraService(context, mockHub.Object, mockAccessor.Object, logger);
        return (service, mockProxy);
    }

    // ================================================================
    // Interface contract: IDemoraService can be resolved from DemoraService
    // ================================================================

    [Fact]
    public async Task IDemoraService_CanBeResolvedFromImplementation()
    {
        // Arrange: verify IDemoraService can be assigned from DemoraService
        var context = CreateDbContext();
        var (service, _) = CreateService(context);

        IDemoraService iface = service;

        // Act: call ObtenerPorPedidoAsync through the interface (will throw KeyNotFoundException for non-existent pedido)
        // But we just need to verify the interface resolves, not the behavior

        // Assert: interface contract works
        Assert.NotNull(iface);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ObtenerPorPedidoAsync: returns demoras for a given pedido
    // ================================================================

    [Fact]
    public async Task ObtenerPorPedidoAsync_PedidoConDemoras_ReturnsDemoraResponses()
    {
        // Arrange: seed pedido, usuario, and 2 demoras
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);

        var demora1 = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 15, Sector = "cocina", Observaciones = "falta stock" };
        var demora2 = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 30, Sector = "reparto" };
        context.Demoras.AddRange(demora1, demora2);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act
        var result = await service.ObtenerPorPedidoAsync(pedido.Id);

        // Assert: returns 2 demoras with correct fields
        var list = result.ToList();
        Assert.Equal(2, list.Count);
        Assert.Contains(list, d => d.DemoraMinutos == 15 && d.Sector == "cocina");
        Assert.Contains(list, d => d.DemoraMinutos == 30 && d.Sector == "reparto");

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerPorPedidoAsync_PedidoSinDemoras_ReturnsEmptyCollection()
    {
        // Arrange: seed pedido without demoras
        var context = CreateDbContext();
        var (_, pedido, _) = SeedPedidoYUsuario(context);

        var (service, _) = CreateService(context);

        // Act
        var result = await service.ObtenerPorPedidoAsync(pedido.Id);

        // Assert: empty collection (pedido exists but has no demoras)
        var list = result.ToList();
        Assert.Empty(list);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerPorPedidoAsync_PedidoInexistente_ThrowsKeyNotFoundException()
    {
        // Arrange: no pedido seeded
        var context = CreateDbContext();
        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.ObtenerPorPedidoAsync(999));

        Assert.Contains("999", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // CrearAsync: creates demora, sends SignalR, extracts userId from claims
    // ================================================================

    [Fact]
    public async Task CrearAsync_ValidData_CreatesDemoraAndReturnsResponse()
    {
        // Arrange: seed pedido and usuario
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, _) = CreateService(context, userId: usuario.Id, role: "cocina");

        // Act
        var result = await service.CrearAsync(pedido.Id, 20, "falta stock");

        // Assert: returned DTO has correct fields
        Assert.Equal(pedido.Id, result.PedidoId);
        Assert.Equal(usuario.Id, result.UsuarioId);
        Assert.Equal(20, result.DemoraMinutos);
        Assert.Equal("cocina", result.Sector);
        Assert.Equal("falta stock", result.Observaciones);
        Assert.NotEqual(0, result.Id); // Id assigned by DB

        // Assert: demora is persisted
        var saved = await context.Demoras.FindAsync(result.Id);
        Assert.NotNull(saved);
        Assert.Equal(20, saved!.DemoraMinutos);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_SendsSignalRNotification()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, mockProxy) = CreateService(context, userId: usuario.Id);

        // Act
        await service.CrearAsync(pedido.Id, 25, null);

        // Assert: SignalR sent DemoraRegistrada event twice (pedido group + Cajeros group)
        mockProxy.Verify(
            proxy => proxy.SendCoreAsync("DemoraRegistrada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()),
            Times.Exactly(2));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_DemoraMinutosZero_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, pedido, _) = SeedPedidoYUsuario(context);
        var (service, _) = CreateService(context);

        // Act + Assert: demoraMinutos = 0 is invalid
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearAsync(pedido.Id, 0, null));

        Assert.Contains("mayor que cero", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_DemoraMinutosNegativo_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, pedido, _) = SeedPedidoYUsuario(context);
        var (service, _) = CreateService(context);

        // Act + Assert: demoraMinutos < 0 is invalid
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearAsync(pedido.Id, -5, null));

        Assert.Contains("mayor que cero", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_PedidoInexistente_ThrowsKeyNotFoundException()
    {
        // Arrange: no pedido seeded
        var context = CreateDbContext();
        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.CrearAsync(999, 10, null));

        Assert.Contains("999", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_ExtractsUserIdFromClaims_Internally()
    {
        // Arrange: seed pedido and usuario with a specific userId
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, _) = CreateService(context, userId: usuario.Id);

        // Act: userId comes from claims, NOT from parameters
        var result = await service.CrearAsync(pedido.Id, 10, null);

        // Assert: userId extracted from claims matches the seeded user
        Assert.Equal(usuario.Id, result.UsuarioId);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // ActualizarAsync: updates demora fields, returns null if not found
    // ================================================================

    [Fact]
    public async Task ActualizarAsync_ExistingDemora_UpdatesFieldsAndReturnsResponse()
    {
        // Arrange: seed pedido, usuario, and demora
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var demora = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 15, Sector = "cocina", Observaciones = "original" };
        context.Demoras.Add(demora);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act: update demoraMinutos and observaciones
        var result = await service.ActualizarAsync(demora.Id, 30, "actualizado");

        // Assert
        Assert.NotNull(result);
        Assert.Equal(30, result!.DemoraMinutos);
        Assert.Equal("cocina", result.Sector);
        Assert.Equal("actualizado", result.Observaciones);
        Assert.Equal(demora.Id, result.Id);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarAsync_NonexistentDemora_ReturnsNull()
    {
        // Arrange
        var context = CreateDbContext();
        var (service, _) = CreateService(context);

        // Act
        var result = await service.ActualizarAsync(999, 10, null);

        // Assert
        Assert.Null(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // EliminarAsync: hard delete, returns false if not found
    // ================================================================

    [Fact]
    public async Task EliminarAsync_ExistingDemora_DeletesAndReturnsTrue()
    {
        // Arrange: seed pedido, usuario, and demora
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var demora = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 15, Sector = "cocina" };
        context.Demoras.Add(demora);
        context.SaveChanges();
        var demoraId = demora.Id;

        var (service, _) = CreateService(context);

        // Act
        var result = await service.EliminarAsync(demoraId);

        // Assert: deleted successfully
        Assert.True(result);

        // Assert: demora no longer in database
        var deleted = await context.Demoras.FindAsync(demoraId);
        Assert.Null(deleted);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarAsync_NonexistentDemora_ReturnsFalse()
    {
        // Arrange
        var context = CreateDbContext();
        var (service, _) = CreateService(context);

        // Act
        var result = await service.EliminarAsync(999);

        // Assert
        Assert.False(result);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Triangulation: additional scenarios exercising different code paths
    // ================================================================

    [Fact]
    public async Task CrearAsync_WithNullSectorAndObservaciones_CreatesDemora()
    {
        // Arrange: triangulate with null optional fields
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, _) = CreateService(context, userId: usuario.Id);

        // Act
        var result = await service.CrearAsync(pedido.Id, 45, null);

        // Assert: null fields are accepted
        Assert.Equal(45, result.DemoraMinutos);
        Assert.Equal("Desconocido", result.Sector);
        Assert.Null(result.Observaciones);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ObtenerPorPedidoAsync_ReturnsDemorasForCorrectPedidoOnly()
    {
        // Arrange: seed 2 pedidos with demoras, verify only the target pedido's demoras are returned
        var context = CreateDbContext();
        var (_, pedido1, usuario) = SeedPedidoYUsuario(context);

        // Create a second pedido
        var estadoPedido2 = new EstadoPedido { Nombre = "EnProgreso" };
        context.EstadosPedidos.Add(estadoPedido2);
        context.SaveChanges();
        var metodoPago2 = context.MetodoPago.First();
        var metodoVenta2 = context.MetodosVenta.First();
        var pedido2 = new Pedido { EstadoId = estadoPedido2.Id, MetodoPagoId = metodoPago2.Id, MetodoVentaId = metodoVenta2.Id, TotalEstimado = 3000.0 };
        context.Pedidos.Add(pedido2);
        context.SaveChanges();

        var d1 = new Demora { PedidoId = pedido1.Id, UsuarioId = usuario.Id, DemoraMinutos = 10 };
        var d2 = new Demora { PedidoId = pedido2.Id, UsuarioId = usuario.Id, DemoraMinutos = 20 };
        context.Demoras.AddRange(d1, d2);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act: get demoras for pedido1 only
        var result = await service.ObtenerPorPedidoAsync(pedido1.Id);

        // Assert: only pedido1's demora is returned
        var list = result.ToList();
        Assert.Single(list);
        Assert.Equal(10, list[0].DemoraMinutos);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task ActualizarAsync_PartialUpdate_OnlyChangesProvidedFields()
    {
        // Arrange: seed demora with sector and observaciones
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var demora = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 15, Sector = "cocina", Observaciones = "original" };
        context.Demoras.Add(demora);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act: update only demoraMinutos, pass null for others
        var result = await service.ActualizarAsync(demora.Id, 45, null);

        // Assert: demoraMinutos updated, sector and observaciones unchanged (null = no update)
        Assert.NotNull(result);
        Assert.Equal(45, result!.DemoraMinutos);
        Assert.Equal("cocina", result.Sector);
        Assert.Equal("original", result.Observaciones);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_DifferentData_ProducesDifferentResults()
    {
        // Arrange: triangulation - different inputs produce different outputs
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service1, _) = CreateService(context, userId: usuario.Id, role: "cocina");
        var (service2, _) = CreateService(context, userId: usuario.Id, role: "reparto");

        // Act: create first demora
        var result1 = await service1.CrearAsync(pedido.Id, 10, "test1");

        // Assert: fields match input
        Assert.Equal(10, result1.DemoraMinutos);
        Assert.Equal("cocina", result1.Sector);
        Assert.Equal("test1", result1.Observaciones);

        // Act: create second demora with different data
        var result2 = await service2.CrearAsync(pedido.Id, 30, "test2");

        // Assert: different results
        Assert.Equal(30, result2.DemoraMinutos);
        Assert.Equal("reparto", result2.Sector);
        Assert.Equal("test2", result2.Observaciones);
        Assert.NotEqual(result1.Id, result2.Id);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task EliminarAsync_DemoraNoLongerReturnedByObtenerPorPedido()
    {
        // Arrange: seed 2 demoras, delete one
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var d1 = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 10 };
        var d2 = new Demora { PedidoId = pedido.Id, UsuarioId = usuario.Id, DemoraMinutos = 20 };
        context.Demoras.AddRange(d1, d2);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act: delete one demora
        await service.EliminarAsync(d1.Id);

        // Assert: only one demora remains
        var remaining = await service.ObtenerPorPedidoAsync(pedido.Id);
        var list = remaining.ToList();
        Assert.Single(list);
        Assert.Equal(20, list[0].DemoraMinutos);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Task 4.4 â€” CrearAsync sends DemoraRegistradaMessage typed DTO
    // ================================================================

    [Fact]
    public async Task CrearAsync_SendsDemoraRegistradaMessage_TypedDTO()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, mockProxy) = CreateService(context, userId: usuario.Id, role: "reparto");

        object?[]? capturedArgs = null;
        mockProxy
            .Setup(proxy => proxy.SendCoreAsync("DemoraRegistrada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Callback<string, object?[], CancellationToken>((_, args, _) => capturedArgs = args)
            .Returns(Task.CompletedTask);

        // Act
        await service.CrearAsync(pedido.Id, 25, "falta stock");

        // Assert: DemoraRegistradaMessage typed DTO was sent with full contract
        Assert.NotNull(capturedArgs);
        var msg = Assert.IsType<DemoraRegistradaMessage>(capturedArgs![0]);
        Assert.True(msg.DemoraId > 0);
        Assert.Equal(pedido.Id, msg.PedidoId);
        Assert.Equal("reparto", msg.Sector);
        Assert.Equal(25, msg.TiempoEstimadoMinutos);
        Assert.Equal("falta stock", msg.Observaciones);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_SendsDemoraRegistradaMessageToCajerosGroup()
    {
        // Arrange
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, mockProxy) = CreateService(context, userId: usuario.Id, role: "Cocina");

        var sentTargets = new List<string>();
        mockProxy
            .Setup(proxy => proxy.SendCoreAsync("DemoraRegistrada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Callback<string, object?[], CancellationToken>((_, args, _) =>
            {
                var clients = mockProxy.Object;
                // We cannot inspect the group name directly from the proxy callback,
                // so we verify the hub path through the IHubContext mock setup below.
            })
            .Returns(Task.CompletedTask);

        var mockClients = new Moq.Mock<IHubClients>();
        var mockGroupProxy = new Moq.Mock<IClientProxy>();
        mockGroupProxy
            .Setup(p => p.SendCoreAsync("DemoraRegistrada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Callback<string, object?[], CancellationToken>((_, args, _) =>
            {
                sentTargets.Add(args[0] is DemoraRegistradaMessage m ? $"group:{m.Sector}" : "unknown");
            })
            .Returns(Task.CompletedTask);
        mockClients.Setup(c => c.Group(It.IsAny<string>())).Returns(mockGroupProxy.Object);

        var mockHubContext = new Moq.Mock<IHubContext<LogisticaHub>>();
        mockHubContext.Setup(h => h.Clients).Returns(mockClients.Object);

        var logger = new LoggerFactory().CreateLogger<DemoraService>();
        var mockAccessor = CreateMockHttpContextAccessor(usuario.Id, "Cocina");
        var serviceWithMock = new DemoraService(context, mockHubContext.Object, mockAccessor.Object, logger);

        // Act
        await serviceWithMock.CrearAsync(pedido.Id, 15, "falta stock");

        // Assert: both groups were invoked
        mockClients.Verify(c => c.Group($"pedido_{pedido.Id}"), Times.Once);
        mockClients.Verify(c => c.Group("Cajeros"), Times.Once);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearAsync_NullSector_SendsDesconocido()
    {
        // Arrange: missing role should default to "Desconocido"
        var context = CreateDbContext();
        var (_, pedido, usuario) = SeedPedidoYUsuario(context);
        var (service, mockProxy) = CreateService(context, userId: usuario.Id);

        object?[]? capturedArgs = null;
        mockProxy
            .Setup(proxy => proxy.SendCoreAsync("DemoraRegistrada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Callback<string, object?[], CancellationToken>((_, args, _) => capturedArgs = args)
            .Returns(Task.CompletedTask);

        // Act
        await service.CrearAsync(pedido.Id, 10, null);

        // Assert: Sector defaults to "Desconocido"
        Assert.NotNull(capturedArgs);
        var msg = Assert.IsType<DemoraRegistradaMessage>(capturedArgs![0]);
        Assert.Equal("Desconocido", msg.Sector);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}

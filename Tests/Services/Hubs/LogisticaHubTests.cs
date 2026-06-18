using System.Reflection;
using System.Security.Claims;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Hubs;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Logging;
using Moq;

namespace ApiGastronomia.Tests.Services.Hubs;

public class LogisticaHubTests
{
    /// <summary>
    /// Creates a LogisticaHub with mocked dependencies and a ClaimsPrincipal
    /// containing the specified roles. Claims use ClaimTypes.Role so
    /// IsInRole() works correctly, matching JWT middleware behavior.
    /// </summary>
    private static LogisticaHub CreateHubWithRoles(params string[] roles)
    {
        var claims = roles.Select(r => new Claim(ClaimTypes.Role, r)).ToList();
        claims.Add(new Claim("sub", "1"));
        claims.Add(new Claim("unique_name", "testuser"));
        var identity = new ClaimsIdentity(claims, "TestAuth");
        var user = new ClaimsPrincipal(identity);

        var mockContext = new Mock<HubCallerContext>();
        mockContext.Setup(c => c.ConnectionId).Returns("test-connection");
        mockContext.Setup(c => c.User).Returns(user);

        var mockGroups = new Mock<IGroupManager>();
        mockGroups
            .Setup(g => g.AddToGroupAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        mockGroups
            .Setup(g => g.RemoveFromGroupAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var logger = new LoggerFactory().CreateLogger<LogisticaHub>();
        var hub = new LogisticaHub(logger) { Context = mockContext.Object, Groups = mockGroups.Object };

        return hub;
    }

    private static LogisticaHub CreateHubWithClients(params string[] roles)
    {
        var hub = CreateHubWithRoles(roles);

        var mockClients = new Mock<IHubCallerClients>();
        var mockProxy = new Mock<IClientProxy>();
        mockProxy
            .Setup(p => p.SendCoreAsync(It.IsAny<string>(), It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        mockClients.Setup(c => c.Group(It.IsAny<string>())).Returns(mockProxy.Object);
        hub.Clients = mockClients.Object;

        return hub;
    }

    /// <summary>
    /// Creates a LogisticaHub with mocked dependencies and IClientProxy for verification.
    /// </summary>
    private static (LogisticaHub Hub, Mock<IClientProxy> MockProxy) CreateHubWithProxy(params string[] roles)
    {
        var hub = CreateHubWithRoles(roles);

        var mockProxy = new Mock<IClientProxy>();
        mockProxy
            .Setup(p => p.SendCoreAsync(It.IsAny<string>(), It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var mockClients = new Mock<IHubCallerClients>();
        mockClients.Setup(c => c.Group(It.IsAny<string>())).Returns(mockProxy.Object);
        hub.Clients = mockClients.Object;

        return (hub, mockProxy);
    }

    // ================================================================
    // Task 2.1: [Authorize] attribute presence
    // ================================================================

    [Fact]
    public void LogisticaHub_HasAuthorizeAttribute()
    {
        // Assert: the Hub class must carry [Authorize] to enforce JWT auth
        var attribute = typeof(LogisticaHub).GetCustomAttribute<AuthorizeAttribute>();
        Assert.NotNull(attribute);
    }

    // ================================================================
    // Task 2.3: UnirseAGrupo — cocina group role gate
    // ================================================================

    [Fact]
    public async Task UnirseAGrupo_CocinaGroup_WithCocinaRole_Succeeds()
    {
        var hub = CreateHubWithRoles("Cocina");

        // Should not throw — Cocina role is allowed in "cocina" group
        await hub.UnirseAGrupo("cocina");
    }

    [Fact]
    public async Task UnirseAGrupo_CocinaGroup_WithCajeroRole_Succeeds()
    {
        var hub = CreateHubWithRoles("Cajero");

        // Should not throw — Cajero role is allowed in "cocina" group
        await hub.UnirseAGrupo("cocina");
    }

    [Fact]
    public async Task UnirseAGrupo_CocinaGroup_WithRepartidorRole_ThrowsHubException()
    {
        var hub = CreateHubWithRoles("Repartidor");

        var ex = await Assert.ThrowsAsync<HubException>(() => hub.UnirseAGrupo("cocina"));
        Assert.Contains("permiso", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    // ================================================================
    // Task 2.3: UnirseAGrupo — pedido_repartidor group role gate
    // ================================================================

    [Fact]
    public async Task UnirseAGrupo_RepartidorGroup_WithRepartidorRole_Succeeds()
    {
        var hub = CreateHubWithRoles("Repartidor");

        // Should not throw — Repartidor role can join pedido_repartidor_{id} tracking group
        await hub.UnirseAGrupo("pedido_repartidor_5");
    }

    [Fact]
    public async Task UnirseAGrupo_RepartidorGroup_WithCocinaRole_ThrowsHubException()
    {
        var hub = CreateHubWithRoles("Cocina");

        var ex = await Assert.ThrowsAsync<HubException>(() => hub.UnirseAGrupo("pedido_repartidor_5"));
        Assert.Contains("permiso", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    // ================================================================
    // Task 2.3: UnirseAGrupo — other groups open to any authenticated user
    // ================================================================

    [Fact]
    public async Task UnirseAGrupo_PedidoGroup_AnyAuthenticatedUser_Succeeds()
    {
        var hub = CreateHubWithRoles("Cajero");

        // "pedido_42" group is open to any authenticated user
        await hub.UnirseAGrupo("pedido_42");
    }

    [Fact]
    public async Task UnirseAGrupo_PedidoGroup_RepartidorUser_Succeeds()
    {
        var hub = CreateHubWithRoles("Repartidor");

        // Any authenticated user can join a pedido group
        await hub.UnirseAGrupo("pedido_42");
    }

    // ================================================================
    // Task 2.3: EnviarPosicionGPS — Repartidor role gate
    // ================================================================

    [Fact]
    public async Task EnviarPosicionGPS_WithRepartidorRole_Succeeds()
    {
        var hub = CreateHubWithClients("Repartidor");

        // Should not throw — Repartidor role is allowed to send GPS position
        await hub.EnviarPosicionGPS(1, -34.6, -58.4);
    }

    [Fact]
    public async Task EnviarPosicionGPS_WithCocinaRole_ThrowsHubException()
    {
        var hub = CreateHubWithClients("Cocina");

        var ex = await Assert.ThrowsAsync<HubException>(() => hub.EnviarPosicionGPS(1, -34.6, -58.4));
        Assert.Contains("repartidor", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task EnviarPosicionGPS_WithCajeroRole_ThrowsHubException()
    {
        var hub = CreateHubWithClients("Cajero");

        var ex = await Assert.ThrowsAsync<HubException>(() => hub.EnviarPosicionGPS(1, -34.6, -58.4));
        Assert.Contains("repartidor", ex.Message, StringComparison.OrdinalIgnoreCase);
    }

    // ================================================================
    // Task 5.1: Removed Notificar* methods — verify they don't exist
    // ================================================================

    [Fact]
    public void LogisticaHub_DoesNotHave_NotificarCambioEstado()
    {
        // Assert: NotificarCambioEstado should NOT exist as a public method
        var method = typeof(LogisticaHub).GetMethod("NotificarCambioEstado",
            BindingFlags.Public | BindingFlags.Instance);
        Assert.Null(method);
    }

    [Fact]
    public void LogisticaHub_DoesNotHave_NotificarNuevoPedido()
    {
        var method = typeof(LogisticaHub).GetMethod("NotificarNuevoPedido",
            BindingFlags.Public | BindingFlags.Instance);
        Assert.Null(method);
    }

    [Fact]
    public void LogisticaHub_DoesNotHave_NotificarRepartidorAsignado()
    {
        var method = typeof(LogisticaHub).GetMethod("NotificarRepartidorAsignado",
            BindingFlags.Public | BindingFlags.Instance);
        Assert.Null(method);
    }

    [Fact]
    public void LogisticaHub_DoesNotHave_NotificarDemora()
    {
        var method = typeof(LogisticaHub).GetMethod("NotificarDemora",
            BindingFlags.Public | BindingFlags.Instance);
        Assert.Null(method);
    }

    // ================================================================
    // Task 5.2: EnviarPosicionGPS sends PosicionGPSMessage typed DTO
    // ================================================================

    [Fact]
    public async Task EnviarPosicionGPS_SendsPosicionGPSMessage_TypedDTO()
    {
        // Arrange
        var (hub, mockProxy) = CreateHubWithProxy("Repartidor");

        object?[]? capturedArgs = null;
        mockProxy
            .Setup(proxy => proxy.SendCoreAsync("PosicionGPSActualizada", It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Callback<string, object?[], CancellationToken>((_, args, _) => capturedArgs = args)
            .Returns(Task.CompletedTask);

        // Act: invoke GPS position
        await hub.EnviarPosicionGPS(5, -34.6037, -58.3816);

        // Assert: PosicionGPSMessage typed DTO was sent
        Assert.NotNull(capturedArgs);
        var msg = Assert.IsType<PosicionGPSMessage>(capturedArgs![0]);
        Assert.Equal(5, msg.RepartidorId);
        Assert.Equal(-34.6037, msg.Latitud);
        Assert.Equal(-58.3816, msg.Longitud);
    }
}
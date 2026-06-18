using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Moq;

namespace ApiGastronomia.Tests.Services;

public class PedidoServiceTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static AppDbContext CreateDbContext(string? dbName = null)
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(dbName ?? $"PedidoServiceTestDb_{Guid.NewGuid()}")
            .Options;

        var context = new AppDbContext(options);
        context.Database.EnsureCreated();
        return context;
    }

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

    private static (PedidoService Service, Mock<IClientProxy> MockProxy) CreateService(AppDbContext context)
    {
        var (mockClients, mockProxy) = CreateMockHubClients();
        var mockHub = new Mock<IHubContext<LogisticaHub>>();
        mockHub.Setup(h => h.Clients).Returns(mockClients.Object);

        var logger = new LoggerFactory().CreateLogger<PedidoService>();

        var service = new PedidoService(context, mockHub.Object, logger);
        return (service, mockProxy);
    }

    /// <summary>
    /// Seeds the required reference data (EstadoPedido entries) into the context.
    /// Returns the context for chaining.
    /// </summary>
    private static AppDbContext SeedEstados(AppDbContext context)
    {
        context.EstadosPedidos.AddRange(
            new EstadoPedido { Id = 1, Nombre = "Pendiente" },
            new EstadoPedido { Id = 2, Nombre = "EnPreparacion" },
            new EstadoPedido { Id = 3, Nombre = "ListoParaRetirar" },
            new EstadoPedido { Id = 4, Nombre = "EnCamino" },
            new EstadoPedido { Id = 5, Nombre = "Entregado" },
            new EstadoPedido { Id = 6, Nombre = "Retirado" },
            new EstadoPedido { Id = 7, Nombre = "Cancelado" },
            new EstadoPedido { Id = 8, Nombre = "Devuelto" }
        );
        context.SaveChanges();
        return context;
    }

    /// <summary>
    /// Seeds a Pedido in a given state and returns (context, pedido).
    /// </summary>
    private static (AppDbContext Context, Pedido Pedido) SeedPedido(
        AppDbContext context,
        EstadoPedidoEnum estado,
        int id = 0)
    {
        var estadoEntity = context.EstadosPedidos.First(e => e.Id == (int)estado);

        var pedido = new Pedido
        {
            EstadoId = (int)estado,
            Estado = estadoEntity,
            MetodoPagoId = 1,
            MetodoPago = new MetodoPago { Nombre = "Efectivo" },
            MetodoVentaId = 1,
            MetodoVenta = new MetodoVenta { Nombre = "Local" },
            TotalEstimado = 5000.0,
            ClienteNombre = "Test Client",
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = 1, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };

        context.Pedidos.Add(pedido);
        context.SaveChanges();
        return (context, pedido);
    }

    /// <summary>
    /// Seeds a basic set of FK data needed for CrearPedidoAsync tests.
    /// Returns (context, metodoPago, metodoVenta, producto).
    /// </summary>
    private static (AppDbContext Context, MetodoPago MetodoPago, MetodoVenta MetodoVenta, Producto Producto) SeedFkData(
        AppDbContext context)
    {
        var metodoPago = new MetodoPago { Nombre = "Efectivo" };
        var metodoVenta = new MetodoVenta { Nombre = "Delivery" };
        var producto = new Producto { Nombre = "Pizza Napolitana", Precio = 5500.0, Demora = 20 };

        context.MetodoPago.Add(metodoPago);
        context.MetodosVenta.Add(metodoVenta);
        context.Productos.Add(producto);
        context.SaveChanges();

        return (context, metodoPago, metodoVenta, producto);
    }

    /// <summary>
    /// Seeds a Usuario with a given role and availability.
    /// Returns (context, usuario).
    /// </summary>
    private static (AppDbContext Context, Usuario Usuario) SeedUsuario(
        AppDbContext context,
        string rolNombre = "Repartidor",
        bool disponible = true,
        bool activo = true)
    {
        var rol = context.Roles.FirstOrDefault(r => r.Nombre == rolNombre);
        if (rol == null)
        {
            rol = new Rol { Nombre = rolNombre };
            context.Roles.Add(rol);
            context.SaveChanges();
        }

        var usuario = new Usuario
        {
            UsuarioNombre = $"Test_{rolNombre}_{Guid.NewGuid():N}",
            PasswordHash = "hash",
            Disponible = disponible,
            Activo = activo,
            RolId = rol.Id,
            Rol = rol
        };

        context.Usuarios.Add(usuario);
        context.SaveChanges();

        return (context, usuario);
    }

    // ================================================================
    // Task 2.1 — CambiarEstadoAsync: Devuelto terminal state fix
    // ================================================================

    [Fact]
    public async Task CambiarEstadoAsync_DevueltoIsTerminal_ThrowsInvalidOperationException()
    {
        // Arrange: seed a pedido in Devuelto state
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.Devuelto);
        var (service, _) = CreateService(context);

        // Act + Assert: attempting to change from Devuelto should throw
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.Pendiente));
        Assert.Contains("No se puede cambiar el estado", ex.Message);
        Assert.Contains("Devuelto", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CambiarEstadoAsync_EntregadoIsTerminal_ThrowsInvalidOperationException()
    {
        // Arrange: seed a pedido in Entregado state
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.Entregado);
        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.EnCamino));
        Assert.Contains("No se puede cambiar el estado", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CambiarEstadoAsync_RetiradoIsTerminal_ThrowsInvalidOperationException()
    {
        // Arrange: seed a pedido in Retirado state
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.Retirado);
        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.Pendiente));
        Assert.Contains("No se puede cambiar el estado", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CambiarEstadoAsync_CanceladoIsTerminal_ThrowsInvalidOperationException()
    {
        // Arrange: seed a pedido in Cancelado state
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.Cancelado);
        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.Pendiente));
        Assert.Contains("No se puede cambiar el estado", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CambiarEstadoAsync_TransitionToDevuelto_SetsFechaFinalizado()
    {
        // Arrange: seed a pedido in EnCamino state, transition to Devuelto
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.EnCamino);
        var (service, _) = CreateService(context);

        // Act: transition to Devuelto
        var result = await service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.Devuelto);

        // Assert: FechaFinalizado must be set
        Assert.Equal((int)EstadoPedidoEnum.Devuelto, result.EstadoId);
        Assert.NotNull(result.FechaFinalizado);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CambiarEstadoAsync_TransitionToCancelado_SetsFechaFinalizado()
    {
        // Arrange: triangulation — Cancelado also sets FechaFinalizado
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, pedido) = SeedPedido(context, EstadoPedidoEnum.Pendiente);
        var (service, _) = CreateService(context);

        // Act: transition to Cancelado
        var result = await service.CambiarEstadoAsync(pedido.Id, EstadoPedidoEnum.Cancelado);

        // Assert
        Assert.Equal((int)EstadoPedidoEnum.Cancelado, result.EstadoId);
        Assert.NotNull(result.FechaFinalizado);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Task 2.2 — CrearPedidoAsync: FK validation
    // ================================================================

    [Fact]
    public async Task CrearPedidoAsync_InvalidCajaId_ThrowsInvalidOperationException()
    {
        // Arrange: seed FK data but use a non-existent CajaId
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            CajaId = 999, // non-existent
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("Caja", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_NullCajaId_IsAllowed()
    {
        // Arrange: CajaId is nullable, so null should work
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            CajaId = null, // null is okay
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };

        // Act
        var result = await service.CrearPedidoAsync(pedido);

        // Assert: pedido created with null CajaId
        Assert.NotNull(result);
        Assert.Null(result.CajaId);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_InvalidMetodoPagoId_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = 999, // non-existent
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("Método de pago", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_InvalidMetodoVentaId_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = 999, // non-existent
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("Método de venta", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_InvalidProductoId_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = 999, Nombre = "Ghost", Precio = 100.0, Cantidad = 1 } // non-existent
            }
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("Producto", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_AllValidFks_CreatesPedido()
    {
        // Arrange: all FK references are valid
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = metodoPago.Id,
            MetodoVentaId = metodoVenta.Id,
            TotalEstimado = 5500.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza Napolitana", Precio = 5500.0, Cantidad = 1 }
            }
        };

        // Act
        var result = await service.CrearPedidoAsync(pedido);

        // Assert: pedido created with correct defaults
        Assert.NotNull(result);
        Assert.Equal((int)EstadoPedidoEnum.Pendiente, result.EstadoId);
        Assert.NotEqual(default, result.FechaIngreso);
        Assert.True(result.Id > 0);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Task 2.3 — CrearPedidoAsync: empty DetallePedidos validation
    // ================================================================

    [Fact]
    public async Task CrearPedidoAsync_NullDetalles_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        SeedEstados(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = 1,
            MetodoVentaId = 1,
            TotalEstimado = 0,
            DetallePedidos = null!
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("al menos un producto", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task CrearPedidoAsync_EmptyDetalles_ThrowsInvalidOperationException()
    {
        // Arrange
        var context = CreateDbContext();
        SeedEstados(context);
        var (service, _) = CreateService(context);

        var pedido = new Pedido
        {
            MetodoPagoId = 1,
            MetodoVentaId = 1,
            TotalEstimado = 0,
            DetallePedidos = new List<DetallePedido>()
        };

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.CrearPedidoAsync(pedido));
        Assert.Contains("al menos un producto", ex.Message);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    // ================================================================
    // Task 2.4 — AsignarRepartidorAsync: role/disponibilidad/activo validation
    // ================================================================

    [Fact]
    public async Task AsignarRepartidorAsync_ValidRepartidor_AssignsSuccessfully()
    {
        // Arrange: seed a pedido and a valid repartidor
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (_, repartidor) = SeedUsuario(context, rolNombre: "Repartidor", disponible: true, activo: true);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act
        var result = await service.AsignarRepartidorAsync(pedido.Id, repartidor.Id);

        // Assert
        Assert.Equal(repartidor.Id, result.RepartidorId);
        Assert.NotNull(result.FechaAsignado);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AsignarRepartidorAsync_NonRepartidorRole_ThrowsInvalidOperationException()
    {
        // Arrange: seed a user with role "Cocinero" (not Repartidor)
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (_, noRepartidor) = SeedUsuario(context, rolNombre: "Cocinero", disponible: true, activo: true);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.AsignarRepartidorAsync(pedido.Id, noRepartidor.Id));
        Assert.Contains("no tiene rol de repartidor", ex.Message.ToLower());

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AsignarRepartidorAsync_UnavailableRepartidor_ThrowsInvalidOperationException()
    {
        // Arrange: seed a repartidor who is not available
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (_, repartidor) = SeedUsuario(context, rolNombre: "Repartidor", disponible: false, activo: true);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.AsignarRepartidorAsync(pedido.Id, repartidor.Id));
        Assert.Contains("no está disponible", ex.Message.ToLower());

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AsignarRepartidorAsync_NonExistentUser_ThrowsKeyNotFoundException()
    {
        // Arrange: use a non-existent user ID
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act + Assert: non-existent user throws KeyNotFoundException (existing behavior)
        await Assert.ThrowsAsync<KeyNotFoundException>(
            () => service.AsignarRepartidorAsync(pedido.Id, 9999));

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AsignarRepartidorAsync_ReassignReplacesPreviousAssignment()
    {
        // Arrange: seed a valid repartidor, assign them, then reassign to another
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (_, repartidor1) = SeedUsuario(context, rolNombre: "Repartidor", disponible: true, activo: true);
        var (_, repartidor2) = SeedUsuario(context, rolNombre: "Repartidor", disponible: true, activo: true);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act: assign first repartidor
        var result1 = await service.AsignarRepartidorAsync(pedido.Id, repartidor1.Id);
        Assert.Equal(repartidor1.Id, result1.RepartidorId);

        // Act: reassign to second repartidor
        var result2 = await service.AsignarRepartidorAsync(pedido.Id, repartidor2.Id);
        Assert.Equal(repartidor2.Id, result2.RepartidorId);

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }

    [Fact]
    public async Task AsignarRepartidorAsync_InactiveUser_ThrowsInvalidOperationException()
    {
        // Arrange: seed a repartidor who is not active
        var context = CreateDbContext();
        SeedEstados(context);
        var (_, metodoPago, metodoVenta, producto) = SeedFkData(context);
        var (_, repartidor) = SeedUsuario(context, rolNombre: "Repartidor", disponible: true, activo: false);

        var pedido = new Pedido
        {
            EstadoId = (int)EstadoPedidoEnum.Pendiente,
            Estado = context.EstadosPedidos.First(e => e.Id == (int)EstadoPedidoEnum.Pendiente),
            MetodoPagoId = metodoPago.Id,
            MetodoPago = metodoPago,
            MetodoVentaId = metodoVenta.Id,
            MetodoVenta = metodoVenta,
            TotalEstimado = 5000.0,
            DetallePedidos = new List<DetallePedido>
            {
                new() { ProductoId = producto.Id, Nombre = "Pizza", Precio = 5000.0, Cantidad = 1 }
            }
        };
        context.Pedidos.Add(pedido);
        context.SaveChanges();

        var (service, _) = CreateService(context);

        // Act + Assert
        var ex = await Assert.ThrowsAsync<InvalidOperationException>(
            () => service.AsignarRepartidorAsync(pedido.Id, repartidor.Id));
        Assert.Contains("no está activo", ex.Message.ToLower());

        // Cleanup
        await context.Database.EnsureDeletedAsync();
        context.Dispose();
    }
}
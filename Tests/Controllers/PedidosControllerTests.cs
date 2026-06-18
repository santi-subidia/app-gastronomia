using ApiGastronomia.Controllers;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class PedidosControllerTests
{
    // ================================================================
    // Helpers — build Pedido entities with loaded navigations
    // ================================================================

    private static Pedido CreatePedido(
        int id = 1,
        string clienteNombre = "Juan Perez",
        string? clienteDireccion = "Calle 123",
        double totalEstimado = 17000.0,
        int? demoraAprox = 45,
        double? latitudDestino = -34.6037,
        double? longitudDestino = -58.3816,
        string estadoNombre = "EnPreparacion",
        int estadoId = 2,
        string? metodoVentaNombre = "Delivery",
        string? metodoPagoNombre = "Efectivo",
        string? repartidorNombre = null,
        int? cajaId = null,
        DateTime? fechaIngreso = null,
        DateTime? fechaEstimadoFin = null,
        DateTime? fechaAsignado = null,
        DateTime? fechaEnCamino = null,
        DateTime? fechaFinalizado = null,
        List<DetallePedido>? detalles = null)
    {
        var estado = new EstadoPedido { Id = estadoId, Nombre = estadoNombre };
        var metodoVenta = metodoVentaNombre is not null
            ? new MetodoVenta { Id = 1, Nombre = metodoVentaNombre }
            : null;
        var metodoPago = metodoPagoNombre is not null
            ? new MetodoPago { Id = 1, Nombre = metodoPagoNombre }
            : null;
        var repartidor = repartidorNombre is not null
            ? new Usuario { Id = 10, UsuarioNombre = repartidorNombre }
            : null;
        var caja = cajaId is not null
            ? new Caja { Id = cajaId.Value }
            : null;

        var pedido = new Pedido
        {
            Id = id,
            ClienteNombre = clienteNombre,
            ClienteDireccion = clienteDireccion,
            TotalEstimado = totalEstimado,
            DemoraAprox = demoraAprox,
            LatitudDestino = latitudDestino,
            LongitudDestino = longitudDestino,
            EstadoId = estadoId,
            Estado = estado,
            MetodoVentaId = metodoVenta is not null ? 1 : default,
            MetodoVenta = metodoPagoNombre is not null || metodoVentaNombre is not null
                ? metodoVenta!
                : null,
            MetodoPagoId = metodoPago is not null ? 1 : default,
            MetodoPago = metodoPago,
            RepartidorId = repartidor is not null ? 10 : null,
            Repartidor = repartidor,
            CajaId = cajaId,
            Caja = caja,
            FechaIngreso = fechaIngreso ?? DateTime.UtcNow,
            FechaEstimadoFin = fechaEstimadoFin,
            FechaAsignado = fechaAsignado,
            FechaEnCamino = fechaEnCamino,
            FechaFinalizado = fechaFinalizado,
            DetallePedidos = detalles ?? new List<DetallePedido>()
        };

        // Wire up back-references for detalles
        foreach (var d in pedido.DetallePedidos)
        {
            d.PedidoId = pedido.Id;
            d.Pedido = pedido;
        }

        return pedido;
    }

    private static DetallePedido CreateDetalle(
        int productoId = 1,
        string nombre = "Pizza",
        int cantidad = 2,
        double precio = 5500.0,
        int productoDemora = 20)
    {
        var producto = new Producto { Id = productoId, Nombre = nombre, Precio = precio, Demora = productoDemora };
        return new DetallePedido
        {
            PedidoId = 1,
            ProductoId = productoId,
            Nombre = nombre,
            Cantidad = cantidad,
            Precio = precio,
            Producto = producto
        };
    }

    /// <summary>
    /// Creates the controller. No authenticated context needed for GET mapping tests
    /// since we only test the mapping output, not auth policies.
    /// </summary>
    private static PedidosController CreateController(IPedidoService service)
    {
        var logger = new Mock<ILogger<PedidosController>>();
        return new PedidosController(service, logger.Object);
    }

    // ================================================================
    // GET /api/pedidos — returns PedidoResumenDTO list
    // ================================================================

    [Fact]
    public async Task GetPedidos_ReturnsOkWithResumenDTOs()
    {
        // Arrange: 3 pedidos with loaded navigations
        var pedidos = new List<Pedido>
        {
            CreatePedido(id: 1, clienteNombre: "Ana", estadoNombre: "Pendiente",
                metodoVentaNombre: "Local", totalEstimado: 5000.0),
            CreatePedido(id: 2, clienteNombre: "Carlos", estadoNombre: "EnPreparacion",
                metodoVentaNombre: "Delivery", totalEstimado: 12000.0),
            CreatePedido(id: 3, clienteNombre: "Maria", estadoNombre: "Entregado",
                metodoVentaNombre: "Local", totalEstimado: 8000.0)
        };

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidosAsync())
            .ReturnsAsync(pedidos);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedidos();

        // Assert: 200 OK with list of PedidoResumenDTO
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dtos = Assert.IsAssignableFrom<IEnumerable<PedidoResumenDTO>>(okResult.Value!);
        var list = dtos.ToList();
        Assert.Equal(3, list.Count);

        // Verify DTO field mapping — summarised data only
        var first = list.First(d => d.Id == 1);
        Assert.Equal("Pendiente", first.Estado);
        Assert.Equal("Ana", first.ClienteNombre);
        Assert.Equal("Local", first.MetodoVenta);
        Assert.Equal(5000.0, first.TotalEstimado);
    }

    [Fact]
    public async Task GetPedidos_EmptyList_ReturnsOkWithEmptyDTOs()
    {
        // Triangulation: empty list returns 200 with empty DTO list
        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidosAsync())
            .ReturnsAsync([]);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedidos();

        // Assert: 200 OK with empty collection
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dtos = Assert.IsAssignableFrom<IEnumerable<PedidoResumenDTO>>(okResult.Value!);
        Assert.Empty(dtos);
    }

    // ================================================================
    // GET /api/pedidos/{id} — returns PedidoDetalleDTO
    // ================================================================

    [Fact]
    public async Task GetPedido_ExistingId_ReturnsOkWithDetalleDTO()
    {
        // Arrange: pedido with all navigations loaded
        var fechaIngreso = new DateTime(2026, 6, 18, 12, 0, 0, DateTimeKind.Utc);
        var fechaAsignado = new DateTime(2026, 6, 18, 12, 5, 0, DateTimeKind.Utc);
        var fechaEnCamino = new DateTime(2026, 6, 18, 12, 20, 0, DateTimeKind.Utc);
        var detalle1 = CreateDetalle(productoId: 1, nombre: "Pizza", cantidad: 2, precio: 5500.0, productoDemora: 20);
        var detalle2 = CreateDetalle(productoId: 3, nombre: "Empanada", cantidad: 4, precio: 1500.0, productoDemora: 10);

        var pedido = CreatePedido(
            id: 5,
            clienteNombre: "Juan Perez",
            clienteDireccion: "Calle 123",
            totalEstimado: 17000.0,
            demoraAprox: 45,
            latitudDestino: -34.6037,
            longitudDestino: -58.3816,
            estadoNombre: "EnCamino",
            estadoId: 4,
            metodoVentaNombre: "Delivery",
            metodoPagoNombre: "Efectivo",
            repartidorNombre: "Carlos",
            cajaId: 3,
            fechaIngreso: fechaIngreso,
            fechaAsignado: fechaAsignado,
            fechaEnCamino: fechaEnCamino,
            detalles: new List<DetallePedido> { detalle1, detalle2 }
        );

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidoPorIdAsync(5))
            .ReturnsAsync(pedido);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedido(5);

        // Assert: 200 OK with PedidoDetalleDTO
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<PedidoDetalleDTO>(okResult.Value!);

        Assert.Equal(5, dto.Id);
        Assert.Equal("EnCamino", dto.Estado);
        Assert.Equal("Juan Perez", dto.ClienteNombre);
        Assert.Equal("Calle 123", dto.ClienteDireccion);
        Assert.Equal("Delivery", dto.MetodoVenta);
        Assert.Equal("Efectivo", dto.MetodoPago);
        Assert.Equal(17000.0, dto.TotalEstimado);
        Assert.Equal(45, dto.DemoraAprox);
        Assert.Equal(-34.6037, dto.LatitudDestino);
        Assert.Equal(-58.3816, dto.LongitudDestino);
        Assert.Equal(fechaIngreso, dto.FechaIngreso);
        Assert.Equal(fechaAsignado, dto.FechaAsignado);
        Assert.Equal(fechaEnCamino, dto.FechaEnCamino);
        Assert.Equal("Carlos", dto.RepartidorNombre);
        Assert.Equal(3, dto.CajaId);
        Assert.Equal(4, dto.EstadoId);

        // DetallePedidos mapped with TiempoMaquina from Producto.Demora
        Assert.Equal(2, dto.DetallePedidos.Count);
        var firstDetail = dto.DetallePedidos.First(d => d.ProductoId == 1);
        Assert.Equal("Pizza", firstDetail.Nombre);
        Assert.Equal(2, firstDetail.Cantidad);
        Assert.Equal(5500.0, firstDetail.Precio);
        Assert.Equal(20, firstDetail.TiempoMaquina); // derived from Producto.Demora

        var secondDetail = dto.DetallePedidos.First(d => d.ProductoId == 3);
        Assert.Equal(10, secondDetail.TiempoMaquina); // derived from Producto.Demora
    }

    [Fact]
    public async Task GetPedido_NonexistentId_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidoPorIdAsync(999))
            .ReturnsAsync((Pedido?)null);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedido(999);

        // Assert: 404 NotFound with message
        var notFoundResult = Assert.IsType<NotFoundObjectResult>(result.Result);
        var value = notFoundResult.Value!;
        var mensajeProp = value.GetType().GetProperty("Mensaje");
        Assert.NotNull(mensajeProp);
        Assert.Equal("Pedido #999 no encontrado.", mensajeProp!.GetValue(value));
    }

    [Fact]
    public async Task GetPedido_EmptyDetallePedidos_ReturnsEmptyList()
    {
        // Triangulation: pedido with no details still has empty list (not null)
        var pedido = CreatePedido(
            id: 10,
            estadoNombre: "Pendiente",
            metodoVentaNombre: "Local",
            metodoPagoNombre: "Efectivo",
            detalles: new List<DetallePedido>()
        );

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidoPorIdAsync(10))
            .ReturnsAsync(pedido);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedido(10);

        // Assert: DetallePedidos is empty array, not null
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<PedidoDetalleDTO>(okResult.Value!);
        Assert.NotNull(dto.DetallePedidos);
        Assert.Empty(dto.DetallePedidos);
    }

    [Fact]
    public async Task GetPedido_NullNavigations_ReturnsDTOWithNulls()
    {
        // Triangulation: pedido without MetodoPago, Repartidor, etc. — nulls mapped safely
        var pedido = CreatePedido(
            id: 1,
            estadoNombre: "Pendiente",
            metodoVentaNombre: null,    // will cause method to be null
            metodoPagoNombre: null,
            repartidorNombre: null,
            cajaId: null,
            detalles: new List<DetallePedido>()
        );
        // Override MetodoVenta to null since the helper sets it
        pedido.MetodoVenta = null!;
        pedido.MetodoVentaId = default;
        pedido.MetodoPago = null!;
        pedido.MetodoPagoId = default;

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidoPorIdAsync(1))
            .ReturnsAsync(pedido);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedido(1);

        // Assert: null navigations map to null strings safely
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<PedidoDetalleDTO>(okResult.Value!);
        Assert.Null(dto.MetodoVenta);
        Assert.Null(dto.MetodoPago);
        Assert.Null(dto.RepartidorNombre);
        Assert.Null(dto.CajaId);
    }

    // ================================================================
    // GET /api/pedidos/estado/{estado} — returns PedidoResumenDTO list
    // ================================================================

    [Fact]
    public async Task GetPedidosPorEstado_ReturnsOkWithFilteredResumenDTOs()
    {
        // Arrange: 2 pedidos in Pendiente state
        var pedidos = new List<Pedido>
        {
            CreatePedido(id: 1, clienteNombre: "Ana", estadoNombre: "Pendiente",
                metodoVentaNombre: "Local", totalEstimado: 5000.0),
            CreatePedido(id: 3, clienteNombre: "Pedro", estadoNombre: "Pendiente",
                metodoVentaNombre: "Delivery", totalEstimado: 9000.0)
        };

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidosPorEstadoAsync(EstadoPedidoEnum.Pendiente))
            .ReturnsAsync(pedidos);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedidosPorEstado(EstadoPedidoEnum.Pendiente);

        // Assert: 200 OK with filtered DTOs
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dtos = Assert.IsAssignableFrom<IEnumerable<PedidoResumenDTO>>(okResult.Value!);
        var list = dtos.ToList();
        Assert.Equal(2, list.Count);
        Assert.All(list, d => Assert.Equal("Pendiente", d.Estado));
    }

    // ================================================================
    // PATCH /api/pedidos/{id}/repartidor — InvalidOperationException → BadRequest
    // ================================================================

    [Fact]
    public async Task AsignarRepartidor_InvalidOperation_ReturnsBadRequest()
    {
        // Arrange: service throws InvalidOperationException (e.g., non-repartidor role)
        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.AsignarRepartidorAsync(1, 5))
            .ThrowsAsync(new InvalidOperationException("El usuario no tiene el rol Repartidor."));

        var controller = CreateController(mockService.Object);
        var request = new AsignarRepartidorRequest(RepartidorId: 5);

        // Act
        var result = await controller.AsignarRepartidor(1, request);

        // Assert: 400 BadRequest with message
        var badRequestResult = Assert.IsType<BadRequestObjectResult>(result.Result);
        var value = badRequestResult.Value!;
        var mensajeProp = value.GetType().GetProperty("Mensaje");
        Assert.NotNull(mensajeProp);
        Assert.Equal("El usuario no tiene el rol Repartidor.", mensajeProp!.GetValue(value));
    }

    // ================================================================
    // Triangulation: different data for GetPedido detail mapping
    // ================================================================

    [Fact]
    public async Task GetPedido_WithDifferentData_MapsAllFields()
    {
        // Triangulation: different pedido data exercises different code paths
        var fechaIngreso = new DateTime(2026, 6, 17, 10, 0, 0, DateTimeKind.Utc);
        var fechaFinalizado = new DateTime(2026, 6, 17, 10, 50, 0, DateTimeKind.Utc);
        var detalle = CreateDetalle(productoId: 7, nombre: "Hamburguesa", cantidad: 1, precio: 8000.0, productoDemora: 30);

        var pedido = CreatePedido(
            id: 42,
            clienteNombre: "Maria Lopez",
            clienteDireccion: "Av. Siempre Viva 742",
            totalEstimado: 8000.0,
            demoraAprox: 30,
            estadoNombre: "Entregado",
            estadoId: 5,
            metodoVentaNombre: "Local",
            metodoPagoNombre: "MercadoPago",
            repartidorNombre: "Ramiro",
            cajaId: 7,
            fechaIngreso: fechaIngreso,
            fechaFinalizado: fechaFinalizado,
            detalles: new List<DetallePedido> { detalle }
        );

        var mockService = new Mock<IPedidoService>();
        mockService
            .Setup(s => s.ObtenerPedidoPorIdAsync(42))
            .ReturnsAsync(pedido);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetPedido(42);

        // Assert: all fields mapped correctly
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<PedidoDetalleDTO>(okResult.Value!);

        Assert.Equal(42, dto.Id);
        Assert.Equal("Entregado", dto.Estado);
        Assert.Equal("Maria Lopez", dto.ClienteNombre);
        Assert.Equal("Av. Siempre Viva 742", dto.ClienteDireccion);
        Assert.Equal("Local", dto.MetodoVenta);
        Assert.Equal("MercadoPago", dto.MetodoPago);
        Assert.Equal("Ramiro", dto.RepartidorNombre);
        Assert.Equal(7, dto.CajaId);
        Assert.Equal(5, dto.EstadoId);
        Assert.Single(dto.DetallePedidos);
        Assert.Equal(30, dto.DetallePedidos[0].TiempoMaquina);
    }
}
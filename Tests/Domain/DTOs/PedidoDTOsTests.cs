using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Tests.Domain.DTOs;

public class PedidoDTOsTests
{
    // ================================================================
    // PedidoDetalleDTO — construction and property storage (full fields)
    // ================================================================

    [Fact]
    public void PedidoDetalleDTO_Stores_All_Properties()
    {
        // Arrange
        var fechaIngreso = DateTime.UtcNow;
        var fechaEstimadoFin = fechaIngreso.AddMinutes(45);
        var fechaAsignado = fechaIngreso.AddMinutes(5);
        var fechaEnCamino = fechaIngreso.AddMinutes(20);
        var fechaFinalizado = fechaIngreso.AddMinutes(50);
        var detalles = new List<DetallePedidoDTO>
        {
            new(ProductoId: 1, Nombre: "Pizza", Cantidad: 2, Precio: 5500.0, TiempoMaquina: 20),
            new(ProductoId: 3, Nombre: "Empanada", Cantidad: 4, Precio: 1500.0, TiempoMaquina: 10)
        };

        // Act
        var dto = new PedidoDetalleDTO(
            Id: 5,
            Estado: "EnPreparacion",
            ClienteNombre: "Juan Perez",
            ClienteDireccion: "Calle 123",
            MetodoVenta: "Delivery",
            MetodoPago: "Efectivo",
            TotalEstimado: 17000.0,
            DemoraAprox: 45,
            LatitudDestino: -34.6037,
            LongitudDestino: -58.3816,
            FechaIngreso: fechaIngreso,
            FechaEstimadoFin: fechaEstimadoFin,
            FechaAsignado: fechaAsignado,
            FechaEnCamino: fechaEnCamino,
            FechaFinalizado: fechaFinalizado,
            RepartidorNombre: "Carlos",
            CajaId: 3,
            EstadoId: 2,
            DetallePedidos: detalles
        );

        // Assert: every property carries the value passed to the constructor
        Assert.Equal(5, dto.Id);
        Assert.Equal("EnPreparacion", dto.Estado);
        Assert.Equal("Juan Perez", dto.ClienteNombre);
        Assert.Equal("Calle 123", dto.ClienteDireccion);
        Assert.Equal("Delivery", dto.MetodoVenta);
        Assert.Equal("Efectivo", dto.MetodoPago);
        Assert.Equal(17000.0, dto.TotalEstimado);
        Assert.Equal(45, dto.DemoraAprox);
        Assert.Equal(-34.6037, dto.LatitudDestino);
        Assert.Equal(-58.3816, dto.LongitudDestino);
        Assert.Equal(fechaIngreso, dto.FechaIngreso);
        Assert.Equal(fechaEstimadoFin, dto.FechaEstimadoFin);
        Assert.Equal(fechaAsignado, dto.FechaAsignado);
        Assert.Equal(fechaEnCamino, dto.FechaEnCamino);
        Assert.Equal(fechaFinalizado, dto.FechaFinalizado);
        Assert.Equal("Carlos", dto.RepartidorNombre);
        Assert.Equal(3, dto.CajaId);
        Assert.Equal(2, dto.EstadoId);
        Assert.Equal(2, dto.DetallePedidos.Count);
        Assert.Equal("Pizza", dto.DetallePedidos[0].Nombre);
        Assert.Equal("Empanada", dto.DetallePedidos[1].Nombre);
    }

    [Fact]
    public void PedidoDetalleDTO_With_Different_Values()
    {
        // Triangulation: different inputs produce different outputs
        var detalles = new List<DetallePedidoDTO>
        {
            new(ProductoId: 10, Nombre: "Hamburguesa", Cantidad: 1, Precio: 8000.0, TiempoMaquina: 30)
        };

        var dto = new PedidoDetalleDTO(
            Id: 42,
            Estado: "Pendiente",
            ClienteNombre: "Maria Lopez",
            ClienteDireccion: "Av. Siempre Viva 742",
            MetodoVenta: "Local",
            MetodoPago: "MercadoPago",
            TotalEstimado: 8000.0,
            DemoraAprox: 30,
            LatitudDestino: null,
            LongitudDestino: null,
            FechaIngreso: DateTime.UtcNow.AddDays(-1),
            FechaEstimadoFin: null,
            FechaAsignado: null,
            FechaEnCamino: null,
            FechaFinalizado: null,
            RepartidorNombre: null,
            CajaId: null,
            EstadoId: 1,
            DetallePedidos: detalles
        );

        Assert.Equal(42, dto.Id);
        Assert.Equal("Pendiente", dto.Estado);
        Assert.Equal("Maria Lopez", dto.ClienteNombre);
        Assert.Equal("Local", dto.MetodoVenta);
        Assert.Equal("MercadoPago", dto.MetodoPago);
        Assert.Null(dto.LatitudDestino);
        Assert.Null(dto.LongitudDestino);
        Assert.Null(dto.FechaEstimadoFin);
        Assert.Null(dto.FechaAsignado);
        Assert.Null(dto.FechaEnCamino);
        Assert.Null(dto.FechaFinalizado);
        Assert.Null(dto.RepartidorNombre);
        Assert.Null(dto.CajaId);
        Assert.Equal(1, dto.EstadoId);
        Assert.Single(dto.DetallePedidos);
    }

    // ================================================================
    // PedidoDetalleDTO — nullable fields (pedido without repartidor/fechas)
    // ================================================================

    [Fact]
    public void PedidoDetalleDTO_PendingPedido_NullOptionalFields()
    {
        // A newly created pedido (Pendiente) has null repartidor and dates
        var dto = new PedidoDetalleDTO(
            Id: 1,
            Estado: "Pendiente",
            ClienteNombre: "Ana",
            ClienteDireccion: null,
            MetodoVenta: "Local",
            MetodoPago: "Efectivo",
            TotalEstimado: 5000.0,
            DemoraAprox: null,
            LatitudDestino: null,
            LongitudDestino: null,
            FechaIngreso: DateTime.UtcNow,
            FechaEstimadoFin: null,
            FechaAsignado: null,
            FechaEnCamino: null,
            FechaFinalizado: null,
            RepartidorNombre: null,
            CajaId: null,
            EstadoId: 1,
            DetallePedidos: new List<DetallePedidoDTO>()
        );

        Assert.Null(dto.ClienteDireccion);
        Assert.Null(dto.DemoraAprox);
        Assert.Null(dto.FechaEstimadoFin);
        Assert.Null(dto.RepartidorNombre);
        Assert.Null(dto.CajaId);
        Assert.Empty(dto.DetallePedidos);
    }

    // ================================================================
    // PedidoDetalleDTO — record equality semantics
    // ================================================================

    [Fact]
    public void PedidoDetalleDTO_Equality_By_Value()
    {
        // Records have structural equality
        var details = new List<DetallePedidoDTO>
        {
            new(ProductoId: 1, Nombre: "Pizza", Cantidad: 1, Precio: 5500.0, TiempoMaquina: 20)
        };
        var fechaIngreso = DateTime.UtcNow;

        var dto1 = new PedidoDetalleDTO(1, "Pendiente", "Ana", null, "Local", "Efectivo", 5500.0, null, null, null, fechaIngreso, null, null, null, null, null, null, 1, details);
        var dto2 = new PedidoDetalleDTO(1, "Pendiente", "Ana", null, "Local", "Efectivo", 5500.0, null, null, null, fechaIngreso, null, null, null, null, null, null, 1, details);

        Assert.Equal(dto1, dto2);
    }

    // ================================================================
    // CrearDetalleRequest — TiempoMaquina removed (compilation test)
    // ================================================================

    [Fact]
    public void CrearDetalleRequest_Constructs_Without_TiempoMaquina()
    {
        // After removing TiempoMaquina from CrearDetalleRequest, it has 4 params
        var request = new CrearDetalleRequest(
            ProductoId: 1,
            Nombre: "Pizza",
            Precio: 5500.0,
            Cantidad: 2
        );

        Assert.Equal(1, request.ProductoId);
        Assert.Equal("Pizza", request.Nombre);
        Assert.Equal(5500.0, request.Precio);
        Assert.Equal(2, request.Cantidad);
    }

    [Fact]
    public void CrearDetalleRequest_With_Different_Values()
    {
        // Triangulation: different values
        var request = new CrearDetalleRequest(
            ProductoId: 3,
            Nombre: "Empanada",
            Precio: 1500.0,
            Cantidad: 6
        );

        Assert.Equal(3, request.ProductoId);
        Assert.Equal("Empanada", request.Nombre);
        Assert.Equal(1500.0, request.Precio);
        Assert.Equal(6, request.Cantidad);
    }

    [Fact]
    public void CrearDetalleRequest_Equality_By_Value()
    {
        var r1 = new CrearDetalleRequest(1, "Pizza", 5500.0, 2);
        var r2 = new CrearDetalleRequest(1, "Pizza", 5500.0, 2);

        Assert.Equal(r1, r2);
    }
}
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Moq;

namespace ApiGastronomia.Tests.Services;

public class EstimacionPedidoServiceTests
{
    [Fact]
    public async Task CalcularAsync_UsaLaMayorDemoraYNoMultiplicaPorCantidad()
    {
        await using var context = CreateContext();
        var pedido = new Pedido
        {
            Id = 1,
            MetodoVentaId = 2,
            FechaIngreso = new DateTime(2026, 7, 23, 12, 0, 0, DateTimeKind.Utc),
            DetallePedidos =
            [
                new DetallePedido { Cantidad = 5, Producto = new Producto { Demora = 10 } },
                new DetallePedido { Cantidad = 2, Producto = new Producto { Demora = 25 } }
            ],
            Demoras = [new Demora { DemoraMinutos = 7 }]
        };

        var service = CreateService(context, 99);

        await service.CalcularAsync(pedido);

        Assert.Equal(25, pedido.DemoraPreparacionAprox);
        Assert.Equal(7, pedido.DemoraDemorasAprox);
        Assert.Null(pedido.DemoraDeliveryAprox);
        Assert.Equal(32, pedido.DemoraAprox);
        Assert.Equal(pedido.FechaIngreso.AddMinutes(32), pedido.FechaEstimadoFin);
    }

    [Fact]
    public async Task CalcularAsync_SumaLaDuracionDeRutaSoloParaDelivery()
    {
        await using var context = CreateContext();
        context.Configuracion.Add(new Configuracion
        {
            LatitudPartida = -34.6,
            LongitudPartida = -58.4
        });
        await context.SaveChangesAsync();

        var pedido = new Pedido
        {
            Id = 2,
            MetodoVentaId = 1,
            LatitudDestino = -34.7,
            LongitudDestino = -58.5,
            FechaIngreso = DateTime.UtcNow,
            DetallePedidos = [new DetallePedido { Producto = new Producto { Demora = 20 } }]
        };
        var service = CreateService(context, 12);

        await service.CalcularAsync(pedido);

        Assert.Equal(12, pedido.DemoraDeliveryAprox);
        Assert.Equal(32, pedido.DemoraAprox);
    }

    private static EstimacionPedidoService CreateService(AppDbContext context, int routeMinutes)
    {
        var routing = new Mock<IRoutingService>();
        routing
            .Setup(service => service.ObtenerDuracionMinutosAsync(
                It.IsAny<double>(), It.IsAny<double>(), It.IsAny<double>(), It.IsAny<double>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(routeMinutes);

        var proxy = new Mock<IClientProxy>();
        proxy.Setup(p => p.SendCoreAsync(
                It.IsAny<string>(), It.IsAny<object?[]>(), It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);
        var clients = new Mock<IHubClients>();
        clients.Setup(c => c.Group(It.IsAny<string>())).Returns(proxy.Object);
        var hub = new Mock<IHubContext<LogisticaHub>>();
        hub.Setup(h => h.Clients).Returns(clients.Object);

        return new EstimacionPedidoService(
            context,
            routing.Object,
            hub.Object,
            new LoggerFactory().CreateLogger<EstimacionPedidoService>());
    }

    private static AppDbContext CreateContext()
    {
        var options = new DbContextOptionsBuilder<AppDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new AppDbContext(options);
    }
}

using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Tests.DTOs;

public class SignalRMessageTests
{
    // ================================================================
    // Task 1.1 — SignalR message DTOs: construction & equality
    // ================================================================

    [Fact]
    public void NuevoPedidoMessage_ConstructsAndEquals()
    {
        var msg1 = new NuevoPedidoMessage(1, "Juan", 1500.0, new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc));
        var msg2 = new NuevoPedidoMessage(1, "Juan", 1500.0, new DateTime(2026, 1, 1, 0, 0, 0, DateTimeKind.Utc));

        Assert.Equal(1, msg1.PedidoId);
        Assert.Equal("Juan", msg1.Cliente);
        Assert.Equal(1500.0, msg1.Total);
        Assert.Equal(msg1, msg2);
        Assert.Equal(msg1.GetHashCode(), msg2.GetHashCode());
    }

    [Fact]
    public void NuevoPedidoMessage_DifferentValues_AreNotEqual()
    {
        var msg1 = new NuevoPedidoMessage(1, "Juan", 1500.0, DateTime.UtcNow);
        var msg2 = new NuevoPedidoMessage(2, "Maria", 2000.0, DateTime.UtcNow);

        Assert.NotEqual(msg1, msg2);
    }

    [Fact]
    public void EstadoCambiadoMessage_ConstructsCorrectly()
    {
        var msg = new EstadoCambiadoMessage(42, "Pendiente", "EnPreparacion", DateTime.UtcNow);

        Assert.Equal(42, msg.PedidoId);
        Assert.Equal("Pendiente", msg.EstadoAnterior);
        Assert.Equal("EnPreparacion", msg.EstadoNuevo);
    }

    [Fact]
    public void PedidoActualizadoMessage_ConstructsCorrectly()
    {
        var msg = new PedidoActualizadoMessage(42, "EnPreparacion", DateTime.UtcNow);

        Assert.Equal(42, msg.PedidoId);
        Assert.Equal("EnPreparacion", msg.Estado);
    }

    [Fact]
    public void RepartidorAsignadoMessage_ConstructsCorrectly()
    {
        var msg = new RepartidorAsignadoMessage(42, 5, "Carlos", DateTime.UtcNow);

        Assert.Equal(42, msg.PedidoId);
        Assert.Equal(5, msg.RepartidorId);
        Assert.Equal("Carlos", msg.NombreRepartidor);
    }

    [Fact]
    public void DemoraRegistradaMessage_ConstructsCorrectly()
    {
        var msg = new DemoraRegistradaMessage(42, "Cocina", 15, DateTime.UtcNow);

        Assert.Equal(42, msg.PedidoId);
        Assert.Equal("Cocina", msg.Motivo);
        Assert.Equal(15, msg.TiempoEstimadoMinutos);
    }

    [Fact]
    public void PosicionGPSMessage_ConstructsCorrectly()
    {
        var msg = new PosicionGPSMessage(5, -34.6, -58.4, DateTime.UtcNow);

        Assert.Equal(5, msg.RepartidorId);
        Assert.Equal(-34.6, msg.Latitud);
        Assert.Equal(-58.4, msg.Longitud);
    }

    [Fact]
    public void PedidoFinalizadoMessage_ConstructsCorrectly()
    {
        var msg = new PedidoFinalizadoMessage(42, "Entregado", DateTime.UtcNow);

        Assert.Equal(42, msg.PedidoId);
        Assert.Equal("Entregado", msg.EstadoFinal);
    }
}
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Services.Interfaces;

public interface IPedidoService
{
    Task<Pedido> CrearPedidoAsync(Pedido pedido);
    Task<Pedido?> ObtenerPedidoPorIdAsync(int id);
    Task<IEnumerable<Pedido>> ObtenerPedidosAsync();
    Task<IEnumerable<Pedido>> ObtenerPedidosPorEstadoAsync(EstadoPedidoEnum estado);
    Task<Pedido> CambiarEstadoAsync(int pedidoId, EstadoPedidoEnum nuevoEstado);
    Task<Pedido> AsignarRepartidorAsync(int pedidoId, int repartidorId);
}

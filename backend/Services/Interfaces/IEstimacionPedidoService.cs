using ApiGastronomia.Domain.Entities;

namespace ApiGastronomia.Services.Interfaces;

public interface IEstimacionPedidoService
{
    Task CalcularAsync(Pedido pedido, bool consultarRuta = true);
    Task RecalcularAsync(int pedidoId);
}

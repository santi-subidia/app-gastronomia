using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class EstadoPedidoSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<EstadoPedidoSeedService> _logger;

    private static readonly (int Id, string Nombre)[] SeedEstados =
    [
        (1, "Pendiente"),
        (2, "En preparacion"),
        (3, "Listo para retirar"),
        (4, "En camino"),
        (5, "Entregado"),
        (6, "Retirado"),
        (7, "Cancelado"),
        (8, "Devuelto"),
        (9, "Contingencia")
    ];

    public EstadoPedidoSeedService(AppDbContext context, ILogger<EstadoPedidoSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var (id, nombre) in SeedEstados)
            {
                var existing = await _context.EstadosPedidos.FindAsync(id);
                if (existing is not null)
                {
                    if (existing.Nombre != nombre)
                        existing.Nombre = nombre;
                }
                else
                {
                    _context.EstadosPedidos.Add(new EstadoPedido { Id = id, Nombre = nombre });
                }
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar EstadoPedidoSeedService");
        }
    }
}
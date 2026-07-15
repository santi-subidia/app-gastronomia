using ApiGastronomia.Domain.Entities;
using Microsoft.EntityFrameworkCore;

namespace ApiGastronomia.Infrastructure.Data.Seeds;

public class ProductoSeedService
{
    private readonly AppDbContext _context;
    private readonly ILogger<ProductoSeedService> _logger;

    private static readonly Producto[] SeedProductos =
    [
        // ──────────────── Cocina (con demora) ────────────────
        new() { Nombre = "Milanesa con Papas Fritas", Precio = 8500, Demora = 25 },
        new() { Nombre = "Hamburguesa Completa",     Precio = 7200, Demora = 15 },
        new() { Nombre = "Pizza Muzzarella Grande",  Precio = 6500, Demora = 20 },
        new() { Nombre = "Empanadas de Carne (Docena)", Precio = 4800, Demora = 30 },
        new() { Nombre = "Pastel de Papas",          Precio = 6200, Demora = 35 },
        new() { Nombre = "Pollo al Horno con Verduras", Precio = 7800, Demora = 45 },
        new() { Nombre = "Fideos con Salsa Bolognesa", Precio = 5500, Demora = 15 },
        new() { Nombre = "Omelette de Jamón y Queso", Precio = 4200, Demora = 10 },
        new() { Nombre = "Bife de Chorizo con Ensalada", Precio = 9500, Demora = 25 },
        new() { Nombre = "Ravioles de Ricota con Salsa Mixta", Precio = 5800, Demora = 20 },
        new() { Nombre = "Lomo a la Parrilla",       Precio = 11000, Demora = 30 },
        new() { Nombre = "Canelones de Espinaca",    Precio = 6000, Demora = 35 },
        new() { Nombre = "Tarta de Verdura",         Precio = 4000, Demora = 25 },
        new() { Nombre = "Suprema de Pollo Napolitana", Precio = 8200, Demora = 25 },
        new() { Nombre = "Ojo de Bife con Puré",     Precio = 10500, Demora = 30 },

        // ──────────────── Bebidas (sin demora) ────────────────
        new() { Nombre = "Coca-Cola 500ml",          Precio = 1800, Demora = 0 },
        new() { Nombre = "Agua Mineral 500ml",       Precio = 1200, Demora = 0 },
        new() { Nombre = "Cerveza Quilmes 1L",       Precio = 3500, Demora = 0 },
        new() { Nombre = "Limonada Casera",          Precio = 2200, Demora = 0 },
        new() { Nombre = "Agua Saborizada (Pomelo)", Precio = 1500, Demora = 0 },
        new() { Nombre = "Licuado de Banana",        Precio = 2500, Demora = 0 },
        new() { Nombre = "Vino Tinto Malbec Copa",   Precio = 3000, Demora = 0 },
        new() { Nombre = "Café Espresso",            Precio = 1500, Demora = 0 },
        new() { Nombre = "Café con Leche",           Precio = 2000, Demora = 0 },
        new() { Nombre = "Gaseosa Sprite 500ml",     Precio = 1800, Demora = 0 },
    ];

    public ProductoSeedService(AppDbContext context, ILogger<ProductoSeedService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task SeedAsync()
    {
        try
        {
            foreach (var producto in SeedProductos)
            {
                if (await _context.Productos.AnyAsync(p => p.Nombre == producto.Nombre))
                    continue;

                _context.Productos.Add(new Producto
                {
                    Nombre = producto.Nombre,
                    Precio = producto.Precio,
                    Demora = producto.Demora,
                });
            }

            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error al ejecutar ProductoSeedService");
        }
    }
}

using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Domain.Entities;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services.Interfaces;

namespace ApiGastronomia.Services;

/// <summary>
/// CRUD service for Producto entities. Returns DTOs.
/// Uses soft delete (Activo = false) instead of physical deletion.
/// GetById returns null for inactive products — they are invisible to all callers.
/// </summary>
public class ProductoService : IProductoService
{
    private readonly AppDbContext _context;

    public ProductoService(AppDbContext context)
    {
        _context = context;
    }

    public async Task<IEnumerable<ProductoResponse>> ObtenerProductosAsync()
    {
        return await _context.Productos
            .Where(p => p.Activo)
            .Select(p => new ProductoResponse(
                p.Id,
                p.Nombre,
                p.Precio,
                p.Demora,
                p.Activo))
            .ToListAsync();
    }

    public async Task<ProductoResponse?> ObtenerProductoPorIdAsync(int id)
    {
        var producto = await _context.Productos.FirstOrDefaultAsync(p => p.Id == id);

        if (producto is null || !producto.Activo)
            return null;

        return new ProductoResponse(
            producto.Id,
            producto.Nombre,
            producto.Precio,
            producto.Demora,
            producto.Activo);
    }

    public async Task<ProductoResponse> CrearProductoAsync(string nombre, double precio, int demora)
    {
        if (await _context.Productos.AnyAsync(p => p.Nombre == nombre))
        {
            throw new InvalidOperationException($"Ya existe un producto con el nombre '{nombre}'.");
        }

        var producto = new Producto
        {
            Nombre = nombre,
            Precio = precio,
            Demora = demora,
            Activo = true
        };

        _context.Productos.Add(producto);
        await _context.SaveChangesAsync();

        return new ProductoResponse(
            producto.Id,
            producto.Nombre,
            producto.Precio,
            producto.Demora,
            producto.Activo);
    }

    public async Task<ProductoResponse?> ActualizarProductoAsync(int id, string? nombre, double? precio, int? demora)
    {
        var producto = await _context.Productos.FirstOrDefaultAsync(p => p.Id == id);

        if (producto is null)
            return null;

        if (nombre is not null)
        {
            if (await _context.Productos.AnyAsync(p => p.Id != id && p.Nombre == nombre && p.Activo))
            {
                throw new InvalidOperationException($"Ya existe un producto con el nombre '{nombre}'.");
            }
            producto.Nombre = nombre;
        }

        if (precio.HasValue)
            producto.Precio = precio.Value;

        if (demora.HasValue)
            producto.Demora = demora.Value;

        await _context.SaveChangesAsync();

        return new ProductoResponse(
            producto.Id,
            producto.Nombre,
            producto.Precio,
            producto.Demora,
            producto.Activo);
    }

    public async Task<bool> EliminarProductoAsync(int id)
    {
        var producto = await _context.Productos.FindAsync(id);

        if (producto is null || !producto.Activo)
            return false;

        producto.Activo = false;
        await _context.SaveChangesAsync();

        return true;
    }
}
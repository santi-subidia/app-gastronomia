using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Domain.Entities;

namespace ApiGastronomia.Infrastructure.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<Rol> Roles => Set<Rol>();
    public DbSet<Usuario> Usuarios => Set<Usuario>();
    public DbSet<Producto> Productos => Set<Producto>();
    public DbSet<MetodoPago> MetodoPago => Set<MetodoPago>();
    public DbSet<MetodoVenta> MetodosVenta => Set<MetodoVenta>();
    public DbSet<EstadoPedido> EstadosPedidos => Set<EstadoPedido>();
    public DbSet<Pedido> Pedidos => Set<Pedido>();
    public DbSet<DetallePedido> DetallePedidos => Set<DetallePedido>();
    public DbSet<Caja> Cajas => Set<Caja>();
    public DbSet<Demora> Demoras => Set<Demora>();
    public DbSet<Configuracion> Configuracion => Set<Configuracion>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // ================================================================
        // Usuario
        // ================================================================
        modelBuilder.Entity<Usuario>(e =>
        {
            e.HasOne(u => u.Rol)
             .WithMany(r => r.Usuarios)
             .HasForeignKey(u => u.RolId)
             .OnDelete(DeleteBehavior.Restrict);

            e.HasIndex(u => u.UsuarioNombre).IsUnique();
        });

        // ================================================================
        // Pedido
        // ================================================================
        modelBuilder.Entity<Pedido>(e =>
        {
            // Pedido -> Caja (nullable)
            e.HasOne(p => p.Caja)
             .WithMany(c => c.Pedidos)
             .HasForeignKey(p => p.CajaId)
             .OnDelete(DeleteBehavior.SetNull);

            // Pedido -> Repartidor (Usuario)
            e.HasOne(p => p.Repartidor)
             .WithMany(u => u.PedidosAsignados)
             .HasForeignKey(p => p.RepartidorId)
             .OnDelete(DeleteBehavior.SetNull);

            // Pedido -> EstadoPedido
            e.HasOne(p => p.Estado)
             .WithMany(ep => ep.Pedidos)
             .HasForeignKey(p => p.EstadoId)
             .OnDelete(DeleteBehavior.Restrict);

            // Pedido -> MetodoPago
            e.HasOne(p => p.MetodoPago)
             .WithMany(mp => mp.Pedidos)
             .HasForeignKey(p => p.MetodoPagoId)
             .OnDelete(DeleteBehavior.Restrict);

            // Pedido -> MetodoVenta
            e.HasOne(p => p.MetodoVenta)
             .WithMany(mv => mv.Pedidos)
             .HasForeignKey(p => p.MetodoVentaId)
             .OnDelete(DeleteBehavior.Restrict);
        });

        // ================================================================
        // DetallePedido — PK compuesta (pedido_id, producto_id)
        // ================================================================
        modelBuilder.Entity<DetallePedido>(e =>
        {
            e.HasKey(d => new { d.PedidoId, d.ProductoId });

            e.HasOne(d => d.Pedido)
             .WithMany(p => p.DetallePedidos)
             .HasForeignKey(d => d.PedidoId)
             .OnDelete(DeleteBehavior.Cascade);

            e.HasOne(d => d.Producto)
             .WithMany(p => p.DetallePedidos)
             .HasForeignKey(d => d.ProductoId)
             .OnDelete(DeleteBehavior.Restrict);
        });

        // ================================================================
        // Caja — dos FK a Usuario
        // ================================================================
        modelBuilder.Entity<Caja>(e =>
        {
            e.HasOne(c => c.UsuarioApertura)
             .WithMany(u => u.CajasApertura)
             .HasForeignKey(c => c.UsuarioAperturaId)
             .OnDelete(DeleteBehavior.Restrict);

            e.HasOne(c => c.UsuarioCierre)
             .WithMany(u => u.CajasCierre)
             .HasForeignKey(c => c.UsuarioCierreId)
             .OnDelete(DeleteBehavior.SetNull);
        });

        // ================================================================
        // Demora
        // ================================================================
        modelBuilder.Entity<Demora>(e =>
        {
            e.HasOne(d => d.Usuario)
             .WithMany(u => u.Demoras)
             .HasForeignKey(d => d.UsuarioId)
             .OnDelete(DeleteBehavior.Restrict);

            e.HasOne(d => d.Pedido)
             .WithMany(p => p.Demoras)
             .HasForeignKey(d => d.PedidoId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        // ================================================================
        // Configuracion -> MetodoVenta (metodo_pago_default_id)
        // ================================================================
        modelBuilder.Entity<Configuracion>(e =>
        {
            e.HasOne(c => c.MetodoPagoDefault)
             .WithMany()
             .HasForeignKey(c => c.MetodoPagoDefaultId)
             .OnDelete(DeleteBehavior.SetNull);
        });

        // ================================================================
        // Unique indexes
        // ================================================================
        modelBuilder.Entity<Usuario>()
            .HasIndex(u => u.UsuarioNombre)
            .IsUnique();

        modelBuilder.Entity<Producto>()
            .HasIndex(p => p.Nombre)
            .IsUnique();

        modelBuilder.Entity<Rol>()
            .HasIndex(r => r.Nombre)
            .IsUnique();

        modelBuilder.Entity<EstadoPedido>()
            .HasIndex(ep => ep.Nombre)
            .IsUnique();

        // ================================================================
        // Seed Data — EstadoPedido seed moved to EstadoPedidoSeedService
        // ================================================================
    }
}

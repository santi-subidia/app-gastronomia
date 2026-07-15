using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("cajas")]
public class Caja
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    // FK -> Usuario (apertura)
    [Column("usuario_apertura_id")]
    public int UsuarioAperturaId { get; set; }

    [ForeignKey(nameof(UsuarioAperturaId))]
    public Usuario UsuarioApertura { get; set; } = null!;

    // FK -> Usuario (cierre, nullable)
    [Column("usuario_cierre_id")]
    public int? UsuarioCierreId { get; set; }

    [ForeignKey(nameof(UsuarioCierreId))]
    public Usuario? UsuarioCierre { get; set; }

    [Column("fecha_apertura")]
    public DateTime FechaApertura { get; set; } = DateTime.UtcNow;

    [Column("fecha_cierre")]
    public DateTime? FechaCierre { get; set; }

    [Column("monto_apertura")]
    public decimal MontoApertura { get; set; }

    [Column("monto_cierre_teorico")]
    public decimal? MontoCierreTeorico { get; set; }

    [Column("monto_cierre_real")]
    public decimal? MontoCierreReal { get; set; }

    // Navegación inversa: pedidos asociados a esta caja
    public ICollection<Pedido> Pedidos { get; set; } = new List<Pedido>();
}

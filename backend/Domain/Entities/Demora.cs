using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("demoras")]
public class Demora
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    // FK -> Usuario (quién registró la demora)
    [Column("usuario_id")]
    public int UsuarioId { get; set; }

    [ForeignKey(nameof(UsuarioId))]
    public Usuario Usuario { get; set; } = null!;

    // FK -> Pedido
    [Column("pedido_id")]
    public int PedidoId { get; set; }

    [ForeignKey(nameof(PedidoId))]
    public Pedido Pedido { get; set; } = null!;

    /// <summary>
    /// Minutos adicionales de demora.
    /// </summary>
    [Column("demora")]
    public int DemoraMinutos { get; set; }

    /// <summary>
    /// Sector que reporta la demora (ej: "cocina", "repartidor").
    /// </summary>
    [MaxLength(100)]
    [Column("sector")]
    public string? Sector { get; set; }

    /// <summary>
    /// Observaciones adicionales sobre la demora.
    /// </summary>
    [MaxLength(500)]
    [Column("observaciones")]
    public string? Observaciones { get; set; }
}

using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("metodo_venta")]
public class MetodoVenta
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required, MaxLength(50)]
    [Column("nombre")]
    public string Nombre { get; set; } = string.Empty;

    // Navegación
    public ICollection<Pedido> Pedidos { get; set; } = new List<Pedido>();
}

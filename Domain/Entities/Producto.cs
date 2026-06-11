using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("productos")]
public class Producto
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required, MaxLength(150)]
    [Column("nombre")]
    public string Nombre { get; set; } = string.Empty;

    [Column("precio")]
    public double Precio { get; set; }

    /// <summary>
    /// Tiempo estimado de preparación en minutos.
    /// </summary>
    [Column("demora")]
    public int Demora { get; set; }

    [Column("activo")]
    public bool Activo { get; set; } = true;

    // Navegación
    public ICollection<DetallePedido> DetallePedidos { get; set; } = new List<DetallePedido>();
}

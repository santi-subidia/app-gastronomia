using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("configuracion")]
public class Configuracion
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    // FK -> MetodoVenta (método de pago por defecto)
    [Column("metodo_pago_default_id")]
    public int? MetodoPagoDefaultId { get; set; }

    [ForeignKey(nameof(MetodoPagoDefaultId))]
    public MetodoVenta? MetodoPagoDefault { get; set; }

    [MaxLength(150)]
    [Column("nombreGastronomico")]
    public string? NombreGastronomico { get; set; }

    /// <summary>
    /// Coordenadas de partida del local (latitud).
    /// </summary>
    [Column("latitud_partida")]
    public double? LatitudPartida { get; set; }

    /// <summary>
    /// Coordenadas de partida del local (longitud).
    /// </summary>
    [Column("longitud_partida")]
    public double? LongitudPartida { get; set; }
}

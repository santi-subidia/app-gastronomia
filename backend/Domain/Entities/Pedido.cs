using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using ApiGastronomia.Domain.Enums;

namespace ApiGastronomia.Domain.Entities;

[Table("pedidos")]
public class Pedido
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("caja_id")]
    public int? CajaId { get; set; }

    [ForeignKey(nameof(CajaId))]
    public Caja? Caja { get; set; }

    [Column("repartidor_id")]
    public int? RepartidorId { get; set; }

    [ForeignKey(nameof(RepartidorId))]
    public Usuario? Repartidor { get; set; }

    [Column("estado_id")]
    public int EstadoId { get; set; }

    [ForeignKey(nameof(EstadoId))]
    public EstadoPedido Estado { get; set; } = null!;

    [Column("metodo_pago_id")]
    public int MetodoPagoId { get; set; }

    [ForeignKey(nameof(MetodoPagoId))]
    public MetodoPago MetodoPago { get; set; } = null!;

    [Column("metodo_venta_id")]
    public int MetodoVentaId { get; set; }

    [ForeignKey(nameof(MetodoVentaId))]
    public MetodoVenta MetodoVenta { get; set; } = null!;

    [MaxLength(150)]
    [Column("cliente_nombre")]
    public string? ClienteNombre { get; set; }

    [MaxLength(300)]
    [Column("cliente_direccion")]
    public string? ClienteDireccion { get; set; }

    /// <summary>
    /// Demora estimada total del pedido en minutos.
    /// </summary>
    [Column("demora_aprox")]
    public int? DemoraAprox { get; set; }

    [Column("demora_preparacion_aprox")]
    public int? DemoraPreparacionAprox { get; set; }

    [Column("demora_demoras_aprox")]
    public int? DemoraDemorasAprox { get; set; }

    [Column("demora_delivery_aprox")]
    public int? DemoraDeliveryAprox { get; set; }

    [Column("latitud_destino")]
    public double? LatitudDestino { get; set; }

    [Column("longitud_destino")]
    public double? LongitudDestino { get; set; }

    [Column("total_estimado")]
    public double TotalEstimado { get; set; }

    [Column("fecha_ingreso")]
    public DateTime FechaIngreso { get; set; } = DateTime.UtcNow;

    [Column("fecha_estimado_fin")]
    public DateTime? FechaEstimadoFin { get; set; }

    [Column("fecha_asignado")]
    public DateTime? FechaAsignado { get; set; }

    [Column("fecha_en_camino")]
    public DateTime? FechaEnCamino { get; set; }

    [Column("fecha_finalizado")]
    public DateTime? FechaFinalizado { get; set; }

    // Propiedad calculada (no mapeada) para el enum
    [NotMapped]
    public EstadoPedidoEnum EstadoEnum
    {
        get => (EstadoPedidoEnum)EstadoId;
        set => EstadoId = (int)value;
    }

    public ICollection<DetallePedido> DetallePedidos { get; set; } = new List<DetallePedido>();
    public ICollection<Demora> Demoras { get; set; } = new List<Demora>();
}

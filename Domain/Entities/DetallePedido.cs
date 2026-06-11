using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("detalle_pedidos")]
public class DetallePedido
{
    // PK compuesta: pedido_id + producto_id
    [Column("pedido_id")]
    public int PedidoId { get; set; }

    [ForeignKey(nameof(PedidoId))]
    public Pedido Pedido { get; set; } = null!;

    [Column("producto_id")]
    public int ProductoId { get; set; }

    [ForeignKey(nameof(ProductoId))]
    public Producto Producto { get; set; } = null!;

    /// <summary>
    /// Nombre del producto al momento del pedido (snapshot histórico).
    /// </summary>
    [MaxLength(150)]
    [Column("nombre")]
    public string Nombre { get; set; } = string.Empty;

    [Column("precio")]
    public double Precio { get; set; }

    [Column("cantidad")]
    public int Cantidad { get; set; } = 1;
}

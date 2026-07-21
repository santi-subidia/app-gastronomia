using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ApiGastronomia.Domain.Entities;

[Table("usuarios")]
public class Usuario
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Required, MaxLength(100)]
    [Column("usuario")]
    public string UsuarioNombre { get; set; } = string.Empty;

    [Required, MaxLength(500)]
    [Column("password_hash")]
    public string PasswordHash { get; set; } = string.Empty;

    [Column("disponible")]
    public bool Disponible { get; set; } = true;

    [Column("activo")]
    public bool Activo { get; set; } = true;

    [Column("rol_id")]
    public int RolId { get; set; }

    [ForeignKey(nameof(RolId))]
    public Rol Rol { get; set; } = null!;

    [InverseProperty(nameof(Pedido.Repartidor))]
    public ICollection<Pedido> PedidosAsignados { get; set; } = new List<Pedido>();

    // Cajas (apertura)
    [InverseProperty(nameof(Caja.UsuarioApertura))]
    public ICollection<Caja> CajasApertura { get; set; } = new List<Caja>();

    // Cajas (cierre)
    [InverseProperty(nameof(Caja.UsuarioCierre))]
    public ICollection<Caja> CajasCierre { get; set; } = new List<Caja>();

    // Demoras registradas por este usuario
    public ICollection<Demora> Demoras { get; set; } = new List<Demora>();
}

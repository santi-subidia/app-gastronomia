using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class AddFueraDeServicioUsuario : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<bool>(
                name: "fuera_de_servicio",
                table: "usuarios",
                type: "boolean",
                nullable: false,
                defaultValue: false);

            migrationBuilder.AddColumn<int>(
                name: "max_pedidos_por_repartidor",
                table: "configuracion",
                type: "integer",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "fuera_de_servicio",
                table: "usuarios");

            migrationBuilder.DropColumn(
                name: "max_pedidos_por_repartidor",
                table: "configuracion");
        }
    }
}

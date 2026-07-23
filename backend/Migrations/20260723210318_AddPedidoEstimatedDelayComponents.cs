using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class AddPedidoEstimatedDelayComponents : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "demora_delivery_aprox",
                table: "pedidos",
                type: "integer",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "demora_demoras_aprox",
                table: "pedidos",
                type: "integer",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "demora_preparacion_aprox",
                table: "pedidos",
                type: "integer",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "demora_delivery_aprox",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "demora_demoras_aprox",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "demora_preparacion_aprox",
                table: "pedidos");
        }
    }
}

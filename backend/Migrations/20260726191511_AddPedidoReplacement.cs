using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class AddPedidoReplacement : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "motivo_cancelacion",
                table: "pedidos",
                type: "character varying(255)",
                maxLength: 255,
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "pedido_origen_id",
                table: "pedidos",
                type: "integer",
                nullable: true);

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_pedido_origen_id",
                table: "pedidos",
                column: "pedido_origen_id");

            migrationBuilder.AddForeignKey(
                name: "FK_pedidos_pedidos_pedido_origen_id",
                table: "pedidos",
                column: "pedido_origen_id",
                principalTable: "pedidos",
                principalColumn: "id",
                onDelete: ReferentialAction.Restrict);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_pedidos_pedidos_pedido_origen_id",
                table: "pedidos");

            migrationBuilder.DropIndex(
                name: "IX_pedidos_pedido_origen_id",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "motivo_cancelacion",
                table: "pedidos");

            migrationBuilder.DropColumn(
                name: "pedido_origen_id",
                table: "pedidos");
        }
    }
}

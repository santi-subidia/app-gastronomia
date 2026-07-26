using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class CorregirMetodoPagoDefault : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_configuracion_metodo_venta_metodo_pago_default_id",
                table: "configuracion");

            // Reinterpret the old sales-method ID by matching its name to a
            // payment method. Values without a payment equivalent are cleared.
            migrationBuilder.Sql("""
                UPDATE configuracion AS c
                SET metodo_pago_default_id = mp.id
                FROM metodo_venta AS mv
                LEFT JOIN metodo_pago AS mp ON mp.nombre = mv.nombre
                WHERE c.metodo_pago_default_id = mv.id;
                """);

            migrationBuilder.AddForeignKey(
                name: "FK_configuracion_metodo_pago_metodo_pago_default_id",
                table: "configuracion",
                column: "metodo_pago_default_id",
                principalTable: "metodo_pago",
                principalColumn: "id",
                onDelete: ReferentialAction.SetNull);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_configuracion_metodo_pago_metodo_pago_default_id",
                table: "configuracion");

            migrationBuilder.Sql("""
                UPDATE configuracion AS c
                SET metodo_pago_default_id = mv.id
                FROM metodo_pago AS mp
                LEFT JOIN metodo_venta AS mv ON mv.nombre = mp.nombre
                WHERE c.metodo_pago_default_id = mp.id;
                """);

            migrationBuilder.AddForeignKey(
                name: "FK_configuracion_metodo_venta_metodo_pago_default_id",
                table: "configuracion",
                column: "metodo_pago_default_id",
                principalTable: "metodo_venta",
                principalColumn: "id",
                onDelete: ReferentialAction.SetNull);
        }
    }
}

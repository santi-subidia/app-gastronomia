using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class AddProductoNombreUniqueIndex : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateIndex(
                name: "IX_productos_nombre",
                table: "productos",
                column: "nombre",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_productos_nombre",
                table: "productos");
        }
    }
}

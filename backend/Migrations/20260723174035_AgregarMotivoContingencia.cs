using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class AgregarMotivoContingencia : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "motivo_fuera_servicio",
                table: "usuarios",
                type: "character varying(255)",
                maxLength: 255,
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "motivo_fuera_servicio",
                table: "usuarios");
        }
    }
}

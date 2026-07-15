using Microsoft.EntityFrameworkCore.Migrations;

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class RemoveEstadoPedidoSeeds : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Seeds removed; runtime seed service handles data
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // No-op: runtime seed service handles data
        }
    }
}
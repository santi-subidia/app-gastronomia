using System;
using Microsoft.EntityFrameworkCore.Migrations;
using Npgsql.EntityFrameworkCore.PostgreSQL.Metadata;

#nullable disable

#pragma warning disable CA1814 // Prefer jagged arrays over multidimensional

namespace ApiGastronomia.Migrations
{
    /// <inheritdoc />
    public partial class InitialCreate : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "estados_pedidos",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    nombre = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_estados_pedidos", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "metodo_pago",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    nombre = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_metodo_pago", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "metodo_venta",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    nombre = table.Column<string>(type: "character varying(50)", maxLength: 50, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_metodo_venta", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "productos",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    nombre = table.Column<string>(type: "character varying(150)", maxLength: 150, nullable: false),
                    precio = table.Column<double>(type: "double precision", nullable: false),
                    demora = table.Column<int>(type: "integer", nullable: false),
                    activo = table.Column<bool>(type: "boolean", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_productos", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "roles",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    nombre = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_roles", x => x.id);
                });

            migrationBuilder.CreateTable(
                name: "configuracion",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    metodo_pago_default_id = table.Column<int>(type: "integer", nullable: true),
                    nombreGastronomico = table.Column<string>(type: "character varying(150)", maxLength: 150, nullable: true),
                    latitud_partida = table.Column<double>(type: "double precision", nullable: true),
                    longitud_partida = table.Column<double>(type: "double precision", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_configuracion", x => x.id);
                    table.ForeignKey(
                        name: "FK_configuracion_metodo_venta_metodo_pago_default_id",
                        column: x => x.metodo_pago_default_id,
                        principalTable: "metodo_venta",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "usuarios",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    usuario = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: false),
                    password_hash = table.Column<string>(type: "character varying(500)", maxLength: 500, nullable: false),
                    disponible = table.Column<bool>(type: "boolean", nullable: false),
                    activo = table.Column<bool>(type: "boolean", nullable: false),
                    rol_id = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_usuarios", x => x.id);
                    table.ForeignKey(
                        name: "FK_usuarios_roles_rol_id",
                        column: x => x.rol_id,
                        principalTable: "roles",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "cajas",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    usuario_apertura_id = table.Column<int>(type: "integer", nullable: false),
                    usuario_cierre_id = table.Column<int>(type: "integer", nullable: true),
                    fecha_apertura = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    fecha_cierre = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    monto_apertura = table.Column<decimal>(type: "numeric", nullable: false),
                    monto_cierre_teorico = table.Column<decimal>(type: "numeric", nullable: true),
                    monto_cierre_real = table.Column<decimal>(type: "numeric", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_cajas", x => x.id);
                    table.ForeignKey(
                        name: "FK_cajas_usuarios_usuario_apertura_id",
                        column: x => x.usuario_apertura_id,
                        principalTable: "usuarios",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_cajas_usuarios_usuario_cierre_id",
                        column: x => x.usuario_cierre_id,
                        principalTable: "usuarios",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "pedidos",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    caja_id = table.Column<int>(type: "integer", nullable: true),
                    repartidor_id = table.Column<int>(type: "integer", nullable: true),
                    estado_id = table.Column<int>(type: "integer", nullable: false),
                    metodo_pago_id = table.Column<int>(type: "integer", nullable: false),
                    metodo_venta_id = table.Column<int>(type: "integer", nullable: false),
                    cliente_nombre = table.Column<string>(type: "character varying(150)", maxLength: 150, nullable: true),
                    cliente_direccion = table.Column<string>(type: "character varying(300)", maxLength: 300, nullable: true),
                    demora_aprox = table.Column<int>(type: "integer", nullable: true),
                    latitud_destino = table.Column<double>(type: "double precision", nullable: true),
                    longitud_destino = table.Column<double>(type: "double precision", nullable: true),
                    total_estimado = table.Column<double>(type: "double precision", nullable: false),
                    fecha_ingreso = table.Column<DateTime>(type: "timestamp with time zone", nullable: false),
                    fecha_estimado_fin = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    fecha_asignado = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    fecha_en_camino = table.Column<DateTime>(type: "timestamp with time zone", nullable: true),
                    fecha_finalizado = table.Column<DateTime>(type: "timestamp with time zone", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_pedidos", x => x.id);
                    table.ForeignKey(
                        name: "FK_pedidos_cajas_caja_id",
                        column: x => x.caja_id,
                        principalTable: "cajas",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                    table.ForeignKey(
                        name: "FK_pedidos_estados_pedidos_estado_id",
                        column: x => x.estado_id,
                        principalTable: "estados_pedidos",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_pedidos_metodo_pago_metodo_pago_id",
                        column: x => x.metodo_pago_id,
                        principalTable: "metodo_pago",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_pedidos_metodo_venta_metodo_venta_id",
                        column: x => x.metodo_venta_id,
                        principalTable: "metodo_venta",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                    table.ForeignKey(
                        name: "FK_pedidos_usuarios_repartidor_id",
                        column: x => x.repartidor_id,
                        principalTable: "usuarios",
                        principalColumn: "id",
                        onDelete: ReferentialAction.SetNull);
                });

            migrationBuilder.CreateTable(
                name: "demoras",
                columns: table => new
                {
                    id = table.Column<int>(type: "integer", nullable: false)
                        .Annotation("Npgsql:ValueGenerationStrategy", NpgsqlValueGenerationStrategy.IdentityByDefaultColumn),
                    usuario_id = table.Column<int>(type: "integer", nullable: false),
                    pedido_id = table.Column<int>(type: "integer", nullable: false),
                    demora = table.Column<int>(type: "integer", nullable: false),
                    sector = table.Column<string>(type: "character varying(100)", maxLength: 100, nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_demoras", x => x.id);
                    table.ForeignKey(
                        name: "FK_demoras_pedidos_pedido_id",
                        column: x => x.pedido_id,
                        principalTable: "pedidos",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_demoras_usuarios_usuario_id",
                        column: x => x.usuario_id,
                        principalTable: "usuarios",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "detalle_pedidos",
                columns: table => new
                {
                    pedido_id = table.Column<int>(type: "integer", nullable: false),
                    producto_id = table.Column<int>(type: "integer", nullable: false),
                    nombre = table.Column<string>(type: "character varying(150)", maxLength: 150, nullable: false),
                    precio = table.Column<double>(type: "double precision", nullable: false),
                    cantidad = table.Column<int>(type: "integer", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_detalle_pedidos", x => new { x.pedido_id, x.producto_id });
                    table.ForeignKey(
                        name: "FK_detalle_pedidos_pedidos_pedido_id",
                        column: x => x.pedido_id,
                        principalTable: "pedidos",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_detalle_pedidos_productos_producto_id",
                        column: x => x.producto_id,
                        principalTable: "productos",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.InsertData(
                table: "estados_pedidos",
                columns: new[] { "id", "nombre" },
                values: new object[,]
                {
                    { 1, "Pendiente" },
                    { 2, "EnPreparacion" },
                    { 3, "ListoParaRetirar" },
                    { 4, "EnCamino" },
                    { 5, "Entregado" },
                    { 6, "Retirado" },
                    { 7, "Cancelado" }
                });

            migrationBuilder.InsertData(
                table: "roles",
                columns: new[] { "id", "nombre" },
                values: new object[,]
                {
                    { 1, "Admin" },
                    { 2, "Cocinero" },
                    { 3, "Repartidor" },
                    { 4, "Cajero" }
                });

            migrationBuilder.CreateIndex(
                name: "IX_cajas_usuario_apertura_id",
                table: "cajas",
                column: "usuario_apertura_id");

            migrationBuilder.CreateIndex(
                name: "IX_cajas_usuario_cierre_id",
                table: "cajas",
                column: "usuario_cierre_id");

            migrationBuilder.CreateIndex(
                name: "IX_configuracion_metodo_pago_default_id",
                table: "configuracion",
                column: "metodo_pago_default_id");

            migrationBuilder.CreateIndex(
                name: "IX_demoras_pedido_id",
                table: "demoras",
                column: "pedido_id");

            migrationBuilder.CreateIndex(
                name: "IX_demoras_usuario_id",
                table: "demoras",
                column: "usuario_id");

            migrationBuilder.CreateIndex(
                name: "IX_detalle_pedidos_producto_id",
                table: "detalle_pedidos",
                column: "producto_id");

            migrationBuilder.CreateIndex(
                name: "IX_estados_pedidos_nombre",
                table: "estados_pedidos",
                column: "nombre",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_caja_id",
                table: "pedidos",
                column: "caja_id");

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_estado_id",
                table: "pedidos",
                column: "estado_id");

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_metodo_pago_id",
                table: "pedidos",
                column: "metodo_pago_id");

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_metodo_venta_id",
                table: "pedidos",
                column: "metodo_venta_id");

            migrationBuilder.CreateIndex(
                name: "IX_pedidos_repartidor_id",
                table: "pedidos",
                column: "repartidor_id");

            migrationBuilder.CreateIndex(
                name: "IX_roles_nombre",
                table: "roles",
                column: "nombre",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "IX_usuarios_rol_id",
                table: "usuarios",
                column: "rol_id");

            migrationBuilder.CreateIndex(
                name: "IX_usuarios_usuario",
                table: "usuarios",
                column: "usuario",
                unique: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "configuracion");

            migrationBuilder.DropTable(
                name: "demoras");

            migrationBuilder.DropTable(
                name: "detalle_pedidos");

            migrationBuilder.DropTable(
                name: "pedidos");

            migrationBuilder.DropTable(
                name: "productos");

            migrationBuilder.DropTable(
                name: "cajas");

            migrationBuilder.DropTable(
                name: "estados_pedidos");

            migrationBuilder.DropTable(
                name: "metodo_pago");

            migrationBuilder.DropTable(
                name: "metodo_venta");

            migrationBuilder.DropTable(
                name: "usuarios");

            migrationBuilder.DropTable(
                name: "roles");
        }
    }
}

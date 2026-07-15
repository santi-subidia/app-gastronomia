package com.example.app_movil_gastronomia.ui.pedido;

/**
 * UI-layer representation of one detail line in {@code CrearPedidoFragment}.
 *
 * <p>Spec PED-CRUD-001 / pedido-creacion "DetalleLine UI Model": the
 * {@code DetalleAdapter} and {@code CrearPedidoFragment} work with
 * this POJO so the wire DTO {@code CrearDetalleRequest} stays a pure
 * data-transfer object. The mapping to {@code CrearDetalleRequest}
 * happens at submit time inside
 * {@link CrearPedidoViewModel#mapDetalles(java.util.List)}.</p>
 *
 * <p>Same four fields as {@code CrearDetalleRequest}
 * ({@code productoId, nombre, precio, cantidad}) but without
 * {@code @SerializedName} annotations — these objects never reach
 * the network.</p>
 */
public class DetalleLine {

    private int productoId;
    private String nombre;
    private double precio;
    private int cantidad;

    public DetalleLine(int productoId, String nombre, double precio, int cantidad) {
        this.productoId = productoId;
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
    }

    public int getProductoId() {
        return productoId;
    }

    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }
}

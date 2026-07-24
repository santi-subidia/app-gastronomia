package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

public class CrearDetalleRequest {

    @SerializedName("productoId")
    private int productoId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("precio")
    private double precio;

    @SerializedName("cantidad")
    private int cantidad;

    public CrearDetalleRequest(int productoId, String nombre, double precio, int cantidad) {
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

package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

public class DetallePedidoDto {

    @SerializedName("productoId")
    private int productoId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("cantidad")
    private int cantidad;

    @SerializedName("precio")
    private double precio;

    @SerializedName("tiempoMaquina")
    private int tiempoMaquina;

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

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getTiempoMaquina() {
        return tiempoMaquina;
    }

    public void setTiempoMaquina(int tiempoMaquina) {
        this.tiempoMaquina = tiempoMaquina;
    }
}

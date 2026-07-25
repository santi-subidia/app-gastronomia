package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

public class DetallePedidoCajaDto {
    @SerializedName("productoId") private int productoId;
    @SerializedName("nombre") private String nombre;
    @SerializedName("cantidad") private int cantidad;
    @SerializedName("precio") private double precio;

    public String getNombre() { return nombre; }
    public int getCantidad() { return cantidad; }
    public double getPrecio() { return precio; }
}

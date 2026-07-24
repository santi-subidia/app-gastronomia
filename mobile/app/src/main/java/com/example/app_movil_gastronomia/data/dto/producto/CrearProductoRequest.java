package com.example.app_movil_gastronomia.data.dto.producto;

import com.google.gson.annotations.SerializedName;

public class CrearProductoRequest {

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("precio")
    private double precio;

    @SerializedName("demora")
    private int demora;

    public CrearProductoRequest(String nombre, double precio, int demora) {
        this.nombre = nombre;
        this.precio = precio;
        this.demora = demora;
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

    public int getDemora() {
        return demora;
    }

    public void setDemora(int demora) {
        this.demora = demora;
    }
}

package com.example.app_movil_gastronomia.data.dto.producto;

import com.google.gson.annotations.SerializedName;

public class ActualizarProductoRequest {

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("precio")
    private Double precio;

    @SerializedName("demora")
    private Integer demora;

    public ActualizarProductoRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Integer getDemora() {
        return demora;
    }

    public void setDemora(Integer demora) {
        this.demora = demora;
    }
}

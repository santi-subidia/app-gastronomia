package com.example.app_movil_gastronomia.data.dto.producto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/productos/{id}} (partial update).
 *
 * <p>Spec PROD-CRUD-005: a field that the caller does <b>not</b> set must
 * be omitted from the JSON body — the server treats the request as a
 * partial update. Gson's default behavior is to skip {@code null} boxed
 * fields during serialization, so {@code Double} and {@code Integer} are
 * used instead of primitives.
 */
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

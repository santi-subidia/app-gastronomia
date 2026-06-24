package com.example.app_movil_gastronomia.data.dto.catalogo;

import com.google.gson.annotations.SerializedName;

/**
 * Wire-format representation of a single catalog entry returned by the
 * {@code /api/catalogo/*} endpoints (estados-pedido, metodos-pago,
 * metodos-venta).
 *
 * <p>Spec CAT-DTO-001: each catalog entry is a small object with
 * exactly two fields: an integer {@code id} and a String {@code nombre}.
 * The list response is just an array of these objects — no envelope.</p>
 *
 * <p>Both fields use the same default field names as the JSON keys, so
 * Gson's default behavior covers both directions without custom
 * adapters. The {@link SerializedName} annotations are kept explicit so
 * a future rename of a Java field cannot silently break the wire
 * contract.</p>
 */
public class CatalogoItemDto {

    @SerializedName("id")
    private int id;

    @SerializedName("nombre")
    private String nombre;

    public CatalogoItemDto(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

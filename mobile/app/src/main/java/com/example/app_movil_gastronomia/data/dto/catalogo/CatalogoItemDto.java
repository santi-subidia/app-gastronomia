package com.example.app_movil_gastronomia.data.dto.catalogo;

import com.google.gson.annotations.SerializedName;

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

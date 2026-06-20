package com.example.app_movil_gastronomia.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response body for {@code GET/POST/PUT /api/demoras}.
 *
 * <p>Spec DEM-DTO-001: serialized JSON must contain exactly the keys
 * {@code id}, {@code pedidoId}, {@code usuarioId}, {@code demoraMinutos},
 * {@code sector}, {@code observaciones} — matching the server's contract.
 * All fields are primitives or {@code String} (no boxing required because
 * a {@code DemoraDto} is always returned fully populated by the server).
 */
public class DemoraDto {

    @SerializedName("id")
    private int id;

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("usuarioId")
    private int usuarioId;

    @SerializedName("demoraMinutos")
    private int demoraMinutos;

    @SerializedName("sector")
    private String sector;

    @SerializedName("observaciones")
    private String observaciones;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getDemoraMinutos() {
        return demoraMinutos;
    }

    public void setDemoraMinutos(int demoraMinutos) {
        this.demoraMinutos = demoraMinutos;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

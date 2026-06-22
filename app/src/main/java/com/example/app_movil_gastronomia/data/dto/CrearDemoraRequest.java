package com.example.app_movil_gastronomia.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/demoras}.
 *
 * <p>Spec DEM-DTO-001: serialized JSON must contain exactly the keys
 * {@code pedidoId}, {@code demoraMinutos}, {@code sector},
 * {@code observaciones} — matching the server's contract. All four
 * fields are required by the server, so they are kept as primitives
 * or {@code String}.
 *
 * <p>{@code usuarioId} is intentionally absent: the server derives the
 * caller from the auth token (see design decision in
 * {@code sdd/entidad-demoras/design}).
 */
public class CrearDemoraRequest {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("demoraMinutos")
    private int demoraMinutos;

    @SerializedName("sector")
    private String sector;

    @SerializedName("observaciones")
    private String observaciones;

    public CrearDemoraRequest(int pedidoId, int demoraMinutos, String sector, String observaciones) {
        this.pedidoId = pedidoId;
        this.demoraMinutos = demoraMinutos;
        this.sector = sector;
        this.observaciones = observaciones;
    }

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
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

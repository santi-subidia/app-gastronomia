package com.example.app_movil_gastronomia.data.dto.demora;

import com.google.gson.annotations.SerializedName;

/**
 * Response body for {@code GET/POST/PUT /api/demoras}.
 *
 * <p>Spec DEM-DTO-001 (v2): serialized JSON must contain exactly the
 * keys {@code id}, {@code pedidoId}, {@code usuarioId},
 * {@code demoraMinutos} and {@code observaciones} — the
 * {@code sector} field was removed. All fields are primitives
 * or {@code String} (no boxing required because a {@code DemoraDto}
 * is always returned fully populated by the server).</p>
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

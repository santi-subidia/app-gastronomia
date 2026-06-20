package com.example.app_movil_gastronomia.data.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/demoras/{id}} (partial update).
 *
 * <p>Spec DEM-DTO-001: a field that the caller does <b>not</b> set must
 * be omitted from the JSON body — the server treats the request as a
 * partial update. Gson's default behavior is to skip {@code null} boxed
 * fields during serialization, so {@code demoraMinutos} is {@code Integer}
 * instead of {@code int}. {@code sector} and {@code observaciones} are
 * nullable {@code String} for the same reason.
 *
 * <p>This pattern is verified against {@code ActualizarProductoRequest}.
 */
public class ActualizarDemoraRequest {

    @SerializedName("demoraMinutos")
    private Integer demoraMinutos;

    @SerializedName("sector")
    private String sector;

    @SerializedName("observaciones")
    private String observaciones;

    public ActualizarDemoraRequest() {
    }

    public Integer getDemoraMinutos() {
        return demoraMinutos;
    }

    public void setDemoraMinutos(Integer demoraMinutos) {
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

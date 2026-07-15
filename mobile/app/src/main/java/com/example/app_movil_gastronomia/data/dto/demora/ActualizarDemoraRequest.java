package com.example.app_movil_gastronomia.data.dto.demora;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PUT /api/demoras/{id}} (partial update).
 *
 * <p>Spec DEM-DTO-001 (v2): a field that the caller does <b>not</b> set must
 * be omitted from the JSON body — the server treats the request as a
 * partial update. Gson's default behavior is to skip {@code null} boxed
 * fields during serialization, so {@code demoraMinutos} is {@code Integer}
 * instead of {@code int}. {@code observaciones} is a nullable
 * {@code String} for the same reason. The {@code sector} field was
 * removed in the v2 contract.</p>
 *
 * <p>This pattern is verified against {@code ActualizarProductoRequest}.</p>
 */
public class ActualizarDemoraRequest {

    @SerializedName("demoraMinutos")
    private Integer demoraMinutos;

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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

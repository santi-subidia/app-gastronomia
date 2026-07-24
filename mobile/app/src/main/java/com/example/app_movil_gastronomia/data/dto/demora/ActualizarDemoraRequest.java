package com.example.app_movil_gastronomia.data.dto.demora;

import com.google.gson.annotations.SerializedName;

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

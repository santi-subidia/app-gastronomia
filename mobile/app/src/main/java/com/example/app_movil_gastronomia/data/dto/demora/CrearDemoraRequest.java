package com.example.app_movil_gastronomia.data.dto.demora;

import com.google.gson.annotations.SerializedName;

public class CrearDemoraRequest {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("demoraMinutos")
    private int demoraMinutos;

    @SerializedName("observaciones")
    private String observaciones;

    public CrearDemoraRequest(int pedidoId, int demoraMinutos, String observaciones) {
        this.pedidoId = pedidoId;
        this.demoraMinutos = demoraMinutos;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}

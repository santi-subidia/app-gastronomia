package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

public class DemoraRegistradaMessage {

    @SerializedName("demoraId")
    private int demoraId;

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("demoraMinutos")
    private int demoraMinutos;

    @SerializedName("sector")
    private String sector;

    @SerializedName("observaciones")
    private String observaciones;

    @SerializedName("fecha")
    private String fecha;

    public int getDemoraId() {
        return demoraId;
    }

    public void setDemoraId(int demoraId) {
        this.demoraId = demoraId;
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

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}

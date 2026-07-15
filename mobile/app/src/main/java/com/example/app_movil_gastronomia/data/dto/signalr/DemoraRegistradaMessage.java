package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message for a demora registered against a pedido.
 * Emitted by the {@code logistica} hub on the {@code DemoraRegistrada}
 * event so the Cocina UI can show a countdown/notice and the
 * Repartidor can see expected pick-up delays.
 *
 * <p>Spec SR-DTO-004.</p>
 */
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
}

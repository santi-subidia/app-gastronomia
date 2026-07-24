package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

public class EstadoCambiadoMessage {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("detallePedidoId")
    private Integer detallePedidoId;

    @SerializedName("nuevoEstado")
    private String nuevoEstado;

    @SerializedName("anteriorEstado")
    private String anteriorEstado;

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Integer getDetallePedidoId() {
        return detallePedidoId;
    }

    public void setDetallePedidoId(Integer detallePedidoId) {
        this.detallePedidoId = detallePedidoId;
    }

    public String getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(String nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getAnteriorEstado() {
        return anteriorEstado;
    }

    public void setAnteriorEstado(String anteriorEstado) {
        this.anteriorEstado = anteriorEstado;
    }
}

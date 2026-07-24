package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

public class RepartidorAsignadoMessage {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("repartidorId")
    private int repartidorId;

    @SerializedName("repartidorNombre")
    private String repartidorNombre;

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public int getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(int repartidorId) {
        this.repartidorId = repartidorId;
    }

    public String getRepartidorNombre() {
        return repartidorNombre;
    }

    public void setRepartidorNombre(String repartidorNombre) {
        this.repartidorNombre = repartidorNombre;
    }
}

package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

public class PedidoFinalizadoMessage {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("estadoFinal")
    private String estadoFinal;

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getEstadoFinal() {
        return estadoFinal;
    }

    public void setEstadoFinal(String estadoFinal) {
        this.estadoFinal = estadoFinal;
    }
}

package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message announcing the terminal state of a pedido.
 * Emitted by the {@code logistica} hub on the {@code PedidoFinalizado}
 * event so the Cocina and Cajero views can drop the row from their
 * active lists.
 *
 * <p>Spec SR-DTO-006.</p>
 */
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

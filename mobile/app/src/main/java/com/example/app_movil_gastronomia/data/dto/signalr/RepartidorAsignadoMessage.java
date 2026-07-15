package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message announcing which repartidor was assigned to
 * a given pedido. Emitted by the {@code logistica} hub on the
 * {@code RepartidorAsignado} event so both the Cocina and the
 * Repartidor clients can update their views in real time.
 *
 * <p>Spec SR-DTO-003.</p>
 */
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

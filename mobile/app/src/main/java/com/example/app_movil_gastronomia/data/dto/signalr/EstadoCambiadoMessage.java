package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message for a pedido (or detalle) state transition.
 * Emitted by the {@code logistica} hub on the {@code EstadoCambiado}
 * event. {@code detallePedidoId} is boxed as {@link Integer} so Gson
 * can deserialize a null payload when the transition targets the
 * pedido itself rather than one of its details.
 *
 * <p>Spec SR-DTO-002.</p>
 */
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

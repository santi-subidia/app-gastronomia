package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message for a brand-new pedido. Emitted by the
 * {@code logistica} hub on the {@code NuevoPedido} event so the
 * Cocina UI can refresh the queue without polling.
 *
 * <p>Spec SR-DTO-001: fields match the wire format 1:1.</p>
 */
public class NuevoPedidoMessage {

    @SerializedName("pedidoId")
    private int pedidoId;

    @SerializedName("clienteNombre")
    private String clienteNombre;

    @SerializedName("metodoVenta")
    private String metodoVenta;

    @SerializedName("totalEstimado")
    private double totalEstimado;

    @SerializedName("fechaIngreso")
    private String fechaIngreso;

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getMetodoVenta() {
        return metodoVenta;
    }

    public void setMetodoVenta(String metodoVenta) {
        this.metodoVenta = metodoVenta;
    }

    public double getTotalEstimado() {
        return totalEstimado;
    }

    public void setTotalEstimado(double totalEstimado) {
        this.totalEstimado = totalEstimado;
    }

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(String fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}

package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

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

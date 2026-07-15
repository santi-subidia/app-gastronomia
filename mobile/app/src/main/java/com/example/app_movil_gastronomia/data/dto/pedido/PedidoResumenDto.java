package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

/**
 * Summary projection of a pedido returned by list endpoints
 * ({@code GET /api/pedidos} and {@code GET /api/pedidos/estado/{estado}}).
 *
 * <p>Spec PED-DTO-001: the six fields match the wire format 1:1.</p>
 */
public class PedidoResumenDto {

    @SerializedName("id")
    private int id;

    @SerializedName("estado")
    private String estado;

    @SerializedName("clienteNombre")
    private String clienteNombre;

    @SerializedName("metodoVenta")
    private String metodoVenta;

    @SerializedName("totalEstimado")
    private double totalEstimado;

    @SerializedName("fechaIngreso")
    private String fechaIngreso;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

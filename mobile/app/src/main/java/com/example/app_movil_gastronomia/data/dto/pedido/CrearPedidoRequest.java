package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Request body for {@code POST /api/pedidos}.
 *
 * <p>Spec PED-DTO-001 / PED-CRUD-001: nullable fields (cajaId,
 * latitudDestino, longitudDestino, demoraAprox) are typed as boxed
 * wrappers so Gson omits them from the JSON when null and accepts
 * null on deserialization. Non-nullable fields use primitives per
 * the same null-omit convention used by {@code ActualizarProductoRequest}.</p>
 */
public class CrearPedidoRequest {

    @SerializedName("cajaId")
    private Integer cajaId;

    @SerializedName("metodoPagoId")
    private int metodoPagoId;

    @SerializedName("metodoVentaId")
    private int metodoVentaId;

    @SerializedName("clienteNombre")
    private String clienteNombre;

    @SerializedName("clienteDireccion")
    private String clienteDireccion;

    @SerializedName("latitudDestino")
    private Double latitudDestino;

    @SerializedName("longitudDestino")
    private Double longitudDestino;

    @SerializedName("totalEstimado")
    private double totalEstimado;

    @SerializedName("demoraAprox")
    private Integer demoraAprox;

    @SerializedName("detalles")
    private List<CrearDetalleRequest> detalles;

    public Integer getCajaId() {
        return cajaId;
    }

    public void setCajaId(Integer cajaId) {
        this.cajaId = cajaId;
    }

    public int getMetodoPagoId() {
        return metodoPagoId;
    }

    public void setMetodoPagoId(int metodoPagoId) {
        this.metodoPagoId = metodoPagoId;
    }

    public int getMetodoVentaId() {
        return metodoVentaId;
    }

    public void setMetodoVentaId(int metodoVentaId) {
        this.metodoVentaId = metodoVentaId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public Double getLatitudDestino() {
        return latitudDestino;
    }

    public void setLatitudDestino(Double latitudDestino) {
        this.latitudDestino = latitudDestino;
    }

    public Double getLongitudDestino() {
        return longitudDestino;
    }

    public void setLongitudDestino(Double longitudDestino) {
        this.longitudDestino = longitudDestino;
    }

    public double getTotalEstimado() {
        return totalEstimado;
    }

    public void setTotalEstimado(double totalEstimado) {
        this.totalEstimado = totalEstimado;
    }

    public Integer getDemoraAprox() {
        return demoraAprox;
    }

    public void setDemoraAprox(Integer demoraAprox) {
        this.demoraAprox = demoraAprox;
    }

    public List<CrearDetalleRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<CrearDetalleRequest> detalles) {
        this.detalles = detalles;
    }
}

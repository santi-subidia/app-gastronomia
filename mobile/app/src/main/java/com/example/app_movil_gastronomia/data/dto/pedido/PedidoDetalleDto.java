package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Full detail of a pedido returned by {@code GET /api/pedidos/{id}} and
 * accepted as response from {@code POST /api/pedidos},
 * {@code PATCH /api/pedidos/{id}/estado}, and
 * {@code PATCH /api/pedidos/{id}/repartidor}.
 *
 * <p>Spec PED-DTO-001: lifecycle/timing fields are nullable in the wire
 * contract (the pedido has not yet reached those states) and are typed
 * as boxed {@code Integer} / {@code Double} so Gson can omit them when
 * null and deserialize a missing JSON value as {@code null} instead of
 * a primitive default.</p>
 */
public class PedidoDetalleDto {

    @SerializedName("id")
    private int id;

    @SerializedName("estado")
    private String estado;

    @SerializedName("clienteNombre")
    private String clienteNombre;

    @SerializedName("clienteDireccion")
    private String clienteDireccion;

    @SerializedName("metodoVenta")
    private String metodoVenta;

    @SerializedName("metodoPago")
    private String metodoPago;

    @SerializedName("cajaId")
    private Integer cajaId;

    @SerializedName("repartidorNombre")
    private String repartidorNombre;

    @SerializedName("latitudDestino")
    private Double latitudDestino;

    @SerializedName("longitudDestino")
    private Double longitudDestino;

    @SerializedName("totalEstimado")
    private double totalEstimado;

    @SerializedName("demoraAprox")
    private Integer demoraAprox;

    @SerializedName("fechaIngreso")
    private String fechaIngreso;

    @SerializedName("fechaEstimadoFin")
    private String fechaEstimadoFin;

    @SerializedName("fechaAsignado")
    private String fechaAsignado;

    @SerializedName("fechaEnCamino")
    private String fechaEnCamino;

    @SerializedName("fechaFinalizado")
    private String fechaFinalizado;

    @SerializedName("estadoId")
    private int estadoId;

    @SerializedName("detallePedidos")
    private List<DetallePedidoDto> detallePedidos;

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

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public String getMetodoVenta() {
        return metodoVenta;
    }

    public void setMetodoVenta(String metodoVenta) {
        this.metodoVenta = metodoVenta;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Integer getCajaId() {
        return cajaId;
    }

    public void setCajaId(Integer cajaId) {
        this.cajaId = cajaId;
    }

    public String getRepartidorNombre() {
        return repartidorNombre;
    }

    public void setRepartidorNombre(String repartidorNombre) {
        this.repartidorNombre = repartidorNombre;
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

    public String getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(String fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getFechaEstimadoFin() {
        return fechaEstimadoFin;
    }

    public void setFechaEstimadoFin(String fechaEstimadoFin) {
        this.fechaEstimadoFin = fechaEstimadoFin;
    }

    public String getFechaAsignado() {
        return fechaAsignado;
    }

    public void setFechaAsignado(String fechaAsignado) {
        this.fechaAsignado = fechaAsignado;
    }

    public String getFechaEnCamino() {
        return fechaEnCamino;
    }

    public void setFechaEnCamino(String fechaEnCamino) {
        this.fechaEnCamino = fechaEnCamino;
    }

    public String getFechaFinalizado() {
        return fechaFinalizado;
    }

    public void setFechaFinalizado(String fechaFinalizado) {
        this.fechaFinalizado = fechaFinalizado;
    }

    public int getEstadoId() {
        return estadoId;
    }

    public void setEstadoId(int estadoId) {
        this.estadoId = estadoId;
    }

    public List<DetallePedidoDto> getDetallePedidos() {
        return detallePedidos;
    }

    public void setDetallePedidos(List<DetallePedidoDto> detallePedidos) {
        this.detallePedidos = detallePedidos;
    }
}

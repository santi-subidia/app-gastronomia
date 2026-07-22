package com.example.app_movil_gastronomia.data.dto.configuracion;

import com.google.gson.annotations.SerializedName;

public class ActualizarConfiguracionRequest {

    @SerializedName("metodoPagoDefaultId")
    private Integer metodoPagoDefaultId;

    @SerializedName("nombreGastronomico")
    private String nombreGastronomico;

    @SerializedName("latitudPartida")
    private Double latitudPartida;

    @SerializedName("longitudPartida")
    private Double longitudPartida;

    @SerializedName("maxPedidosPorRepartidor")
    private Integer maxPedidosPorRepartidor;

    public ActualizarConfiguracionRequest() {
    }

    public Integer getMetodoPagoDefaultId() {
        return metodoPagoDefaultId;
    }

    public void setMetodoPagoDefaultId(Integer metodoPagoDefaultId) {
        this.metodoPagoDefaultId = metodoPagoDefaultId;
    }

    public String getNombreGastronomico() {
        return nombreGastronomico;
    }

    public void setNombreGastronomico(String nombreGastronomico) {
        this.nombreGastronomico = nombreGastronomico;
    }

    public Double getLatitudPartida() {
        return latitudPartida;
    }

    public void setLatitudPartida(Double latitudPartida) {
        this.latitudPartida = latitudPartida;
    }

    public Double getLongitudPartida() {
        return longitudPartida;
    }

    public void setLongitudPartida(Double longitudPartida) {
        this.longitudPartida = longitudPartida;
    }

    public Integer getMaxPedidosPorRepartidor() {
        return maxPedidosPorRepartidor;
    }

    public void setMaxPedidosPorRepartidor(Integer maxPedidosPorRepartidor) {
        this.maxPedidosPorRepartidor = maxPedidosPorRepartidor;
    }
}

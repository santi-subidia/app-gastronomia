package com.example.app_movil_gastronomia.data.dto.configuracion;

import com.google.gson.annotations.SerializedName;

/**
 * Response/request body for {@code GET/POST/PUT /api/configuracion}.
 *
 * <p>Spec CONF-DTO-001: serialized JSON must contain exactly the keys
 * {@code id}, {@code metodoPagoDefaultId}, {@code metodoPagoDefaultNombre},
 * {@code nombreGastronomico}, {@code latitudPartida}, {@code longitudPartida}
 * — matching the server's contract. The optional fields
 * {@code metodoPagoDefaultId}, {@code latitudPartida} and
 * {@code longitudPartida} are boxed (nullable) because a fresh business
 * may not yet have a default payment method or GPS coordinates; primitives
 * would silently coerce missing JSON keys to 0 and corrupt state.
 */
public class ConfiguracionDto {

    @SerializedName("id")
    private int id;

    @SerializedName("metodoPagoDefaultId")
    private Integer metodoPagoDefaultId;

    @SerializedName("metodoPagoDefaultNombre")
    private String metodoPagoDefaultNombre;

    @SerializedName("nombreGastronomico")
    private String nombreGastronomico;

    @SerializedName("latitudPartida")
    private Double latitudPartida;

    @SerializedName("longitudPartida")
    private Double longitudPartida;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getMetodoPagoDefaultId() {
        return metodoPagoDefaultId;
    }

    public void setMetodoPagoDefaultId(Integer metodoPagoDefaultId) {
        this.metodoPagoDefaultId = metodoPagoDefaultId;
    }

    public String getMetodoPagoDefaultNombre() {
        return metodoPagoDefaultNombre;
    }

    public void setMetodoPagoDefaultNombre(String metodoPagoDefaultNombre) {
        this.metodoPagoDefaultNombre = metodoPagoDefaultNombre;
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
}

package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/cajas/apertura}.
 *
 * <p>Spec CAJ-DTO-001 (v2): serialized JSON must contain exactly
 * the key {@code montoApertura}. The {@code usuarioAperturaId}
 * field was removed in the v2 contract — the server derives the
 * user from the auth token. The single field is required by the
 * server, so it is kept as a primitive.</p>
 */
public class AbrirCajaRequest {

    @SerializedName("montoApertura")
    private double montoApertura;

    public AbrirCajaRequest(double montoApertura) {
        this.montoApertura = montoApertura;
    }

    public double getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(double montoApertura) {
        this.montoApertura = montoApertura;
    }
}

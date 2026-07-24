package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

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

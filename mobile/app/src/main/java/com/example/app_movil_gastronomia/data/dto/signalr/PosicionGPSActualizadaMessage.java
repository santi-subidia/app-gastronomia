package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

public class PosicionGPSActualizadaMessage {

    @SerializedName("repartidorId")
    private int repartidorId;

    @SerializedName("latitud")
    private double latitud;

    @SerializedName("longitud")
    private double longitud;

    public int getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(int repartidorId) {
        this.repartidorId = repartidorId;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
}

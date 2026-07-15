package com.example.app_movil_gastronomia.data.dto.signalr;

import com.google.gson.annotations.SerializedName;

/**
 * Server-pushed message carrying a repartidor's current GPS
 * coordinates. Emitted by the {@code logistica} hub on the
 * {@code PosicionGPSActualizada} event so the Cajero/Cocina
 * view can track the rider in real time.
 *
 * <p>Spec SR-DTO-005. The hub event name was renamed from
 * {@code PosicionGPS} in the v2 contract; the outbound
 * {@code EnviarPosicionGPS} hub method is unchanged.</p>
 */
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

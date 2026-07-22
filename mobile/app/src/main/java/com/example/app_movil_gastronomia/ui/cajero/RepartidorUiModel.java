package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.Nullable;

/** Immutable presentation model for a delivery driver in the cashier map. */
public final class RepartidorUiModel {

    private final int id;
    private final String nombre;
    private final String estado;
    private final boolean fueraDeServicio;
    @Nullable
    private final Double latitud;
    @Nullable
    private final Double longitud;

    public RepartidorUiModel(int id, String nombre, String estado,
                             boolean fueraDeServicio, @Nullable Double latitud,
                             @Nullable Double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.estado = estado;
        this.fueraDeServicio = fueraDeServicio;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEstado() { return estado; }
    public boolean isFueraDeServicio() { return fueraDeServicio; }
    @Nullable public Double getLatitud() { return latitud; }
    @Nullable public Double getLongitud() { return longitud; }

    public boolean hasLocation() {
        return latitud != null && longitud != null;
    }
}

package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

public class AsignarRepartidorRequest {

    @SerializedName("repartidorId")
    private int repartidorId;

    public AsignarRepartidorRequest(int repartidorId) {
        this.repartidorId = repartidorId;
    }

    public int getRepartidorId() {
        return repartidorId;
    }

    public void setRepartidorId(int repartidorId) {
        this.repartidorId = repartidorId;
    }
}

package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code PATCH /api/pedidos/{id}/repartidor}.
 *
 * <p>Spec PED-DTO-001: the repartidor id is required by the server and
 * is typed as a primitive {@code int}.</p>
 */
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

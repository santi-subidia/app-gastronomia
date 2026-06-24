package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for {@code POST /api/cajas/{id}/cierre}.
 *
 * <p>Spec CAJ-DTO-001 (v2): serialized JSON must contain exactly
 * the keys {@code montoCierreTeorico} and {@code montoCierreReal}.
 * The {@code usuarioCierreId} field was removed in the v2 contract
 * — the server derives the user from the auth token. Both fields
 * are required by the server, so they are kept as primitives.</p>
 */
public class CerrarCajaRequest {

    @SerializedName("montoCierreTeorico")
    private double montoCierreTeorico;

    @SerializedName("montoCierreReal")
    private double montoCierreReal;

    public CerrarCajaRequest(double montoCierreTeorico, double montoCierreReal) {
        this.montoCierreTeorico = montoCierreTeorico;
        this.montoCierreReal = montoCierreReal;
    }

    public double getMontoCierreTeorico() {
        return montoCierreTeorico;
    }

    public void setMontoCierreTeorico(double montoCierreTeorico) {
        this.montoCierreTeorico = montoCierreTeorico;
    }

    public double getMontoCierreReal() {
        return montoCierreReal;
    }

    public void setMontoCierreReal(double montoCierreReal) {
        this.montoCierreReal = montoCierreReal;
    }
}

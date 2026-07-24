package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

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

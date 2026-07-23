package com.example.app_movil_gastronomia.data.dto.usuario;

import com.google.gson.annotations.SerializedName;

public class ReportarContingenciaRequest {
    @SerializedName("motivo")
    private String motivo;

    public ReportarContingenciaRequest(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}

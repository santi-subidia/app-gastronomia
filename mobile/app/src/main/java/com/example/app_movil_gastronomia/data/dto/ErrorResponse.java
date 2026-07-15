package com.example.app_movil_gastronomia.data.dto;

import com.google.gson.annotations.SerializedName;

public class ErrorResponse {

    @SerializedName(value = "mensaje", alternate = {"Mensaje"})
    private String mensaje;

    @SerializedName(value = "codigo", alternate = {"Codigo"})
    private String codigo;

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}

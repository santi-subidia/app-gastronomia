package com.example.app_movil_gastronomia.data.dto.auth;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    @SerializedName("usuarioNombre")
    private String usuarioNombre;

    @SerializedName("password")
    private String password;

    public LoginRequest(String usuarioNombre, String password) {
        this.usuarioNombre = usuarioNombre;
        this.password = password;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
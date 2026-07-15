package com.example.app_movil_gastronomia.data.dto.auth;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("usuarioNombre")
    private String usuarioNombre;

    @SerializedName("rolId")
    private int rolId;

    @SerializedName("rolNombre")
    private String rolNombre;

    @SerializedName("token")
    private String token;

    @SerializedName("expiraEn")
    private String expiraEn;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public int getRolId() {
        return rolId;
    }

    public void setRolId(int rolId) {
        this.rolId = rolId;
    }

    public String getRolNombre() {
        return rolNombre;
    }

    public void setRolNombre(String rolNombre) {
        this.rolNombre = rolNombre;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(String expiraEn) {
        this.expiraEn = expiraEn;
    }
}
package com.example.app_movil_gastronomia.core;

public interface TokenManager {

    void saveToken(String token, String rolNombre, int userId, String nombreUsuario);

    String getToken();

    String getRole();

    int getUserId();

    String getNombreUsuario();

    long decodeTokenExp();

    boolean hasToken();

    void clearToken();
}

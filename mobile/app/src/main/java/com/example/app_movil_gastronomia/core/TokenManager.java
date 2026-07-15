package com.example.app_movil_gastronomia.core;

/**
 * Manages secure storage and retrieval of JWT token and session metadata.
 * Implementation uses EncryptedSharedPreferences for E2E encryption at rest.
 */
public interface TokenManager {

    /**
     * Saves the authentication token and associated session metadata.
     *
     * @param token        the JWT token
     * @param rolNombre    the user's role name (e.g. "Cajero", "Cocina", "Repartidor")
     * @param userId       the user's ID from the login response
     * @param nombreUsuario the user's display name from the login response
     */
    void saveToken(String token, String rolNombre, int userId, String nombreUsuario);

    /** Returns the stored JWT token, or null if no session exists. */
    String getToken();

    /** Returns the stored role name, or null if no session exists. */
    String getRole();

    /** Returns the stored user ID, or -1 if no session exists. */
    int getUserId();

    /** Returns the stored display name, or null if no session exists. */
    String getNombreUsuario();

    /**
     * Decodes the {@code exp} (expiration) claim from the stored JWT, in
     * epoch seconds. Returns {@code -1} if no token is stored, the token
     * is malformed, or the payload cannot be parsed.
     *
     * <p>The caller is responsible for comparing the returned value to
     * {@code System.currentTimeMillis() / 1000} to determine if the token
     * is still valid.
     */
    long decodeTokenExp();

    /** Returns true if a non-null token is stored. */
    boolean hasToken();

    /** Clears all stored session data (token, role, userId, nombreUsuario). */
    void clearToken();
}
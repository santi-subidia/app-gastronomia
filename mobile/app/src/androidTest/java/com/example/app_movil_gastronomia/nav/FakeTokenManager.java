package com.example.app_movil_gastronomia.nav;

import androidx.annotation.Nullable;

import com.example.app_movil_gastronomia.core.TokenManager;

/**
 * Implementación mutable de {@link TokenManager} para pruebas instrumentadas Hilt.
 *
 * <p>El estado inicial representa una instalación nueva. Las pruebas que
 * necesitan una sesión iniciada llaman a
 * {@link #setRole(String)} antes de iniciar la actividad.
 *
 * <p>Las instancias no deben compartirse entre pruebas. Cada prueba configura
 * explícitamente el rol o utiliza el estado inicial sin sesión.
 */
public class FakeTokenManager implements TokenManager {

    /** Desplazamiento de expiración JWT por defecto: una hora futura. */
    private static final long DEFAULT_EXP_OFFSET_SECONDS = 3600L;

    @Nullable
    private volatile String token;
    @Nullable
    private volatile String role;
    @Nullable
    private volatile String userName;
    private volatile int userId = -1;
    private volatile long expSeconds = -1L;

    public FakeTokenManager() {
    }

    /**
     * Configura esta instancia como un usuario autenticado con el rol indicado.
     * Limpia la sesión anterior, establece una expiración futura y asigna un nombre.
     *
     * <p>Pasar {@code null} restablece el estado sin sesión.
     *
     * @param role nombre del rol o null para restablecer la sesión
     */
    public void setRole(@Nullable String role) {
        if (role == null) {
            clearToken();
            return;
        }
        this.role = role;
        this.userId = 1;
        this.userName = "Test " + role;
        this.expSeconds = (System.currentTimeMillis() / 1000L) + DEFAULT_EXP_OFFSET_SECONDS;
        this.token = "fake.jwt.token";
    }

    @Override
    public void saveToken(String token, String rolNombre, int userId, String nombreUsuario) {
        this.token = token;
        this.role = rolNombre;
        this.userId = userId;
        this.userName = nombreUsuario;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public String getNombreUsuario() {
        return userName;
    }

    @Override
    public long decodeTokenExp() {
        return expSeconds;
    }

    @Override
    public boolean hasToken() {
        return token != null;
    }

    @Override
    public void clearToken() {
        this.token = null;
        this.role = null;
        this.userName = null;
        this.userId = -1;
        this.expSeconds = -1L;
    }
}

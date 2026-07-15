package com.example.app_movil_gastronomia.nav;

import androidx.annotation.Nullable;

import com.example.app_movil_gastronomia.core.TokenManager;

/**
 * Mutable {@link TokenManager} test double for instrumented Hilt tests.
 *
 * <p>The default state mimics a fresh app install (no token, no role, no
 * user name) so tests that do not configure a role behave the same as a
 * brand-new session. Tests that need a logged-in user call
 * {@link #setRole(String)} before launching the activity.
 *
 * <p>Instances are NOT safe to share across tests: every test must call
 * {@code setRole} (or rely on the default "no session" state) to make its
 * intent explicit. Hilt's {@code @BindValue} provides a fresh instance
 * per test method, which is the intended usage.
 */
public class FakeTokenManager implements TokenManager {

    /** Default JWT expiration offset: one hour in the future. */
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
        // No-arg constructor: default state is "no session".
    }

    /**
     * Configures this fake to look like a logged-in user with the given
     * role. Clears any previous session state, sets a future JWT
     * expiration, and assigns a synthetic user name.
     *
     * <p>Passing {@code null} resets the fake to the "no session" state,
     * which is the same as constructing a fresh instance.
     *
     * @param role the role name (e.g. "cajero", "cocina", "repartidor")
     *             or null to reset.
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

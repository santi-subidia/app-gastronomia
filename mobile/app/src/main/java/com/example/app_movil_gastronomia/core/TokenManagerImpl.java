package com.example.app_movil_gastronomia.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

/**
 * EncryptedSharedPreferences-backed implementation of {@link TokenManager}.
 * Stores JWT token, role name, user ID, and display name with E2E encryption
 * at rest. Falls back gracefully if MasterKey initialization fails.
 */
public class TokenManagerImpl implements TokenManager {

    private static final String TAG = "TokenManagerImpl";
    private static final String PREFS_FILE_NAME = "secure_auth_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "rol_nombre";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences encryptedPrefs;

    public TokenManagerImpl(Context context) {
        this(createEncryptedPrefs(context));
    }

    /**
     * Test-friendly constructor that accepts a pre-built {@link SharedPreferences}.
     * Package-private to keep production wiring on the {@link Context}-based
     * constructor while letting unit tests inject a fake prefs.
     */
    TokenManagerImpl(SharedPreferences prefs) {
        this.encryptedPrefs = prefs;
    }

    private static SharedPreferences createEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to clear prefs", e);
            return context.getSharedPreferences(PREFS_FILE_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }

    @Override
    public void saveToken(String token, String rolNombre, int userId, String nombreUsuario) {
        encryptedPrefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_ROLE, rolNombre)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, nombreUsuario)
                .apply();
    }

    @Override
    public String getToken() {
        return encryptedPrefs.getString(KEY_TOKEN, null);
    }

    @Override
    public String getRole() {
        return encryptedPrefs.getString(KEY_ROLE, null);
    }

    @Override
    public int getUserId() {
        return encryptedPrefs.getInt(KEY_USER_ID, -1);
    }

    @Override
    public String getNombreUsuario() {
        return encryptedPrefs.getString(KEY_USER_NAME, null);
    }

    /**
     * Decodes the {@code exp} claim from the stored JWT.
     *
     * <p>Splits on {@code "."}, Base64URL-decodes the payload, parses it as
     * JSON, and returns {@code exp} as epoch seconds. Returns {@code -1L}
     * on any failure (no token, malformed JWT, missing/invalid {@code exp}).
     */
    @Override
    public long decodeTokenExp() {
        String token = getToken();
        if (token == null) return -1L;
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return -1L;
            byte[] decoded = decodeBase64Url(parts[1]);
            if (decoded == null) return -1L;
            String payload = new String(decoded, StandardCharsets.UTF_8);
            return new JSONObject(payload).optLong("exp", -1L);
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * Decodes a Base64URL-encoded string. Protected so unit tests can
     * override it with a JVM-compatible decoder; production uses
     * {@code android.util.Base64} to avoid pulling in core-library
     * desugaring or an external library on minSdk 24.
     */
    protected byte[] decodeBase64Url(String s) {
        return android.util.Base64.decode(s, android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING);
    }

    @Override
    public boolean hasToken() {
        return getToken() != null;
    }

    @Override
    public void clearToken() {
        encryptedPrefs.edit().clear().apply();
    }
}

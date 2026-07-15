package com.example.app_movil_gastronomia.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.SharedPreferences;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests for {@link TokenManagerImpl#decodeTokenExp()} and related
 * session storage behavior.
 *
 * <p>The decode logic is structured so production uses
 * {@code android.util.Base64} (no new runtime dependencies on minSdk 24)
 * while tests override the protected {@code decodeBase64Url} hook to use
 * {@link java.util.Base64} — which is a real implementation on the host
 * JVM, unlike the android stub (which returns null under
 * {@code isReturnDefaultValues = true}).
 */
public class TokenManagerImplTest {

    @Test
    public void decodeTokenExp_validToken_returnsExpTimestamp() {
        long expectedExp = 1_700_000_000L; // some future timestamp
        String jwt = buildJwt(expectedExp);
        TokenManagerImpl tm = newTokenManagerWithJwt(jwt);

        long exp = tm.decodeTokenExp();

        assertEquals(expectedExp, exp);
    }

    @Test
    public void decodeTokenExp_expiredToken_returnsExpTimestampInPast() {
        long pastExp = 1_000_000_000L; // year 2001
        String jwt = buildJwt(pastExp);
        TokenManagerImpl tm = newTokenManagerWithJwt(jwt);

        long exp = tm.decodeTokenExp();

        assertEquals(pastExp, exp);
    }

    @Test
    public void decodeTokenExp_malformedToken_returnsNegativeOne() {
        TokenManagerImpl tm = newTokenManagerWithJwt("not-a-jwt");

        long exp = tm.decodeTokenExp();

        assertEquals(-1L, exp);
    }

    @Test
    public void decodeTokenExp_nullToken_returnsNegativeOne() {
        TokenManagerImpl tm = newTokenManagerWithJwt(null);

        long exp = tm.decodeTokenExp();

        assertEquals(-1L, exp);
    }

    @Test
    public void decodeTokenExp_payloadWithoutExp_returnsNegativeOne() {
        // Valid JWT shape but no "exp" claim in the payload.
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode("{\"sub\":\"123\"}");
        String jwt = header + "." + payload + ".sig";
        TokenManagerImpl tm = newTokenManagerWithJwt(jwt);

        long exp = tm.decodeTokenExp();

        assertEquals(-1L, exp);
    }

    @Test
    public void getNombreUsuario_returnsStoredName() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        prefs.edit().putString("user_name", "Juan Perez").apply();
        TokenManagerImpl tm = new TokenManagerImpl(prefs);

        assertEquals("Juan Perez", tm.getNombreUsuario());
    }

    @Test
    public void getNombreUsuario_returnsNullWhenNotStored() {
        TokenManagerImpl tm = new TokenManagerImpl(new FakeSharedPreferences());

        assertNull(tm.getNombreUsuario());
    }

    @Test
    public void clearToken_alsoClearsNombreUsuario() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        prefs.edit().putString("jwt_token", "abc.def.ghi").apply();
        prefs.edit().putString("user_name", "Juan Perez").apply();
        TokenManagerImpl tm = new TokenManagerImpl(prefs);

        tm.clearToken();

        assertNull(tm.getNombreUsuario());
        assertNull(tm.getToken());
    }

    // -- Helpers -------------------------------------------------------------

    /**
     * Builds a TokenManagerImpl wired to a fake prefs pre-populated with the
     * given JWT, and overrides the Base64URL decoder to use the JVM's
     * {@link java.util.Base64} (production uses {@code android.util.Base64}
     * which is a stub on the host JVM).
     */
    private static TokenManagerImpl newTokenManagerWithJwt(String jwt) {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        if (jwt != null) {
            prefs.edit().putString("jwt_token", jwt).apply();
        }
        return new TokenManagerImpl(prefs) {
            @Override
            protected byte[] decodeBase64Url(String s) {
                return Base64.getUrlDecoder().decode(s);
            }
        };
    }

    private static String buildJwt(long exp) {
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode("{\"sub\":\"123\",\"exp\":" + exp + "}");
        return header + "." + payload + ".sig";
    }

    private static String base64UrlEncode(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Minimal in-memory SharedPreferences for unit testing. Only the methods
     * used by {@link TokenManagerImpl} are meaningful; others return defaults.
     */
    static final class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> data = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(data);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = data.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = data.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = data.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = data.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = data.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, Set<String> defValues) {
            Object v = data.get(key);
            return v instanceof Set ? (Set<String>) v : defValues;
        }

        @Override
        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();
            private final AtomicBoolean clear = new AtomicBoolean(false);

            @Override
            public Editor putString(String k, String v) { pending.put(k, v); return this; }

            @Override
            public Editor putInt(String k, int v) { pending.put(k, v); return this; }

            @Override
            public Editor putLong(String k, long v) { pending.put(k, v); return this; }

            @Override
            public Editor putFloat(String k, float v) { pending.put(k, v); return this; }

            @Override
            public Editor putBoolean(String k, boolean v) { pending.put(k, v); return this; }

            @Override
            public Editor putStringSet(String k, Set<String> v) { pending.put(k, v); return this; }

            @Override
            public Editor remove(String k) { pending.remove(k); return this; }

            @Override
            public Editor clear() { clear.set(true); pending.clear(); return this; }

            @Override
            public boolean commit() { apply(); return true; }

            @Override
            public void apply() {
                if (clear.get()) data.clear();
                data.putAll(pending);
            }
        }
    }
}

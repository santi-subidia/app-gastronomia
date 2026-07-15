package com.example.app_movil_gastronomia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for {@link MainActivity#resolveHomeDestination(String)}.
 *
 * <p>The mapping is extracted to a package-private static method so the
 * role-to-destination logic can be verified in plain JUnit — no
 * Robolectric, no Activity instantiation, no Hilt test runner required.
 *
 * <p>Mapping contract (see spec/auto-login):
 * <ul>
 *   <li>"cajero" (case-insensitive, whitespace-trimmed) -> R.id.nav_cajero_home</li>
 *   <li>"cocina" (case-insensitive) -> R.id.nav_cocina_home</li>
 *   <li>"repartidor" (case-insensitive) -> R.id.nav_repartidor_home</li>
 *   <li>anything else (null, empty, blank, unknown) -> null (caller falls back to login)</li>
 * </ul>
 */
public class MainActivityNavResolverTest {

    @Test
    public void cajero_titleCase_returnsCajeroHome() {
        assertEquals(Integer.valueOf(R.id.nav_cajero_home),
                MainActivity.resolveHomeDestination("Cajero"));
    }

    @Test
    public void cocina_titleCase_returnsCocinaHome() {
        assertEquals(Integer.valueOf(R.id.nav_cocina_home),
                MainActivity.resolveHomeDestination("Cocina"));
    }

    @Test
    public void repartidor_titleCase_returnsRepartidorHome() {
        assertEquals(Integer.valueOf(R.id.nav_repartidor_home),
                MainActivity.resolveHomeDestination("Repartidor"));
    }

    @Test
    public void cajero_lowercase_returnsCajeroHome() {
        assertEquals(Integer.valueOf(R.id.nav_cajero_home),
                MainActivity.resolveHomeDestination("cajero"));
    }

    @Test
    public void cocina_lowercase_returnsCocinaHome() {
        assertEquals(Integer.valueOf(R.id.nav_cocina_home),
                MainActivity.resolveHomeDestination("cocina"));
    }

    @Test
    public void repartidor_lowercase_returnsRepartidorHome() {
        assertEquals(Integer.valueOf(R.id.nav_repartidor_home),
                MainActivity.resolveHomeDestination("repartidor"));
    }

    @Test
    public void cajero_upperCase_returnsCajeroHome() {
        // Be defensive: server casing is unconstrained.
        assertEquals(Integer.valueOf(R.id.nav_cajero_home),
                MainActivity.resolveHomeDestination("CAJERO"));
    }

    @Test
    public void valueWithLeadingTrailingWhitespace_isTrimmed() {
        assertEquals(Integer.valueOf(R.id.nav_cajero_home),
                MainActivity.resolveHomeDestination("  Cajero  "));
    }

    @Test
    public void unknownRole_returnsNull() {
        assertNull(MainActivity.resolveHomeDestination("Desconocido"));
    }

    @Test
    public void emptyString_returnsNull() {
        assertNull(MainActivity.resolveHomeDestination(""));
    }

    @Test
    public void whitespaceOnly_returnsNull() {
        assertNull(MainActivity.resolveHomeDestination("   "));
    }

    @Test
    public void null_returnsNull() {
        assertNull(MainActivity.resolveHomeDestination(null));
    }
}

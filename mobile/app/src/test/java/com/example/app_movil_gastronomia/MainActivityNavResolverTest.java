package com.example.app_movil_gastronomia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Pruebas unitarias de {@link MainActivity#resolveHomeDestination(String)}.
 *
 * <p>La resolución está aislada en un método estático para probarla sin
 * la lógica rol-destino se puede verificar con JUnit sin
 * Robolectric, instanciar la actividad ni usar el runner Hilt.
 *
 * <p>Contrato de resolución usado por el auto-login:
 * <ul>
 *   <li>"cajero" -> R.id.nav_cajero_home</li>
 *   <li>"cocina" -> R.id.nav_cocina_home</li>
 *   <li>"repartidor" -> R.id.nav_repartidor_home</li>
 *   <li>cualquier otro valor devuelve null y permite volver al login</li>
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

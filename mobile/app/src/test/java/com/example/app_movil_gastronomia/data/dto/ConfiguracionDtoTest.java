package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec CONF-DTO-001: the response/request body for
 * {@code GET/POST/PUT /api/configuracion} must serialize to/from a JSON
 * object with exactly the keys {@code id}, {@code metodoPagoDefaultId},
 * {@code metodoPagoDefaultNombre}, {@code nombreGastronomico},
 * {@code latitudPartida}, {@code longitudPartida} — matching the server's
 * contract. {@code metodoPagoDefaultId}, {@code latitudPartida} and
 * {@code longitudPartida} are nullable (boxed types) because a fresh
 * business may not yet have a default payment method or GPS coordinates.
 */
public class ConfiguracionDtoTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setId(1);
        dto.setMetodoPagoDefaultId(5);
        dto.setMetodoPagoDefaultNombre("Efectivo");
        dto.setNombreGastronomico("La Esquina");
        dto.setLatitudPartida(-34.6037);
        dto.setLongitudPartida(-58.3816);

        String json = gson.toJson(dto);

        // Round-trip: parse back and verify equality of every field.
        ConfiguracionDto parsed = gson.fromJson(json, ConfiguracionDto.class);
        assertEquals(1, parsed.getId());
        assertEquals(Integer.valueOf(5), parsed.getMetodoPagoDefaultId());
        assertEquals("Efectivo", parsed.getMetodoPagoDefaultNombre());
        assertEquals("La Esquina", parsed.getNombreGastronomico());
        assertEquals(-34.6037, parsed.getLatitudPartida(), 0.0);
        assertEquals(-58.3816, parsed.getLongitudPartida(), 0.0);

        // String-level: every required key MUST be present.
        assertTrue("json must contain 'id', got: " + json, json.contains("\"id\""));
        assertTrue("json must contain 'metodoPagoDefaultId', got: " + json, json.contains("\"metodoPagoDefaultId\""));
        assertTrue("json must contain 'metodoPagoDefaultNombre', got: " + json, json.contains("\"metodoPagoDefaultNombre\""));
        assertTrue("json must contain 'nombreGastronomico', got: " + json, json.contains("\"nombreGastronomico\""));
        assertTrue("json must contain 'latitudPartida', got: " + json, json.contains("\"latitudPartida\""));
        assertTrue("json must contain 'longitudPartida', got: " + json, json.contains("\"longitudPartida\""));
    }

    @Test
    public void gettersReturnSetterValues() {
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setId(42);
        dto.setMetodoPagoDefaultId(7);
        dto.setMetodoPagoDefaultNombre("Mercado Pago");
        dto.setNombreGastronomico("Buen Sabor");
        dto.setLatitudPartida(10.5);
        dto.setLongitudPartida(20.75);

        assertEquals(42, dto.getId());
        assertEquals(Integer.valueOf(7), dto.getMetodoPagoDefaultId());
        assertEquals("Mercado Pago", dto.getMetodoPagoDefaultNombre());
        assertEquals("Buen Sabor", dto.getNombreGastronomico());
        assertEquals(10.5, dto.getLatitudPartida(), 0.0);
        assertEquals(20.75, dto.getLongitudPartida(), 0.0);
    }

    @Test
    public void deserializesFromServerJsonShape() {
        String json = "{"
                + "\"id\":99,"
                + "\"metodoPagoDefaultId\":3,"
                + "\"metodoPagoDefaultNombre\":\"Tarjeta\","
                + "\"nombreGastronomico\":\"El Bodegon\","
                + "\"latitudPartida\":-31.4201,"
                + "\"longitudPartida\":-64.1888"
                + "}";

        ConfiguracionDto parsed = gson.fromJson(json, ConfiguracionDto.class);

        assertEquals(99, parsed.getId());
        assertEquals(Integer.valueOf(3), parsed.getMetodoPagoDefaultId());
        assertEquals("Tarjeta", parsed.getMetodoPagoDefaultNombre());
        assertEquals("El Bodegon", parsed.getNombreGastronomico());
        assertEquals(-31.4201, parsed.getLatitudPartida(), 0.0);
        assertEquals(-64.1888, parsed.getLongitudPartida(), 0.0);
    }

    @Test
    public void nullOptionalFieldsRoundTripAsNull() {
        // Build a DTO with optional fields explicitly null (boxed types allow this).
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setId(1);
        dto.setMetodoPagoDefaultId(null);
        dto.setMetodoPagoDefaultNombre(null);
        dto.setNombreGastronomico("Sin Config");
        dto.setLatitudPartida(null);
        dto.setLongitudPartida(null);

        // Getters return null (the boxed-type contract).
        assertNull(dto.getMetodoPagoDefaultId());
        assertNull(dto.getMetodoPagoDefaultNombre());
        assertNull(dto.getLatitudPartida());
        assertNull(dto.getLongitudPartida());

        // Server may omit the optional keys in the JSON wire format.
        // When it does, the DTO MUST deserialize them as null (boxed types required).
        String serverJson = "{"
                + "\"id\":1,"
                + "\"nombreGastronomico\":\"Sin Config\""
                + "}";
        ConfiguracionDto parsed = gson.fromJson(serverJson, ConfiguracionDto.class);
        assertEquals(1, parsed.getId());
        assertNull(parsed.getMetodoPagoDefaultId());
        assertNull(parsed.getMetodoPagoDefaultNombre());
        assertEquals("Sin Config", parsed.getNombreGastronomico());
        assertNull(parsed.getLatitudPartida());
        assertNull(parsed.getLongitudPartida());
    }
}

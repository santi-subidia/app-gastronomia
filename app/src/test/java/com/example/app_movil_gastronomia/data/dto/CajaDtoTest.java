package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

/**
 * Spec CAJ-DTO-001 (v2): {@link CajaDto} deserializes from the
 * {@code GET /api/cajas} response shape. The 9 remaining fields match
 * the v2 wire format 1:1 (the user IDs were removed in v2 — the
 * server derives them from the auth token on writes, and the
 * server only returns the human-readable names for display).
 * Boxed {@code Double} fields ({@code montoCierreTeorico},
 * {@code montoCierreReal}) must stay {@code null} for an open
 * caja that has no cierre data yet.
 *
 * <p>Sample JSON is taken directly from
 * {@code doc/API_REFERENCIA.md} §3.4 GET /api/cajas (v2 shape).</p>
 */
public class CajaDtoTest {

    private final Gson gson = new Gson();

    private static final String OPEN_CAJA_JSON = "{"
            + "\"id\": 1,"
            + "\"usuarioAperturaNombre\": \"cajero1\","
            + "\"usuarioCierreNombre\": null,"
            + "\"fechaApertura\": \"2026-06-18T08:00:00Z\","
            + "\"fechaCierre\": null,"
            + "\"montoApertura\": 5000,"
            + "\"montoCierreTeorico\": null,"
            + "\"montoCierreReal\": null,"
            + "\"estado\": \"abierta\""
            + "}";

    private static final String CLOSED_CAJA_JSON = "{"
            + "\"id\": 2,"
            + "\"usuarioAperturaNombre\": \"cajero1\","
            + "\"usuarioCierreNombre\": \"cajero2\","
            + "\"fechaApertura\": \"2026-06-18T08:00:00Z\","
            + "\"fechaCierre\": \"2026-06-18T20:00:00Z\","
            + "\"montoApertura\": 5000,"
            + "\"montoCierreTeorico\": 25000,"
            + "\"montoCierreReal\": 24850,"
            + "\"estado\": \"cerrada\""
            + "}";

    @Test
    public void deserializesOpenCajaFromSampleJson() {
        CajaDto dto = gson.fromJson(OPEN_CAJA_JSON, CajaDto.class);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("cajero1", dto.getUsuarioAperturaNombre());
        assertNull(dto.getUsuarioCierreNombre());
        assertEquals("2026-06-18T08:00:00Z", dto.getFechaApertura());
        assertNull(dto.getFechaCierre());
        assertEquals(5000.0, dto.getMontoApertura(), 0.0001);
        assertNull(dto.getMontoCierreTeorico());
        assertNull(dto.getMontoCierreReal());
        assertEquals("abierta", dto.getEstado());
    }

    @Test
    public void deserializesClosedCajaWithAllCierreFields() {
        CajaDto dto = gson.fromJson(CLOSED_CAJA_JSON, CajaDto.class);

        assertNotNull(dto);
        assertEquals(2, dto.getId());
        assertEquals("cajero1", dto.getUsuarioAperturaNombre());
        assertEquals("cajero2", dto.getUsuarioCierreNombre());
        assertEquals("2026-06-18T08:00:00Z", dto.getFechaApertura());
        assertEquals("2026-06-18T20:00:00Z", dto.getFechaCierre());
        assertEquals(5000.0, dto.getMontoApertura(), 0.0001);
        assertEquals(Double.valueOf(25000.0), dto.getMontoCierreTeorico());
        assertEquals(Double.valueOf(24850.0), dto.getMontoCierreReal());
        assertEquals("cerrada", dto.getEstado());
    }

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        CajaDto dto = new CajaDto();
        dto.setId(7);
        dto.setUsuarioAperturaNombre("ana");
        dto.setUsuarioCierreNombre("luis");
        dto.setFechaApertura("2026-06-20T09:00:00Z");
        dto.setFechaCierre("2026-06-20T18:00:00Z");
        dto.setMontoApertura(1000.0);
        dto.setMontoCierreTeorico(2000.0);
        dto.setMontoCierreReal(1995.0);
        dto.setEstado("cerrada");

        String json = gson.toJson(dto);

        assertTrue("json must contain 'id', got: " + json, json.contains("\"id\""));
        assertTrue("json must contain 'usuarioAperturaNombre', got: " + json, json.contains("\"usuarioAperturaNombre\""));
        assertTrue("json must contain 'usuarioCierreNombre', got: " + json, json.contains("\"usuarioCierreNombre\""));
        assertTrue("json must contain 'fechaApertura', got: " + json, json.contains("\"fechaApertura\""));
        assertTrue("json must contain 'fechaCierre', got: " + json, json.contains("\"fechaCierre\""));
        assertTrue("json must contain 'montoApertura', got: " + json, json.contains("\"montoApertura\""));
        assertTrue("json must contain 'montoCierreTeorico', got: " + json, json.contains("\"montoCierreTeorico\""));
        assertTrue("json must contain 'montoCierreReal', got: " + json, json.contains("\"montoCierreReal\""));
        assertTrue("json must contain 'estado', got: " + json, json.contains("\"estado\""));
        // v2 contract: user IDs MUST NOT be present.
        assertTrue("json must NOT contain 'usuarioAperturaId' (removed in v2), got: " + json,
                !json.contains("\"usuarioAperturaId\""));
        assertTrue("json must NOT contain 'usuarioCierreId' (removed in v2), got: " + json,
                !json.contains("\"usuarioCierreId\""));
    }

    @Test
    public void roundTripsClosedCajaViaSerializeThenDeserialize() {
        CajaDto original = new CajaDto();
        original.setId(11);
        original.setUsuarioAperturaNombre("sofia");
        original.setUsuarioCierreNombre("pablo");
        original.setFechaApertura("2026-06-19T08:00:00Z");
        original.setFechaCierre("2026-06-19T20:00:00Z");
        original.setMontoApertura(7500.0);
        original.setMontoCierreTeorico(30000.0);
        original.setMontoCierreReal(29990.0);
        original.setEstado("cerrada");

        String json = gson.toJson(original);
        CajaDto parsed = gson.fromJson(json, CajaDto.class);

        assertNotNull(parsed);
        assertEquals(11, parsed.getId());
        assertEquals("sofia", parsed.getUsuarioAperturaNombre());
        assertEquals("pablo", parsed.getUsuarioCierreNombre());
        assertEquals("2026-06-19T08:00:00Z", parsed.getFechaApertura());
        assertEquals("2026-06-19T20:00:00Z", parsed.getFechaCierre());
        assertEquals(7500.0, parsed.getMontoApertura(), 0.0001);
        assertEquals(Double.valueOf(30000.0), parsed.getMontoCierreTeorico());
        assertEquals(Double.valueOf(29990.0), parsed.getMontoCierreReal());
        assertEquals("cerrada", parsed.getEstado());
    }

    @Test
    public void boxedCierreFieldsAreOmittedFromJsonWhenNull() {
        CajaDto dto = new CajaDto();
        dto.setId(1);
        dto.setUsuarioAperturaNombre("cajero1");
        dto.setFechaApertura("2026-06-18T08:00:00Z");
        dto.setMontoApertura(5000.0);
        dto.setEstado("abierta");
        // usuarioCierreNombre, fechaCierre,
        // montoCierreTeorico, montoCierreReal stay null

        String json = gson.toJson(dto);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertTrue("usuarioCierreNombre must be omitted when null, got: " + json,
                !obj.has("usuarioCierreNombre"));
        assertTrue("fechaCierre must be omitted when null, got: " + json,
                !obj.has("fechaCierre"));
        assertTrue("montoCierreTeorico must be omitted when null, got: " + json,
                !obj.has("montoCierreTeorico"));
        assertTrue("montoCierreReal must be omitted when null, got: " + json,
                !obj.has("montoCierreReal"));
    }

    @Test
    public void defaultConstructorsLeaveFieldsAtDefaults() {
        CajaDto dto = new CajaDto();

        assertEquals(0, dto.getId());
        assertNull(dto.getUsuarioAperturaNombre());
        assertNull(dto.getUsuarioCierreNombre());
        assertNull(dto.getFechaApertura());
        assertNull(dto.getFechaCierre());
        assertEquals(0.0, dto.getMontoApertura(), 0.0001);
        assertNull(dto.getMontoCierreTeorico());
        assertNull(dto.getMontoCierreReal());
        assertNull(dto.getEstado());
    }

    @Test
    public void gettersReturnSetterValues() {
        CajaDto dto = new CajaDto();
        dto.setId(99);
        dto.setUsuarioAperturaNombre("u10");
        dto.setUsuarioCierreNombre("u20");
        dto.setFechaApertura("2026-06-20T08:00:00Z");
        dto.setFechaCierre("2026-06-20T20:00:00Z");
        dto.setMontoApertura(1234.5);
        dto.setMontoCierreTeorico(5678.9);
        dto.setMontoCierreReal(5670.0);
        dto.setEstado("cerrada");

        assertEquals(99, dto.getId());
        assertEquals("u10", dto.getUsuarioAperturaNombre());
        assertEquals("u20", dto.getUsuarioCierreNombre());
        assertEquals("2026-06-20T08:00:00Z", dto.getFechaApertura());
        assertEquals("2026-06-20T20:00:00Z", dto.getFechaCierre());
        assertEquals(1234.5, dto.getMontoApertura(), 0.0001);
        assertEquals(Double.valueOf(5678.9), dto.getMontoCierreTeorico());
        assertEquals(Double.valueOf(5670.0), dto.getMontoCierreReal());
        assertEquals("cerrada", dto.getEstado());
    }
}

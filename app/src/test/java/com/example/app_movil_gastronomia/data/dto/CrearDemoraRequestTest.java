package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.demora.CrearDemoraRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec DEM-DTO-001 (v2): the request body for {@code POST /api/demoras} must
 * serialize to a JSON object with exactly the keys {@code pedidoId},
 * {@code demoraMinutos} and {@code observaciones} — the {@code sector}
 * field was removed in the v2 backend contract. The server derives the
 * sector from the auth token / business rules.
 */
public class CrearDemoraRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        CrearDemoraRequest request = new CrearDemoraRequest(7, 15, "sin papas");

        String json = gson.toJson(request);

        // Round-trip: parse back and verify equality of every field.
        CrearDemoraRequest parsed = gson.fromJson(json, CrearDemoraRequest.class);
        assertEquals(7, parsed.getPedidoId());
        assertEquals(15, parsed.getDemoraMinutos());
        assertEquals("sin papas", parsed.getObservaciones());

        // String-level: the three required keys MUST be present.
        assertTrue("json must contain 'pedidoId', got: " + json, json.contains("\"pedidoId\""));
        assertTrue("json must contain 'demoraMinutos', got: " + json, json.contains("\"demoraMinutos\""));
        assertTrue("json must contain 'observaciones', got: " + json, json.contains("\"observaciones\""));
        // v2 contract: sector MUST NOT be present.
        assertTrue("json must NOT contain 'sector' (removed in v2), got: " + json,
                !json.contains("\"sector\""));
    }

    @Test
    public void gettersReturnConstructorValues() {
        CrearDemoraRequest request = new CrearDemoraRequest(20, 45, "urgente");

        assertEquals(20, request.getPedidoId());
        assertEquals(45, request.getDemoraMinutos());
        assertEquals("urgente", request.getObservaciones());
    }

    @Test
    public void settersUpdateFieldValues() {
        CrearDemoraRequest request = new CrearDemoraRequest(1, 1, "x");
        request.setPedidoId(99);
        request.setDemoraMinutos(120);
        request.setObservaciones("rehacer");

        assertEquals(99, request.getPedidoId());
        assertEquals(120, request.getDemoraMinutos());
        assertEquals("rehacer", request.getObservaciones());
    }
}

package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec DEM-DTO-001: the request body for {@code POST /api/demoras} must
 * serialize to a JSON object with exactly the keys {@code pedidoId},
 * {@code demoraMinutos}, {@code sector}, {@code observaciones} — matching
 * the server's contract. All four fields are required, so they are kept
 * as primitives or {@code String} (no boxing).
 */
public class CrearDemoraRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        CrearDemoraRequest request = new CrearDemoraRequest(7, 15, "cocina", "sin papas");

        String json = gson.toJson(request);

        // Round-trip: parse back and verify equality of every field.
        CrearDemoraRequest parsed = gson.fromJson(json, CrearDemoraRequest.class);
        assertEquals(7, parsed.getPedidoId());
        assertEquals(15, parsed.getDemoraMinutos());
        assertEquals("cocina", parsed.getSector());
        assertEquals("sin papas", parsed.getObservaciones());

        // String-level: the four required keys MUST be present.
        assertTrue("json must contain 'pedidoId', got: " + json, json.contains("\"pedidoId\""));
        assertTrue("json must contain 'demoraMinutos', got: " + json, json.contains("\"demoraMinutos\""));
        assertTrue("json must contain 'sector', got: " + json, json.contains("\"sector\""));
        assertTrue("json must contain 'observaciones', got: " + json, json.contains("\"observaciones\""));
    }

    @Test
    public void gettersReturnConstructorValues() {
        CrearDemoraRequest request = new CrearDemoraRequest(20, 45, "barra", "urgente");

        assertEquals(20, request.getPedidoId());
        assertEquals(45, request.getDemoraMinutos());
        assertEquals("barra", request.getSector());
        assertEquals("urgente", request.getObservaciones());
    }

    @Test
    public void settersUpdateFieldValues() {
        CrearDemoraRequest request = new CrearDemoraRequest(1, 1, "x", "x");
        request.setPedidoId(99);
        request.setDemoraMinutos(120);
        request.setSector("cocina");
        request.setObservaciones("rehacer");

        assertEquals(99, request.getPedidoId());
        assertEquals(120, request.getDemoraMinutos());
        assertEquals("cocina", request.getSector());
        assertEquals("rehacer", request.getObservaciones());
    }
}

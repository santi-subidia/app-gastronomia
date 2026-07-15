package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec PROD-CRUD-005: the request body for POST /api/productos must serialize
 * to a JSON object with exactly the keys {@code nombre}, {@code precio},
 * {@code demora} — matching the server's contract.
 */
public class CrearProductoRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        CrearProductoRequest request = new CrearProductoRequest("Milanesa", 2500.0, 15);

        String json = gson.toJson(request);

        // Order-independent check: parse back and verify equality of every key.
        CrearProductoRequest parsed = gson.fromJson(json, CrearProductoRequest.class);
        assertEquals("Milanesa", parsed.getNombre());
        assertEquals(2500.0, parsed.getPrecio(), 0.0001);
        assertEquals(15, parsed.getDemora());

        // String-level: the three required keys MUST be present.
        assertTrue("json must contain 'nombre', got: " + json, json.contains("\"nombre\""));
        assertTrue("json must contain 'precio', got: " + json, json.contains("\"precio\""));
        assertTrue("json must contain 'demora', got: " + json, json.contains("\"demora\""));
    }

    @Test
    public void gettersReturnConstructorValues() {
        CrearProductoRequest request = new CrearProductoRequest("Empanada", 800.0, 10);

        assertEquals("Empanada", request.getNombre());
        assertEquals(800.0, request.getPrecio(), 0.0001);
        assertEquals(10, request.getDemora());
    }
}

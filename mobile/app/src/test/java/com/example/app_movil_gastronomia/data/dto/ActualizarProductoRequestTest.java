package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec PROD-CRUD-005: the request body for PUT /api/productos/{id} is a
 * <b>partial update</b>. Only the fields the caller explicitly set must
 * be sent. Gson drops {@code null} boxed fields by default — these tests
 * lock that contract in.
 */
public class ActualizarProductoRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesOnlySetFieldsAndOmitsNulls() {
        // Only the price is being changed.
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setPrecio(2000.0);

        String json = gson.toJson(request);

        assertTrue("json must contain 'precio', got: " + json, json.contains("\"precio\""));
        Double parsedPrecio = gson.fromJson(json, ActualizarProductoRequest.class).getPrecio();
        assertEquals(Double.valueOf(2000.0), parsedPrecio);
    }

    @Test
    public void nullNombreAndDemoraAreOmittedFromJson() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setPrecio(2000.0);
        // nombre and demora stay null.

        String json = gson.toJson(request);

        assertFalse("json must NOT contain 'nombre' when null, got: " + json, json.contains("\"nombre\""));
        assertFalse("json must NOT contain 'demora' when null, got: " + json, json.contains("\"demora\""));
    }

    @Test
    public void nullPrecioIsOmittedFromJson() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setNombre("Pizza");
        request.setDemora(20);
        // precio stays null.

        String json = gson.toJson(request);

        assertTrue("json must contain 'nombre', got: " + json, json.contains("\"nombre\""));
        assertTrue("json must contain 'demora', got: " + json, json.contains("\"demora\""));
        assertFalse("json must NOT contain 'precio' when null, got: " + json, json.contains("\"precio\""));
    }

    @Test
    public void allFieldsNullProducesEmptyObject() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();

        String json = gson.toJson(request);

        assertEquals("{}", json);
    }

    @Test
    public void allFieldsSetSerializesAllKeys() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setNombre("Pizza");
        request.setPrecio(3500.0);
        request.setDemora(25);

        String json = gson.toJson(request);

        assertTrue(json.contains("\"nombre\""));
        assertTrue(json.contains("\"precio\""));
        assertTrue(json.contains("\"demora\""));
    }
}

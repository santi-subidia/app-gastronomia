package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec DEM-DTO-001: the request body for {@code PUT /api/demoras/{id}} is
 * a <b>partial update</b>. Only the fields the caller explicitly set must
 * be sent. Gson drops {@code null} boxed fields by default — these tests
 * lock that contract in.
 *
 * <p>All three fields ({@code demoraMinutos}, {@code sector},
 * {@code observaciones}) are nullable; {@code demoraMinutos} is the only
 * one whose omission matters server-side (a primitive {@code int} would
 * default to 0 and wrongly overwrite the stored value).
 */
public class ActualizarDemoraRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesOnlySetFieldsAndOmitsNulls() {
        // Only the demoraMinutos is being changed.
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(30);

        String json = gson.toJson(request);

        assertTrue("json must contain 'demoraMinutos', got: " + json, json.contains("\"demoraMinutos\""));
        Integer parsedDemora = gson.fromJson(json, ActualizarDemoraRequest.class).getDemoraMinutos();
        assertEquals(Integer.valueOf(30), parsedDemora);
    }

    @Test
    public void nullSectorAndObservacionesAreOmittedFromJson() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(30);
        // sector and observaciones stay null.

        String json = gson.toJson(request);

        assertFalse("json must NOT contain 'sector' when null, got: " + json, json.contains("\"sector\""));
        assertFalse("json must NOT contain 'observaciones' when null, got: " + json, json.contains("\"observaciones\""));
    }

    @Test
    public void nullDemoraMinutosIsOmittedFromJson() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setSector("cocina");
        request.setObservaciones("rehacer");
        // demoraMinutos stays null — this is the key safety property: a
        // primitive int would serialize as 0 and zero out the stored value.

        String json = gson.toJson(request);

        assertTrue("json must contain 'sector', got: " + json, json.contains("\"sector\""));
        assertTrue("json must contain 'observaciones', got: " + json, json.contains("\"observaciones\""));
        assertFalse("json must NOT contain 'demoraMinutos' when null, got: " + json, json.contains("\"demoraMinutos\""));
    }

    @Test
    public void allFieldsNullProducesEmptyObject() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();

        String json = gson.toJson(request);

        assertEquals("{}", json);
    }

    @Test
    public void allFieldsSetSerializesAllKeys() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(45);
        request.setSector("barra");
        request.setObservaciones("urgente");

        String json = gson.toJson(request);

        assertTrue(json.contains("\"demoraMinutos\""));
        assertTrue(json.contains("\"sector\""));
        assertTrue(json.contains("\"observaciones\""));
    }

    @Test
    public void settersUpdateFieldValues() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(10);
        request.setSector("cocina");
        request.setObservaciones("primera version");

        // Reset one to null and the others to new values
        request.setDemoraMinutos(null);
        request.setSector("barra");
        request.setObservaciones("segunda version");

        assertEquals(null, request.getDemoraMinutos());
        assertEquals("barra", request.getSector());
        assertEquals("segunda version", request.getObservaciones());
    }
}

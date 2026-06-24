package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.demora.ActualizarDemoraRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec DEM-DTO-001 (v2): the request body for {@code PUT /api/demoras/{id}} is
 * a <b>partial update</b>. Only the fields the caller explicitly set must
 * be sent. Gson drops {@code null} boxed fields by default — these tests
 * lock that contract in.
 *
 * <p>v2 contract: only two nullable fields remain ({@code demoraMinutos},
 * {@code observaciones}). The {@code sector} field was removed.</p>
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
    public void nullObservacionesIsOmittedFromJson() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(30);
        // observaciones stays null.

        String json = gson.toJson(request);

        assertFalse("json must NOT contain 'observaciones' when null, got: " + json,
                json.contains("\"observaciones\""));
        // v2 contract: sector MUST NOT be present.
        assertFalse("json must NOT contain 'sector' (removed in v2), got: " + json,
                json.contains("\"sector\""));
    }

    @Test
    public void nullDemoraMinutosIsOmittedFromJson() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setObservaciones("rehacer");
        // demoraMinutos stays null — this is the key safety property: a
        // primitive int would serialize as 0 and zero out the stored value.

        String json = gson.toJson(request);

        assertTrue("json must contain 'observaciones', got: " + json, json.contains("\"observaciones\""));
        assertFalse("json must NOT contain 'demoraMinutos' when null, got: " + json,
                json.contains("\"demoraMinutos\""));
        // v2 contract: sector MUST NOT be present.
        assertFalse("json must NOT contain 'sector' (removed in v2), got: " + json,
                json.contains("\"sector\""));
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
        request.setObservaciones("urgente");

        String json = gson.toJson(request);

        assertTrue(json.contains("\"demoraMinutos\""));
        assertTrue(json.contains("\"observaciones\""));
        // v2 contract: sector MUST NOT be present.
        assertTrue("json must NOT contain 'sector' (removed in v2), got: " + json,
                !json.contains("\"sector\""));
    }

    @Test
    public void settersUpdateFieldValues() {
        ActualizarDemoraRequest request = new ActualizarDemoraRequest();
        request.setDemoraMinutos(10);
        request.setObservaciones("primera version");

        // Reset one to null and the other to a new value
        request.setDemoraMinutos(null);
        request.setObservaciones("segunda version");

        assertEquals(null, request.getDemoraMinutos());
        assertEquals("segunda version", request.getObservaciones());
    }
}

package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec CAJ-DTO-001 (v2): the request body for
 * {@code POST /api/cajas/apertura} must serialize to a JSON
 * object with exactly the key {@code montoApertura}, matching
 * the v2 server contract in {@code doc/API_REFERENCIA.md} §3.4.
 * The {@code usuarioAperturaId} field was removed — the server
 * derives the user from the auth token.
 */
public class AbrirCajaRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesOnlyMontoWithExpectedKeys() {
        AbrirCajaRequest request = new AbrirCajaRequest(5000.0);

        String json = gson.toJson(request);

        AbrirCajaRequest parsed = gson.fromJson(json, AbrirCajaRequest.class);
        assertEquals(5000.0, parsed.getMontoApertura(), 0.0001);

        assertTrue("json must contain 'montoApertura', got: " + json,
                json.contains("\"montoApertura\""));
        // v2 contract: usuarioAperturaId MUST NOT be present.
        assertTrue("json must NOT contain 'usuarioAperturaId' (removed in v2), got: " + json,
                !json.contains("\"usuarioAperturaId\""));
    }

    @Test
    public void roundTripsSampleJsonFromApiReference() {
        // Exact body from doc/API_REFERENCIA.md §3.4 POST /api/cajas/apertura (v2)
        String sample = "{"
                + "\"montoApertura\": 5000.00"
                + "}";

        AbrirCajaRequest parsed = gson.fromJson(sample, AbrirCajaRequest.class);

        assertNotNull(parsed);
        assertEquals(5000.0, parsed.getMontoApertura(), 0.0001);
    }

    @Test
    public void gettersReturnConstructorValues() {
        AbrirCajaRequest request = new AbrirCajaRequest(1234.56);

        assertEquals(1234.56, request.getMontoApertura(), 0.0001);
    }

    @Test
    public void settersUpdateFieldValues() {
        AbrirCajaRequest request = new AbrirCajaRequest(100.0);
        request.setMontoApertura(200.0);

        assertEquals(200.0, request.getMontoApertura(), 0.0001);
    }
}

package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec CAJ-DTO-001 (v2): the request body for
 * {@code POST /api/cajas/{id}/cierre} must serialize to a JSON
 * object with exactly the keys {@code montoCierreTeorico} and
 * {@code montoCierreReal}, matching the v2 server contract in
 * {@code doc/API_REFERENCIA.md} §3.4. The {@code usuarioCierreId}
 * field was removed — the server derives the user from the
 * auth token.
 */
public class CerrarCajaRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesOnlyMontoFieldsWithExpectedKeys() {
        CerrarCajaRequest request = new CerrarCajaRequest(25000.0, 24850.0);

        String json = gson.toJson(request);

        CerrarCajaRequest parsed = gson.fromJson(json, CerrarCajaRequest.class);
        assertEquals(25000.0, parsed.getMontoCierreTeorico(), 0.0001);
        assertEquals(24850.0, parsed.getMontoCierreReal(), 0.0001);

        assertTrue("json must contain 'montoCierreTeorico', got: " + json,
                json.contains("\"montoCierreTeorico\""));
        assertTrue("json must contain 'montoCierreReal', got: " + json,
                json.contains("\"montoCierreReal\""));
        assertTrue("json must NOT contain 'usuarioCierreId' (removed in v2), got: " + json,
                !json.contains("\"usuarioCierreId\""));
    }

    @Test
    public void roundTripsSampleJsonFromApiReference() {
        String sample = "{"
                + "\"montoCierreTeorico\": 25000.00,"
                + "\"montoCierreReal\": 24850.00"
                + "}";

        CerrarCajaRequest parsed = gson.fromJson(sample, CerrarCajaRequest.class);

        assertNotNull(parsed);
        assertEquals(25000.0, parsed.getMontoCierreTeorico(), 0.0001);
        assertEquals(24850.0, parsed.getMontoCierreReal(), 0.0001);
    }

    @Test
    public void gettersReturnConstructorValues() {
        CerrarCajaRequest request = new CerrarCajaRequest(5000.0, 4950.0);

        assertEquals(5000.0, request.getMontoCierreTeorico(), 0.0001);
        assertEquals(4950.0, request.getMontoCierreReal(), 0.0001);
    }

    @Test
    public void settersUpdateFieldValues() {
        CerrarCajaRequest request = new CerrarCajaRequest(100.0, 100.0);
        request.setMontoCierreTeorico(200.0);
        request.setMontoCierreReal(195.0);

        assertEquals(200.0, request.getMontoCierreTeorico(), 0.0001);
        assertEquals(195.0, request.getMontoCierreReal(), 0.0001);
    }
}

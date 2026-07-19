package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec DEM-DTO-001 (v2): the response body for {@code GET/POST/PUT /api/demoras}
 * must serialize to/from a JSON object with exactly the keys
 * {@code id}, {@code pedidoId}, {@code usuarioId}, {@code demoraMinutos}
 * and {@code observaciones} — the {@code sector} field was removed in
 * the v2 backend contract.
 */
public class DemoraDtoTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        DemoraDto dto = new DemoraDto();
        dto.setId(42);
        dto.setPedidoId(7);
        dto.setUsuarioId(3);
        dto.setDemoraMinutos(15);
        dto.setObservaciones("sin papas");

        String json = gson.toJson(dto);

        DemoraDto parsed = gson.fromJson(json, DemoraDto.class);
        assertEquals(42, parsed.getId());
        assertEquals(7, parsed.getPedidoId());
        assertEquals(3, parsed.getUsuarioId());
        assertEquals(15, parsed.getDemoraMinutos());
        assertEquals("sin papas", parsed.getObservaciones());

        assertTrue("json must contain 'id', got: " + json, json.contains("\"id\""));
        assertTrue("json must contain 'pedidoId', got: " + json, json.contains("\"pedidoId\""));
        assertTrue("json must contain 'usuarioId', got: " + json, json.contains("\"usuarioId\""));
        assertTrue("json must contain 'demoraMinutos', got: " + json, json.contains("\"demoraMinutos\""));
        assertTrue("json must contain 'observaciones', got: " + json, json.contains("\"observaciones\""));
        assertTrue("json must NOT contain 'sector' (removed in v2), got: " + json,
                !json.contains("\"sector\""));
    }

    @Test
    public void gettersReturnSetterValues() {
        DemoraDto dto = new DemoraDto();
        dto.setId(1);
        dto.setPedidoId(2);
        dto.setUsuarioId(3);
        dto.setDemoraMinutos(20);
        dto.setObservaciones("urgente");

        assertEquals(1, dto.getId());
        assertEquals(2, dto.getPedidoId());
        assertEquals(3, dto.getUsuarioId());
        assertEquals(20, dto.getDemoraMinutos());
        assertEquals("urgente", dto.getObservaciones());
    }

    @Test
    public void deserializesFromServerJsonShape() {
        String json = "{"
                + "\"id\":10,"
                + "\"pedidoId\":20,"
                + "\"usuarioId\":30,"
                + "\"demoraMinutos\":45,"
                + "\"observaciones\":\"esperar\""
                + "}";

        DemoraDto parsed = gson.fromJson(json, DemoraDto.class);

        assertEquals(10, parsed.getId());
        assertEquals(20, parsed.getPedidoId());
        assertEquals(30, parsed.getUsuarioId());
        assertEquals(45, parsed.getDemoraMinutos());
        assertEquals("esperar", parsed.getObservaciones());
    }
}

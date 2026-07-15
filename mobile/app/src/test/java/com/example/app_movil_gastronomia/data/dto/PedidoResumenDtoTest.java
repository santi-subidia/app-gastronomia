package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec PED-DTO-001: {@link PedidoResumenDto} deserializes from the
 * {@code GET /api/pedidos} response shape — a JSON object with the keys
 * {@code id}, {@code estado}, {@code clienteNombre}, {@code metodoVenta},
 * {@code totalEstimado}, {@code fechaIngreso}. Sample JSON is taken
 * directly from {@code doc/API_REFERENCIA.md}.
 */
public class PedidoResumenDtoTest {

    private final Gson gson = new Gson();

    private static final String SAMPLE_JSON = "{"
            + "\"id\": 1,"
            + "\"estado\": \"Pendiente\","
            + "\"clienteNombre\": \"Juan Pérez\","
            + "\"metodoVenta\": \"Delivery\","
            + "\"totalEstimado\": 15000,"
            + "\"fechaIngreso\": \"2026-06-18T14:30:00Z\""
            + "}";

    @Test
    public void deserializesAllFieldsFromSampleJson() {
        PedidoResumenDto dto = gson.fromJson(SAMPLE_JSON, PedidoResumenDto.class);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("Pendiente", dto.getEstado());
        assertEquals("Juan Pérez", dto.getClienteNombre());
        assertEquals("Delivery", dto.getMetodoVenta());
        assertEquals(15000.0, dto.getTotalEstimado(), 0.0001);
        assertEquals("2026-06-18T14:30:00Z", dto.getFechaIngreso());
    }

    @Test
    public void serializesAllFieldsWithExpectedKeys() {
        PedidoResumenDto dto = new PedidoResumenDto();
        dto.setId(7);
        dto.setEstado("EnCamino");
        dto.setClienteNombre("María");
        dto.setMetodoVenta("Retiro en local");
        dto.setTotalEstimado(9999.50);
        dto.setFechaIngreso("2026-06-19T10:00:00Z");

        String json = gson.toJson(dto);

        assertTrue("json must contain 'id', got: " + json, json.contains("\"id\""));
        assertTrue("json must contain 'estado', got: " + json, json.contains("\"estado\""));
        assertTrue("json must contain 'clienteNombre', got: " + json, json.contains("\"clienteNombre\""));
        assertTrue("json must contain 'metodoVenta', got: " + json, json.contains("\"metodoVenta\""));
        assertTrue("json must contain 'totalEstimado', got: " + json, json.contains("\"totalEstimado\""));
        assertTrue("json must contain 'fechaIngreso', got: " + json, json.contains("\"fechaIngreso\""));
    }

    @Test
    public void defaultConstructorsLeaveFieldsAtDefaults() {
        PedidoResumenDto dto = new PedidoResumenDto();

        assertEquals(0, dto.getId());
        assertNull(dto.getEstado());
        assertNull(dto.getClienteNombre());
        assertNull(dto.getMetodoVenta());
        assertEquals(0.0, dto.getTotalEstimado(), 0.0001);
        assertNull(dto.getFechaIngreso());
    }

    @Test
    public void gettersReturnSetterValues() {
        PedidoResumenDto dto = new PedidoResumenDto();
        dto.setId(42);
        dto.setEstado("Entregado");
        dto.setClienteNombre("Carlos");
        dto.setMetodoVenta("Delivery");
        dto.setTotalEstimado(2500.0);
        dto.setFechaIngreso("2026-06-20T08:00:00Z");

        assertEquals(42, dto.getId());
        assertEquals("Entregado", dto.getEstado());
        assertEquals("Carlos", dto.getClienteNombre());
        assertEquals("Delivery", dto.getMetodoVenta());
        assertEquals(2500.0, dto.getTotalEstimado(), 0.0001);
        assertEquals("2026-06-20T08:00:00Z", dto.getFechaIngreso());
    }
}

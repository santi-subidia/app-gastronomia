package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.pedido.DetallePedidoDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.List;

/**
 * Spec PED-DTO-001: {@link PedidoDetalleDto} deserializes from the
 * {@code GET /api/pedidos/{id}} response shape. Fields marked nullable
 * in the spec (boxed types) must stay null when the JSON has null;
 * non-nullable scalars use primitives. The nested
 * {@code detallePedidos} array must round-trip into a
 * {@link List} of {@link DetallePedidoDto}.
 */
public class PedidoDetalleDtoTest {

    private final Gson gson = new Gson();

    private static final String SAMPLE_JSON = "{"
            + "\"id\": 1,"
            + "\"estado\": \"EnPreparacion\","
            + "\"clienteNombre\": \"Juan Pérez\","
            + "\"clienteDireccion\": \"Av. Siempre Viva 742\","
            + "\"metodoVenta\": \"Delivery\","
            + "\"metodoPago\": \"Efectivo\","
            + "\"totalEstimado\": 15000,"
            + "\"demoraAprox\": 25,"
            + "\"latitudDestino\": -34.6037,"
            + "\"longitudDestino\": -58.3816,"
            + "\"fechaIngreso\": \"2026-06-18T14:30:00Z\","
            + "\"fechaEstimadoFin\": \"2026-06-18T14:55:00Z\","
            + "\"fechaAsignado\": null,"
            + "\"fechaEnCamino\": null,"
            + "\"fechaFinalizado\": null,"
            + "\"repartidorNombre\": null,"
            + "\"cajaId\": 1,"
            + "\"estadoId\": 2,"
            + "\"detallePedidos\": ["
            + "  {"
            + "    \"productoId\": 1,"
            + "    \"nombre\": \"Milanesa con Papas Fritas\","
            + "    \"cantidad\": 2,"
            + "    \"precio\": 8500,"
            + "    \"tiempoMaquina\": 25"
            + "  }"
            + "]"
            + "}";

    @Test
    public void deserializesAllNonNullableFields() {
        PedidoDetalleDto dto = gson.fromJson(SAMPLE_JSON, PedidoDetalleDto.class);

        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("EnPreparacion", dto.getEstado());
        assertEquals("Juan Pérez", dto.getClienteNombre());
        assertEquals("Av. Siempre Viva 742", dto.getClienteDireccion());
        assertEquals("Delivery", dto.getMetodoVenta());
        assertEquals("Efectivo", dto.getMetodoPago());
        assertEquals(15000.0, dto.getTotalEstimado(), 0.0001);
        assertEquals(2, dto.getEstadoId());
        assertEquals("2026-06-18T14:30:00Z", dto.getFechaIngreso());
    }

    @Test
    public void deserializesBoxedNullableFields() {
        PedidoDetalleDto dto = gson.fromJson(SAMPLE_JSON, PedidoDetalleDto.class);

        assertEquals(Integer.valueOf(25), dto.getDemoraAprox());
        assertEquals(Double.valueOf(-34.6037), dto.getLatitudDestino());
        assertEquals(Double.valueOf(-58.3816), dto.getLongitudDestino());
        assertEquals("2026-06-18T14:55:00Z", dto.getFechaEstimadoFin());
        assertEquals(Integer.valueOf(1), dto.getCajaId());
    }

    @Test
    public void deserializesNullableNullsAsNull() {
        PedidoDetalleDto dto = gson.fromJson(SAMPLE_JSON, PedidoDetalleDto.class);

        assertNull("fechaAsignado must be null when JSON is null", dto.getFechaAsignado());
        assertNull("fechaEnCamino must be null when JSON is null", dto.getFechaEnCamino());
        assertNull("fechaFinalizado must be null when JSON is null", dto.getFechaFinalizado());
        assertNull("repartidorNombre must be null when JSON is null", dto.getRepartidorNombre());
    }

    @Test
    public void deserializesNestedDetallePedidos() {
        PedidoDetalleDto dto = gson.fromJson(SAMPLE_JSON, PedidoDetalleDto.class);

        assertNotNull(dto.getDetallePedidos());
        assertEquals(1, dto.getDetallePedidos().size());

        DetallePedidoDto detalle = dto.getDetallePedidos().get(0);
        assertEquals(1, detalle.getProductoId());
        assertEquals("Milanesa con Papas Fritas", detalle.getNombre());
        assertEquals(2, detalle.getCantidad());
        assertEquals(8500.0, detalle.getPrecio(), 0.0001);
        assertEquals(25, detalle.getTiempoMaquina());
    }

    @Test
    public void serializesAllKeys() {
        PedidoDetalleDto dto = new PedidoDetalleDto();
        dto.setId(1);
        dto.setEstado("Pendiente");
        dto.setClienteNombre("Ana");
        dto.setClienteDireccion("Calle 1");
        dto.setMetodoVenta("Delivery");
        dto.setMetodoPago("Efectivo");
        dto.setTotalEstimado(500.0);
        dto.setEstadoId(1);
        dto.setFechaIngreso("2026-06-20T00:00:00Z");
        dto.setCajaId(1);
        dto.setDetallePedidos(new java.util.ArrayList<DetallePedidoDto>());

        String json = gson.toJson(dto);

        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("\"estado\""));
        assertTrue(json.contains("\"clienteNombre\""));
        assertTrue(json.contains("\"clienteDireccion\""));
        assertTrue(json.contains("\"metodoVenta\""));
        assertTrue(json.contains("\"metodoPago\""));
        assertTrue(json.contains("\"totalEstimado\""));
        assertTrue(json.contains("\"estadoId\""));
        assertTrue(json.contains("\"fechaIngreso\""));
        assertTrue(json.contains("\"cajaId\""));
        assertTrue(json.contains("\"detallePedidos\""));
    }

    @Test
    public void nullBoxedFieldsAreOmittedFromJson() {
        // The boxed nullables (demoraAprox, latitudDestino, etc.) must be
        // omitted by Gson when null — that matches the partial-update
        // convention already used by ActualizarProductoRequest.
        PedidoDetalleDto dto = new PedidoDetalleDto();
        dto.setId(1);
        dto.setEstado("Pendiente");
        dto.setClienteNombre("Ana");
        dto.setClienteDireccion("Calle 1");
        dto.setMetodoVenta("Delivery");
        dto.setMetodoPago("Efectivo");
        dto.setTotalEstimado(500.0);
        dto.setEstadoId(1);
        dto.setFechaIngreso("2026-06-20T00:00:00Z");

        String json = gson.toJson(dto);

        assertTrue("demoraAprox must be omitted when null, got: " + json,
                !json.contains("\"demoraAprox\""));
        assertTrue("latitudDestino must be omitted when null, got: " + json,
                !json.contains("\"latitudDestino\""));
        assertTrue("longitudDestino must be omitted when null, got: " + json,
                !json.contains("\"longitudDestino\""));
        assertTrue("repartidorNombre must be omitted when null, got: " + json,
                !json.contains("\"repartidorNombre\""));
    }

    @Test
    public void defaultConstructorsLeaveFieldsAtDefaults() {
        PedidoDetalleDto dto = new PedidoDetalleDto();

        assertEquals(0, dto.getId());
        assertNull(dto.getEstado());
        assertNull(dto.getClienteNombre());
        assertNull(dto.getClienteDireccion());
        assertNull(dto.getMetodoVenta());
        assertNull(dto.getMetodoPago());
        assertEquals(0.0, dto.getTotalEstimado(), 0.0001);
        assertEquals(0, dto.getEstadoId());
        assertNull(dto.getCajaId());
        assertNull(dto.getRepartidorNombre());
        assertNull(dto.getLatitudDestino());
        assertNull(dto.getLongitudDestino());
        assertNull(dto.getDemoraAprox());
        assertNull(dto.getFechaIngreso());
        assertNull(dto.getFechaEstimadoFin());
        assertNull(dto.getFechaAsignado());
        assertNull(dto.getFechaEnCamino());
        assertNull(dto.getFechaFinalizado());
        assertNull(dto.getDetallePedidos());
    }
}

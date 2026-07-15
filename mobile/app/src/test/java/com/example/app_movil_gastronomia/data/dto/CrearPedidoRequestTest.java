package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.pedido.CrearDetalleRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Spec PED-DTO-001 / PED-CRUD-001: the request body for
 * {@code POST /api/pedidos} must serialize to a JSON object with the
 * keys required by the backend. Nullable fields (cajaId,
 * latitudDestino, longitudDestino, demoraAprox) are boxed so Gson
 * omits them when null and accepts null on the way back.
 */
public class CrearPedidoRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesAllRequiredFieldsWithExpectedKeys() {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setCajaId(1);
        request.setMetodoPagoId(1);
        request.setMetodoVentaId(1);
        request.setClienteNombre("Juan Pérez");
        request.setClienteDireccion("Av. Siempre Viva 742");
        request.setLatitudDestino(-34.6037);
        request.setLongitudDestino(-58.3816);
        request.setTotalEstimado(15000.0);
        request.setDemoraAprox(25);
        request.setDetalles(Arrays.<CrearDetalleRequest>asList(
                new CrearDetalleRequest(1, "Milanesa con Papas Fritas", 8500.0, 2)
        ));

        String json = gson.toJson(request);

        assertTrue("json must contain 'cajaId', got: " + json, json.contains("\"cajaId\""));
        assertTrue("json must contain 'metodoPagoId', got: " + json, json.contains("\"metodoPagoId\""));
        assertTrue("json must contain 'metodoVentaId', got: " + json, json.contains("\"metodoVentaId\""));
        assertTrue("json must contain 'clienteNombre', got: " + json, json.contains("\"clienteNombre\""));
        assertTrue("json must contain 'clienteDireccion', got: " + json, json.contains("\"clienteDireccion\""));
        assertTrue("json must contain 'latitudDestino', got: " + json, json.contains("\"latitudDestino\""));
        assertTrue("json must contain 'longitudDestino', got: " + json, json.contains("\"longitudDestino\""));
        assertTrue("json must contain 'totalEstimado', got: " + json, json.contains("\"totalEstimado\""));
        assertTrue("json must contain 'demoraAprox', got: " + json, json.contains("\"demoraAprox\""));
        assertTrue("json must contain 'detalles', got: " + json, json.contains("\"detalles\""));
    }

    @Test
    public void roundTripsSampleJsonFromApiReference() {
        // Exact body from doc/API_REFERENCIA.md §3.2 POST /api/pedidos
        String sample = "{"
                + "\"cajaId\": 1,"
                + "\"metodoPagoId\": 1,"
                + "\"metodoVentaId\": 1,"
                + "\"clienteNombre\": \"Juan Pérez\","
                + "\"clienteDireccion\": \"Av. Siempre Viva 742\","
                + "\"latitudDestino\": -34.6037,"
                + "\"longitudDestino\": -58.3816,"
                + "\"totalEstimado\": 15000,"
                + "\"demoraAprox\": 25,"
                + "\"detalles\": ["
                + "  {"
                + "    \"productoId\": 1,"
                + "    \"nombre\": \"Milanesa con Papas Fritas\","
                + "    \"precio\": 8500,"
                + "    \"cantidad\": 2"
                + "  }"
                + "]"
                + "}";

        CrearPedidoRequest parsed = gson.fromJson(sample, CrearPedidoRequest.class);

        assertNotNull(parsed);
        assertEquals(Integer.valueOf(1), parsed.getCajaId());
        assertEquals(1, parsed.getMetodoPagoId());
        assertEquals(1, parsed.getMetodoVentaId());
        assertEquals("Juan Pérez", parsed.getClienteNombre());
        assertEquals("Av. Siempre Viva 742", parsed.getClienteDireccion());
        assertEquals(Double.valueOf(-34.6037), parsed.getLatitudDestino());
        assertEquals(Double.valueOf(-58.3816), parsed.getLongitudDestino());
        assertEquals(15000.0, parsed.getTotalEstimado(), 0.0001);
        assertEquals(Integer.valueOf(25), parsed.getDemoraAprox());

        assertNotNull(parsed.getDetalles());
        assertEquals(1, parsed.getDetalles().size());
        CrearDetalleRequest detalle = parsed.getDetalles().get(0);
        assertEquals(1, detalle.getProductoId());
        assertEquals("Milanesa con Papas Fritas", detalle.getNombre());
        assertEquals(8500.0, detalle.getPrecio(), 0.0001);
        assertEquals(2, detalle.getCantidad());
    }

    @Test
    public void nullBoxedFieldsAreOmittedFromJson() {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setMetodoPagoId(2);
        request.setMetodoVentaId(2);
        request.setClienteNombre("Ana");
        request.setClienteDireccion("Calle 1");
        request.setTotalEstimado(500.0);
        // cajaId, latitudDestino, longitudDestino, demoraAprox stay null

        String json = gson.toJson(request);

        assertTrue("cajaId must be omitted when null, got: " + json,
                !json.contains("\"cajaId\""));
        assertTrue("latitudDestino must be omitted when null, got: " + json,
                !json.contains("\"latitudDestino\""));
        assertTrue("longitudDestino must be omitted when null, got: " + json,
                !json.contains("\"longitudDestino\""));
        assertTrue("demoraAprox must be omitted when null, got: " + json,
                !json.contains("\"demoraAprox\""));
    }

    @Test
    public void serializesNestedDetallesArray() {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setMetodoPagoId(1);
        request.setMetodoVentaId(2);
        request.setClienteNombre("Test");
        request.setClienteDireccion("Dir");
        request.setTotalEstimado(1000.0);
        request.setDetalles(Arrays.asList(
                new CrearDetalleRequest(1, "A", 100.0, 1),
                new CrearDetalleRequest(2, "B", 200.0, 2)
        ));

        String json = gson.toJson(request);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        com.google.gson.JsonArray detalles = obj.getAsJsonArray("detalles");

        assertNotNull(detalles);
        assertEquals(2, detalles.size());

        com.google.gson.JsonObject first = detalles.get(0).getAsJsonObject();
        assertEquals(1, first.get("productoId").getAsInt());
        assertEquals("A", first.get("nombre").getAsString());
        assertEquals(100.0, first.get("precio").getAsDouble(), 0.0001);
        assertEquals(1, first.get("cantidad").getAsInt());
    }

    @Test
    public void defaultConstructorsLeaveFieldsAtDefaults() {
        CrearPedidoRequest request = new CrearPedidoRequest();

        assertNull(request.getCajaId());
        assertEquals(0, request.getMetodoPagoId());
        assertEquals(0, request.getMetodoVentaId());
        assertNull(request.getClienteNombre());
        assertNull(request.getClienteDireccion());
        assertNull(request.getLatitudDestino());
        assertNull(request.getLongitudDestino());
        assertEquals(0.0, request.getTotalEstimado(), 0.0001);
        assertNull(request.getDemoraAprox());
        assertNull(request.getDetalles());
    }

    @Test
    public void gettersReturnSetterValues() {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setCajaId(3);
        request.setMetodoPagoId(2);
        request.setMetodoVentaId(1);
        request.setClienteNombre("X");
        request.setClienteDireccion("Y");
        request.setLatitudDestino(1.1);
        request.setLongitudDestino(2.2);
        request.setTotalEstimado(999.0);
        request.setDemoraAprox(15);
        List<CrearDetalleRequest> detalles = Arrays.asList(
                new CrearDetalleRequest(7, "Z", 50.0, 4));
        request.setDetalles(detalles);

        assertEquals(Integer.valueOf(3), request.getCajaId());
        assertEquals(2, request.getMetodoPagoId());
        assertEquals(1, request.getMetodoVentaId());
        assertEquals("X", request.getClienteNombre());
        assertEquals("Y", request.getClienteDireccion());
        assertEquals(Double.valueOf(1.1), request.getLatitudDestino());
        assertEquals(Double.valueOf(2.2), request.getLongitudDestino());
        assertEquals(999.0, request.getTotalEstimado(), 0.0001);
        assertEquals(Integer.valueOf(15), request.getDemoraAprox());
        assertEquals(detalles, request.getDetalles());
    }
}

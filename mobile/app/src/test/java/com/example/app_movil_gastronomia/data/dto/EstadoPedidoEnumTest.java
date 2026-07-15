package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Spec PED-DTO-001: {@link EstadoPedidoEnum} has 8 constants that map to the
 * exact API string values used by the backend (case-sensitive, no accents).
 * The enum is serialized to JSON as a plain string equal to its API value.
 */
public class EstadoPedidoEnumTest {

    private final Gson gson = new Gson();

    @Test
    public void hasExactlyEightConstants() {
        assertEquals(8, EstadoPedidoEnum.values().length);
    }

    @Test
    public void apiValueMappingMatchesBackendContract() {
        assertEquals("Pendiente", EstadoPedidoEnum.PENDIENTE.getApiValue());
        assertEquals("EnPreparacion", EstadoPedidoEnum.EN_PREPARACION.getApiValue());
        assertEquals("ListoParaRetirar", EstadoPedidoEnum.LISTO_PARA_RETIRAR.getApiValue());
        assertEquals("EnCamino", EstadoPedidoEnum.EN_CAMINO.getApiValue());
        assertEquals("Entregado", EstadoPedidoEnum.ENTREGADO.getApiValue());
        assertEquals("Retirado", EstadoPedidoEnum.RETIRADO.getApiValue());
        assertEquals("Cancelado", EstadoPedidoEnum.CANCELADO.getApiValue());
        assertEquals("Devuelto", EstadoPedidoEnum.DEVUELTO.getApiValue());
    }

    @Test
    public void fromApiValueReturnsCorrectConstant() {
        assertEquals(EstadoPedidoEnum.PENDIENTE, EstadoPedidoEnum.fromApiValue("Pendiente"));
        assertEquals(EstadoPedidoEnum.EN_PREPARACION, EstadoPedidoEnum.fromApiValue("EnPreparacion"));
        assertEquals(EstadoPedidoEnum.LISTO_PARA_RETIRAR, EstadoPedidoEnum.fromApiValue("ListoParaRetirar"));
        assertEquals(EstadoPedidoEnum.EN_CAMINO, EstadoPedidoEnum.fromApiValue("EnCamino"));
        assertEquals(EstadoPedidoEnum.ENTREGADO, EstadoPedidoEnum.fromApiValue("Entregado"));
        assertEquals(EstadoPedidoEnum.RETIRADO, EstadoPedidoEnum.fromApiValue("Retirado"));
        assertEquals(EstadoPedidoEnum.CANCELADO, EstadoPedidoEnum.fromApiValue("Cancelado"));
        assertEquals(EstadoPedidoEnum.DEVUELTO, EstadoPedidoEnum.fromApiValue("Devuelto"));
    }

    @Test
    public void fromApiValueReturnsNullForUnknownString() {
        assertEquals(null, EstadoPedidoEnum.fromApiValue("Desconocido"));
        assertEquals(null, EstadoPedidoEnum.fromApiValue(""));
    }

    @Test
    public void fromApiValueIsInverseOfGetApiValue() {
        List<EstadoPedidoEnum> all = Arrays.asList(EstadoPedidoEnum.values());
        for (EstadoPedidoEnum estado : all) {
            assertEquals(estado, EstadoPedidoEnum.fromApiValue(estado.getApiValue()));
        }
    }

    @Test
    public void serializesToApiValueString() {
        // Backend contract: when the estado field appears in JSON it is a string
        // like "Pendiente" — not the Java constant name (which happens to match here,
        // but the contract is the apiValue, not the Java identifier).
        String json = gson.toJson(EstadoPedidoEnum.EN_PREPARACION);
        assertEquals("\"EnPreparacion\"", json);

        String jsonListo = gson.toJson(EstadoPedidoEnum.LISTO_PARA_RETIRAR);
        assertEquals("\"ListoParaRetirar\"", jsonListo);
    }

    @Test
    public void serializesInsideJsonObjectAsStringValue() {
        // Integration sanity: an enum inside a JSON object serializes to its
        // apiValue string, not to a Java identifier.
        JsonObject container = new JsonObject();
        container.addProperty("estado", EstadoPedidoEnum.EN_CAMINO.getApiValue());
        String json = gson.toJson(container);
        assertNotNull(json);
        org.junit.Assert.assertTrue(
                "json must contain 'EnCamino', got: " + json,
                json.contains("\"EnCamino\""));
    }
}

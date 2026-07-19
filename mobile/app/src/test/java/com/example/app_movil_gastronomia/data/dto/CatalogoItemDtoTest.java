package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Spec coverage for the catalog DTOs.
 *
 * <p>Spec CAT-DTO-001: each catalog entry is a small object with
 * exactly two fields: an integer {@code id} and a String {@code nombre}.
 * The list response is just an array of these objects — no envelope.</p>
 *
 * <p>The server returns raw JSON like
 * {@code [{"id": 3, "nombre": "EnPreparacion"}]} and the client
 * serializes the same shape on the way back. Gson's default behavior
 * covers both directions because both fields are present in every
 * emission, so we only need to verify the key/values round-trip.</p>
 */
public class CatalogoItemDtoTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesWithExpectedKeys() {
        CatalogoItemDto item = new CatalogoItemDto(3, "EnPreparacion");

        String json = gson.toJson(item);

        assertTrue("json must contain 'id', got: " + json, json.contains("\"id\""));
        assertTrue("json must contain 'nombre', got: " + json, json.contains("\"nombre\""));
    }

    @Test
    public void serializesWithCorrectValues() {
        CatalogoItemDto item = new CatalogoItemDto(7, "Efectivo");

        String json = gson.toJson(item);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        assertEquals(7, obj.get("id").getAsInt());
        assertEquals("Efectivo", obj.get("nombre").getAsString());
    }

    @Test
    public void deserializesSingleItemFromApiJson() {
        String json = "{\"id\": 3, \"nombre\": \"EnPreparacion\"}";

        CatalogoItemDto item = gson.fromJson(json, CatalogoItemDto.class);

        assertNotNull(item);
        assertEquals(3, item.getId());
        assertEquals("EnPreparacion", item.getNombre());
    }

    @Test
    public void deserializesListFromApiJson() {
        String json = "["
                + "{\"id\": 1, \"nombre\": \"Efectivo\"},"
                + "{\"id\": 2, \"nombre\": \"Tarjeta\"},"
                + "{\"id\": 3, \"nombre\": \"MercadoPago\"}"
                + "]";

        List<CatalogoItemDto> items = Arrays.asList(
                gson.fromJson(json, CatalogoItemDto[].class));

        assertNotNull(items);
        assertEquals(3, items.size());
        assertEquals(1, items.get(0).getId());
        assertEquals("Efectivo", items.get(0).getNombre());
        assertEquals(2, items.get(1).getId());
        assertEquals("Tarjeta", items.get(1).getNombre());
        assertEquals(3, items.get(2).getId());
        assertEquals("MercadoPago", items.get(2).getNombre());
    }

    @Test
    public void gettersReturnConstructorValues() {
        CatalogoItemDto item = new CatalogoItemDto(42, "Delivery");

        assertEquals(42, item.getId());
        assertEquals("Delivery", item.getNombre());
    }

    @Test
    public void settersUpdateValues() {
        CatalogoItemDto item = new CatalogoItemDto(0, "");

        item.setId(99);
        item.setNombre("Salon");

        assertEquals(99, item.getId());
        assertEquals("Salon", item.getNombre());
    }

    @Test
    public void roundTripsThroughGson() {
        CatalogoItemDto original = new CatalogoItemDto(5, "Retirado");

        String json = gson.toJson(original);
        CatalogoItemDto restored = gson.fromJson(json, CatalogoItemDto.class);

        assertNotNull(restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getNombre(), restored.getNombre());
    }
}

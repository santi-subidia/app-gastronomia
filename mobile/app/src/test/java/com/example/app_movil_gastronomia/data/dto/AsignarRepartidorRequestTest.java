package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.pedido.AsignarRepartidorRequest;
import com.google.gson.Gson;

import org.junit.Test;

/**
 * Spec PED-DTO-001: {@link AsignarRepartidorRequest} wraps the repartidor
 * id as a primitive {@code int}. Wire format is {@code {"repartidorId":5}}.
 */
public class AsignarRepartidorRequestTest {

    private final Gson gson = new Gson();

    @Test
    public void serializesWithRepartidorIdKey() {
        AsignarRepartidorRequest request = new AsignarRepartidorRequest(5);
        String json = gson.toJson(request);

        assertTrue("json must contain 'repartidorId', got: " + json,
                json.contains("\"repartidorId\""));
        assertTrue("json must contain the id, got: " + json,
                json.contains("5"));
    }

    @Test
    public void roundTripsRepartidorId() {
        String sample = "{\"repartidorId\":7}";
        AsignarRepartidorRequest parsed = gson.fromJson(sample, AsignarRepartidorRequest.class);

        assertEquals(7, parsed.getRepartidorId());
    }

    @Test
    public void constructorStoresRepartidorId() {
        AsignarRepartidorRequest request = new AsignarRepartidorRequest(42);
        assertEquals(42, request.getRepartidorId());
    }

    @Test
    public void setterUpdatesRepartidorId() {
        AsignarRepartidorRequest request = new AsignarRepartidorRequest(1);
        request.setRepartidorId(99);
        assertEquals(99, request.getRepartidorId());
    }
}

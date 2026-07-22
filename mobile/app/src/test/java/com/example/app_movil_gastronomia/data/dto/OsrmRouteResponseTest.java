package com.example.app_movil_gastronomia.data.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.app_movil_gastronomia.data.dto.routing.OsrmRouteResponse;
import com.google.gson.Gson;

import org.junit.Test;

/** Verifies the GeoJSON route contract returned by OSRM. */
public class OsrmRouteResponseTest {

    @Test
    public void parsesGeoJsonCoordinates() {
        String json = "{\"code\":\"Ok\",\"routes\":[{\"geometry\":"
                + "{\"type\":\"LineString\",\"coordinates\":[[-58.4,-34.6],[-58.3,-34.5]]}}]}";

        OsrmRouteResponse response = new Gson().fromJson(json, OsrmRouteResponse.class);

        assertEquals("Ok", response.getCode());
        assertNotNull(response.getFirstRoute());
        assertEquals("LineString", response.getFirstRoute().getGeometry().getType());
        assertEquals(2, response.getRouteCoordinates().size());
        assertEquals(Double.valueOf(-58.4), response.getRouteCoordinates().get(0).get(0));
        assertEquals(Double.valueOf(-34.5), response.getRouteCoordinates().get(1).get(1));
    }
}

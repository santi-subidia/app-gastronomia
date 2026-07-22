package com.example.app_movil_gastronomia.data.dto.routing;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/** Minimal OSRM response model for a GeoJSON route. */
public class OsrmRouteResponse {

    @SerializedName("code")
    private String code;

    @SerializedName("routes")
    private List<Route> routes;

    public String getCode() {
        return code;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public Route getFirstRoute() {
        return routes == null || routes.isEmpty() ? null : routes.get(0);
    }

    public List<List<Double>> getRouteCoordinates() {
        Route route = getFirstRoute();
        if (route == null || route.geometry == null || route.geometry.coordinates == null) {
            return Collections.emptyList();
        }
        return route.geometry.coordinates;
    }

    public static class Route {
        @SerializedName("geometry")
        private Geometry geometry;

        public Geometry getGeometry() {
            return geometry;
        }
    }

    public static class Geometry {
        @SerializedName("type")
        private String type;

        @SerializedName("coordinates")
        private List<List<Double>> coordinates;

        public String getType() {
            return type;
        }

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }
    }
}

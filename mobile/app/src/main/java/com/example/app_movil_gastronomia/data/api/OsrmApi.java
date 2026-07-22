package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.routing.OsrmRouteResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Retrofit contract for the OSRM route service. */
public interface OsrmApi {

    @GET("route/v1/driving/{coordinates}")
    Call<OsrmRouteResponse> getRoute(
            @Path(value = "coordinates", encoded = true) String coordinates,
            @Query("overview") String overview,
            @Query("geometries") String geometries
    );
}

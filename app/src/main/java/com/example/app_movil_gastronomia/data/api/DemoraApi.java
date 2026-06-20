package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.DemoraDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit contract for the demoras REST endpoints.
 *
 * <p>Spec DEM-API-001: every method must match the server routes exactly
 * (paths, query keys, path keys, HTTP verbs). {@code pedidoId} on
 * {@link #getDemoras(Integer)} is nullable so the same endpoint can list
 * either the full set of demoras ({@code pedidoId == null}, query
 * parameter omitted by Retrofit) or the demoras filtered to a single
 * pedido. The design decision and the boxes-vs-primitives rationale live
 * in {@code sdd/entidad-demoras/design}.
 */
public interface DemoraApi {

    @GET("api/demoras")
    Call<List<DemoraDto>> getDemoras(@Query("pedidoId") Integer pedidoId);

    @POST("api/demoras")
    Call<DemoraDto> crearDemora(@Body CrearDemoraRequest request);

    @PUT("api/demoras/{id}")
    Call<DemoraDto> actualizarDemora(
            @Path("id") int id,
            @Body ActualizarDemoraRequest request
    );

    @DELETE("api/demoras/{id}")
    Call<Void> eliminarDemora(@Path("id") int id);
}

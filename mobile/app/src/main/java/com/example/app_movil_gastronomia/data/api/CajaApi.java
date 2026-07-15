package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit interface for the cajas REST endpoints.
 *
 * <p>Spec CAJ-API-001: 5 endpoints, all paths relative to the
 * Retrofit {@code baseUrl} (e.g. {@code https://api.example.com/}).
 * Path values match the backend contract 1:1; the {@code estado}
 * query parameter is nullable so the same endpoint can list all
 * cajas or filter by estado. Retrofit omits null query params
 * automatically, so passing {@code null} returns the full list.
 * {@link #getCajasAbiertas()} uses a dedicated server-side filter
 * (Spec CAJ-ABIERTAS-001) and takes no query parameters.</p>
 */
public interface CajaApi {

    @GET("api/cajas")
    Call<List<CajaDto>> getCajas(@Query("estado") String estado);

    @GET("api/cajas/abiertas")
    Call<List<CajaDto>> getCajasAbiertas();

    @GET("api/cajas/{id}")
    Call<CajaDto> getCaja(@Path("id") int id);

    @POST("api/cajas/apertura")
    Call<CajaDto> abrirCaja(@Body AbrirCajaRequest request);

    @POST("api/cajas/{id}/cierre")
    Call<CajaDto> cerrarCaja(
            @Path("id") int id,
            @Body CerrarCajaRequest request
    );
}

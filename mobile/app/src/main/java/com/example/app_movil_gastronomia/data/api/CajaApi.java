package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialDetalleDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialResumenDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    @GET("api/cajas/historial")
    Call<List<CajaHistorialResumenDto>> getHistorial();

    @GET("api/cajas/{id}/historial-detalle")
    Call<CajaHistorialDetalleDto> getHistorialDetalle(@Path("id") int id);
}

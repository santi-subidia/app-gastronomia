package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.demora.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.demora.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

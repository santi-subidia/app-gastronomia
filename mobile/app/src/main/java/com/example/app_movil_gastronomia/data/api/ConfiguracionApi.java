package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/**
 * Retrofit contract for the configuracion REST endpoints.
 *
 * <p>Spec CONF-API-001: the configuracion resource is a singleton per
 * business, so every route is {@code api/configuracion} (no {@code {id}}
 * path variable). The same DTO shape is used for GET, POST and PUT
 * because create and update share the same body contract.
 */
public interface ConfiguracionApi {

    @GET("api/configuracion")
    Call<ConfiguracionDto> getConfiguracion();

    @POST("api/configuracion")
    Call<ConfiguracionDto> crearConfiguracion(@Body ConfiguracionDto body);

    @PUT("api/configuracion")
    Call<ConfiguracionDto> actualizarConfiguracion(@Body ConfiguracionDto body);
}

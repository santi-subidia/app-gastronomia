package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.usuario.UpdateUserRequest;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UsuarioApi {

    @GET("api/Usuarios/repartidores/disponibles")
    Call<List<UsuarioDto>> getRepartidoresDisponibles();

    /** Returns all active delivery drivers, including unavailable drivers. */
    @GET("api/Usuarios")
    Call<List<UsuarioDto>> getRepartidores(@Query("role") String role);

    @GET("api/Usuarios/{id}")
    Call<UsuarioDto> getUsuario(@Path("id") int id);

    @PUT("api/Usuarios/{id}")
    Call<UsuarioDto> actualizarUsuario(@Path("id") int id, @Body UpdateUserRequest request);

    @retrofit2.http.POST("api/Usuarios/{id}/contingencia")
    Call<Void> reportarContingencia(@Path("id") int id, @Body com.example.app_movil_gastronomia.data.dto.usuario.ReportarContingenciaRequest request);
}

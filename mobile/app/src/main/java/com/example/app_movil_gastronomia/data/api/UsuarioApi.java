package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.usuario.UpdateUserRequest;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UsuarioApi {

    @GET("api/Usuarios/repartidores/disponibles")
    Call<List<UsuarioDto>> getRepartidoresDisponibles();

    @PUT("api/Usuarios/{id}")
    Call<UsuarioDto> actualizarUsuario(@Path("id") int id, @Body UpdateUserRequest request);
}

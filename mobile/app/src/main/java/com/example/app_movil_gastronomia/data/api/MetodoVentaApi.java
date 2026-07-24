package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface MetodoVentaApi {

    @GET("api/catalogo/metodos-venta")
    Call<List<CatalogoItemDto>> getMetodosVenta();
}

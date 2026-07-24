package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface EstadosPedidoApi {

    @GET("api/catalogo/estados-pedido")
    Call<List<CatalogoItemDto>> getEstados();
}

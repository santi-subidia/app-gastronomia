package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit interface for the estados-pedido catalog lookup.
 *
 * <p>Spec CAT-API-001: {@code GET api/catalogo/estados-pedido} returns
 * a flat array of {@link CatalogoItemDto} entries (no envelope). The
 * client uses this list to resolve a human-readable estado name into
 * the integer ID required by the v2 backend PATCH endpoint.</p>
 */
public interface EstadosPedidoApi {

    @GET("api/catalogo/estados-pedido")
    Call<List<CatalogoItemDto>> getEstados();
}

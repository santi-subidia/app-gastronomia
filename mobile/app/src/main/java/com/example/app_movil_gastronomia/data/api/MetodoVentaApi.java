package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit interface for the metodos-venta catalog lookup.
 *
 * <p>Spec CAT-API-001: {@code GET api/catalogo/metodos-venta} returns
 * a flat array of {@link CatalogoItemDto} entries (no envelope). The
 * client uses this list to resolve a sale method display name (e.g.
 * "Delivery" vs "Salon") into the integer ID required by the v2
 * backend.</p>
 */
public interface MetodoVentaApi {

    @GET("api/catalogo/metodos-venta")
    Call<List<CatalogoItemDto>> getMetodosVenta();
}

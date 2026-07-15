package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Retrofit interface for the metodos-pago catalog lookup.
 *
 * <p>Spec CAT-API-001: {@code GET api/catalogo/metodos-pago} returns
 * a flat array of {@link CatalogoItemDto} entries (no envelope). The
 * client uses this list to resolve a payment method display name into
 * the integer ID required by the v2 backend.</p>
 */
public interface MetodoPagoApi {

    @GET("api/catalogo/metodos-pago")
    Call<List<CatalogoItemDto>> getMetodosPago();
}

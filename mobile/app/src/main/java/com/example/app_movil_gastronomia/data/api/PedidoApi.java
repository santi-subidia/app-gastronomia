package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.pedido.AsignarRepartidorRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface for the pedidos REST endpoints.
 *
 * <p>Spec PED-API-001 (v2): 6 endpoints, all paths relative to the
 * Retrofit {@code baseUrl} (e.g. {@code https://api.example.com/}).
 * Path values match the backend contract 1:1; the {@code estado}
 * path variable is resolved from {@link EstadoPedidoEnum#getApiValue()}
 * at the repository layer so the enum never leaks into Retrofit.</p>
 *
 * <p>Spec PED-API-CHG-001: {@link #cambiarEstado(int, int)} accepts
 * a raw {@code int} body (the catalog-resolved estado ID) instead
 * of a wrapper object. Gson's default serialization of a bare
 * {@code @Body int} produces {@code 3} (a raw number), which is the
 * v2 contract.</p>
 */
public interface PedidoApi {

    @GET("api/pedidos")
    Call<List<PedidoResumenDto>> getPedidos();

    @GET("api/pedidos/{id}")
    Call<PedidoDetalleDto> getPedido(@Path("id") int id);

    @GET("api/pedidos/estado/{estado}")
    Call<List<PedidoResumenDto>> getByEstado(@Path("estado") String estado);

    @GET("api/pedidos/repartidor/{repartidorId}")
    Call<List<PedidoResumenDto>> getPedidosPorRepartidor(@Path("repartidorId") int repartidorId);

    @POST("api/pedidos")
    Call<PedidoDetalleDto> crearPedido(@Body CrearPedidoRequest request);

    @PATCH("api/pedidos/{id}/estado")
    Call<PedidoDetalleDto> cambiarEstado(
            @Path("id") int id,
            @Body int nuevoEstadoId
    );

    @PATCH("api/pedidos/{id}/repartidor")
    Call<PedidoDetalleDto> asignarRepartidor(
            @Path("id") int id,
            @Body AsignarRepartidorRequest request
    );
}

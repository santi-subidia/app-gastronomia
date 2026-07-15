package com.example.app_movil_gastronomia.data.api;

import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ProductoApi {

    @GET("api/productos")
    Call<List<ProductoDto>> getProductos();

    @GET("api/productos/{id}")
    Call<ProductoDto> getProducto(@Path("id") int id);

    @POST("api/productos")
    Call<ProductoDto> crearProducto(@Body CrearProductoRequest request);

    @PUT("api/productos/{id}")
    Call<ProductoDto> actualizarProducto(
            @Path("id") int id,
            @Body ActualizarProductoRequest request
    );

    @DELETE("api/productos/{id}")
    Call<Void> eliminarProducto(@Path("id") int id);
}

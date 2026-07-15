package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import java.util.List;

public interface ProductoRepository {

    LiveData<UiState<List<ProductoDto>>> getProductos();

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * product-list state. The same instance is reused across every
     * {@link #getProductos()} call so observers (typically a
     * {@code ViewModel}) can register exactly once.
     */
    LiveData<UiState<List<ProductoDto>>> getProductListState();

    /**
     * Fetches a single product by id. Each call resets the dedicated
     * {@link #getProductoState()} instance to LOADING before the network
     * request and posts SUCCESS or ERROR based on the response.
     */
    LiveData<UiState<ProductoDto>> getProducto(int id);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getProducto(int)} calls.
     */
    LiveData<UiState<ProductoDto>> getProductoState();

    /**
     * Creates a new product. Each call resets the dedicated
     * {@link #getCrearState()} instance to LOADING before the network
     * request and posts SUCCESS (with the created dto) or ERROR.
     */
    LiveData<UiState<ProductoDto>> crearProducto(CrearProductoRequest request);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #crearProducto(CrearProductoRequest)} calls.
     */
    LiveData<UiState<ProductoDto>> getCrearState();

    /**
     * Partially updates a product. Only the fields set on
     * {@code request} are sent in the PUT body (boxed Double/Integer are
     * omitted when null). Each call resets the dedicated
     * {@link #getActualizarState()} instance to LOADING and posts SUCCESS
     * (with the updated dto) or ERROR.
     */
    LiveData<UiState<ProductoDto>> actualizarProducto(
            int id,
            ActualizarProductoRequest request
    );

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #actualizarProducto(int, ActualizarProductoRequest)} calls.
     */
    LiveData<UiState<ProductoDto>> getActualizarState();

    /**
     * Deletes (or soft-deletes) a product. Each call resets the dedicated
     * {@link #getEliminarState()} instance to LOADING and posts SUCCESS
     * (with null data) or ERROR.
     */
    LiveData<UiState<Void>> eliminarProducto(int id);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #eliminarProducto(int)} calls.
     */
    LiveData<UiState<Void>> getEliminarState();
}

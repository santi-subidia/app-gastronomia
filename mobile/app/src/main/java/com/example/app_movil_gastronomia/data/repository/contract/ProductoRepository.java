package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import java.util.List;

public interface ProductoRepository {

    LiveData<UiState<List<ProductoDto>>> getProductos();

    LiveData<UiState<List<ProductoDto>>> getProductListState();

    LiveData<UiState<ProductoDto>> getProducto(int id);

    LiveData<UiState<ProductoDto>> getProductoState();

    LiveData<UiState<ProductoDto>> crearProducto(CrearProductoRequest request);

    LiveData<UiState<ProductoDto>> getCrearState();

    LiveData<UiState<ProductoDto>> actualizarProducto(
            int id,
            ActualizarProductoRequest request
    );

    LiveData<UiState<ProductoDto>> getActualizarState();

    LiveData<UiState<Void>> eliminarProducto(int id);

    LiveData<UiState<Void>> getEliminarState();
}

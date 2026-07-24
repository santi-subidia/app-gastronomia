package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;


public interface CatalogoRepository {

    LiveData<List<CatalogoItemDto>> getEstadosPedido();

    LiveData<List<CatalogoItemDto>> getMetodosPago();

    LiveData<List<CatalogoItemDto>> getMetodosVenta();

    int resolveEstadoId(String nombre);

    int resolveMetodoPagoId(String nombre);

    int resolveMetodoVentaId(String nombre);

    boolean isReady();
}

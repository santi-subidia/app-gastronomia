package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.demora.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.demora.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;

import java.util.List;

public interface DemoraRepository {

    LiveData<UiState<List<DemoraDto>>> getDemoras(Integer pedidoId);

    LiveData<UiState<List<DemoraDto>>> getDemorasState();

    LiveData<UiState<DemoraDto>> crearDemora(CrearDemoraRequest request);

    LiveData<UiState<DemoraDto>> getCrearState();

    LiveData<UiState<DemoraDto>> actualizarDemora(
            int id,
            ActualizarDemoraRequest request
    );

    LiveData<UiState<DemoraDto>> getActualizarState();

    LiveData<UiState<Void>> eliminarDemora(int id);

    LiveData<UiState<Void>> getEliminarState();
}

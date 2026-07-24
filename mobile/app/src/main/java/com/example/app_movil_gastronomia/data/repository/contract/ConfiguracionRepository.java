package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;

public interface ConfiguracionRepository {

    LiveData<UiState<ConfiguracionDto>> getConfiguracion();

    LiveData<UiState<ConfiguracionDto>> getConfiguracionState();

    LiveData<UiState<ConfiguracionDto>> crearConfiguracion(ConfiguracionDto body);

    LiveData<UiState<ConfiguracionDto>> getCrearState();

    LiveData<UiState<ConfiguracionDto>> actualizarConfiguracion(ConfiguracionDto body);

    LiveData<UiState<ConfiguracionDto>> getActualizarState();
}

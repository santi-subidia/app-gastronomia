package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;

/**
 * Configuracion REST data-layer contract.
 *
 * <p>The configuracion resource is a singleton, so the same DTO shape is
 * used for GET, POST and PUT. Each CRUD method returns a single
 * {@link LiveData} instance that is reset to {@code LOADING} on every
 * call, then posted {@code SUCCESS} (with the typed payload) or
 * {@code ERROR} based on the network result. The companion
 * {@code getXxxState()} getter returns that same instance so observers
 * (typically a {@code ViewModel}) can register exactly once and keep
 * receiving emissions across retries without leaking. This pattern is
 * verified in the {@code fix-livedata-pattern} /
 * {@code ProductoRepositoryImpl} chain (see PR #1 of
 * {@code entidad-productos}).
 */
public interface ConfiguracionRepository {

    /**
     * Fetches the singleton business configuration. Each call resets the
     * dedicated {@link #getConfiguracionState()} instance to LOADING
     * before the network request and posts SUCCESS (with the dto) or ERROR.
     */
    LiveData<UiState<ConfiguracionDto>> getConfiguracion();

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getConfiguracion()} calls.
     */
    LiveData<UiState<ConfiguracionDto>> getConfiguracionState();

    /**
     * Creates the singleton configuration (first-time setup). Each call
     * resets the dedicated {@link #getCrearState()} instance to LOADING
     * and posts SUCCESS (with the created dto) or ERROR.
     */
    LiveData<UiState<ConfiguracionDto>> crearConfiguracion(ConfiguracionDto body);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #crearConfiguracion(ConfiguracionDto)} calls.
     */
    LiveData<UiState<ConfiguracionDto>> getCrearState();

    /**
     * Updates the singleton configuration. Each call resets the dedicated
     * {@link #getActualizarState()} instance to LOADING and posts SUCCESS
     * (with the updated dto) or ERROR.
     */
    LiveData<UiState<ConfiguracionDto>> actualizarConfiguracion(ConfiguracionDto body);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #actualizarConfiguracion(ConfiguracionDto)} calls.
     */
    LiveData<UiState<ConfiguracionDto>> getActualizarState();
}

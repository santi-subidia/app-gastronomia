package com.example.app_movil_gastronomia.data.repository;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.DemoraDto;

import java.util.List;

/**
 * Demoras REST data-layer contract.
 *
 * <p>Each CRUD method returns a single {@link LiveData} instance that is
 * reset to {@code LOADING} on every call, then posted {@code SUCCESS}
 * (with the typed payload) or {@code ERROR} based on the network result.
 * The companion {@code getXxxState()} getter returns that same instance
 * so observers (typically a {@code ViewModel}) can register exactly once
 * and keep receiving emissions across retries without leaking. This
 * pattern is verified in the
 * {@code fix-livedata-pattern} / {@code ProductoRepositoryImpl} chain
 * (see PR #1 of {@code entidad-productos}).
 */
public interface DemoraRepository {

    /**
     * Fetches the demoras filtered to {@code pedidoId}, or every demora
     * when {@code pedidoId == null} (the {@code pedidoId} query parameter
     * is then omitted by Retrofit). Each call resets the dedicated
     * {@link #getDemorasState()} instance to LOADING before the network
     * request and posts SUCCESS (with the list) or ERROR.
     */
    LiveData<UiState<List<DemoraDto>>> getDemoras(Integer pedidoId);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getDemoras(Integer)} calls.
     */
    LiveData<UiState<List<DemoraDto>>> getDemorasState();

    /**
     * Creates a new demora. Each call resets the dedicated
     * {@link #getCrearState()} instance to LOADING before the network
     * request and posts SUCCESS (with the created dto) or ERROR.
     */
    LiveData<UiState<DemoraDto>> crearDemora(CrearDemoraRequest request);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #crearDemora(CrearDemoraRequest)} calls.
     */
    LiveData<UiState<DemoraDto>> getCrearState();

    /**
     * Partially updates a demora. Only the fields set on {@code request}
     * are sent in the PUT body (boxed {@code Integer} is omitted when
     * null). Each call resets the dedicated
     * {@link #getActualizarState()} instance to LOADING and posts SUCCESS
     * (with the updated dto) or ERROR.
     */
    LiveData<UiState<DemoraDto>> actualizarDemora(
            int id,
            ActualizarDemoraRequest request
    );

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #actualizarDemora(int, ActualizarDemoraRequest)} calls.
     */
    LiveData<UiState<DemoraDto>> getActualizarState();

    /**
     * Deletes (or soft-deletes) a demora. Each call resets the dedicated
     * {@link #getEliminarState()} instance to LOADING and posts SUCCESS
     * (with null data) or ERROR.
     */
    LiveData<UiState<Void>> eliminarDemora(int id);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #eliminarDemora(int)} calls.
     */
    LiveData<UiState<Void>> getEliminarState();
}

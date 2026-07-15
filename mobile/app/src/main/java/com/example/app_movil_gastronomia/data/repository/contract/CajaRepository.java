package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;

import java.util.List;

/**
 * Repository contract for the cajas REST data layer.
 *
 * <p>Each method emits LOADING through a single dedicated
 * {@link LiveData} instance (exposed via a paired {@code getXxxState()}
 * getter) before the network call and posts SUCCESS or ERROR on the
 * Retrofit callback. The instances are never reallocated so observers
 * registered in a {@code ViewModel} constructor keep receiving
 * emissions across retries without leaking.</p>
 *
 * <p>Spec CAJ-VAL-001: cajas has no client-side validation guards —
 * open/closed transitions are enforced by the server and surfaced via
 * {@code parseMensaje} on the ERROR branch.</p>
 */
public interface CajaRepository {

    /**
     * Fetches the list of cajas, optionally filtered by {@code estado}.
     * A {@code null} {@code estado} returns the full list (Retrofit
     * omits null query params). Each call resets the dedicated
     * {@link #getCajasState()} instance to LOADING and posts SUCCESS
     * (with the list) or ERROR.
     */
    LiveData<UiState<List<CajaDto>>> getCajas(String estado);

    /**
     * Returns the single {@link LiveData} instance that holds the
     * current state of {@link #getCajas(String)} calls.
     */
    LiveData<UiState<List<CajaDto>>> getCajasState();

    /**
     * Fetches the list of currently open cajas via the dedicated
     * {@code GET api/cajas/abiertas} endpoint (Spec CAJ-ABIERTAS-001).
     * Each call resets the dedicated {@link #getCajasAbiertasState()}
     * instance to LOADING and posts SUCCESS (with the list, which
     * may be empty when no caja is open) or ERROR.
     */
    LiveData<UiState<List<CajaDto>>> getCajasAbiertas();

    /**
     * Returns the single {@link LiveData} instance that holds the
     * current state of {@link #getCajasAbiertas()} calls.
     */
    LiveData<UiState<List<CajaDto>>> getCajasAbiertasState();

    /**
     * Fetches a single caja by id. Each call resets the dedicated
     * {@link #getCajaState()} instance to LOADING and posts SUCCESS
     * (with the dto) or ERROR.
     */
    LiveData<UiState<CajaDto>> getCaja(int id);

    /**
     * Returns the single {@link LiveData} instance that holds the
     * current state of {@link #getCaja(int)} calls.
     */
    LiveData<UiState<CajaDto>> getCajaState();

    /**
     * Opens a new caja with the provided apertura data. Each call
     * resets the dedicated {@link #getAbrirState()} instance to
     * LOADING and posts SUCCESS (with the created dto) or ERROR.
     */
    LiveData<UiState<CajaDto>> abrirCaja(AbrirCajaRequest request);

    /**
     * Returns the single {@link LiveData} instance that holds the
     * current state of {@link #abrirCaja(AbrirCajaRequest)} calls.
     */
    LiveData<UiState<CajaDto>> getAbrirState();

    /**
     * Closes the caja identified by {@code id} with the provided
     * cierre data. Each call resets the dedicated
     * {@link #getCerrarState()} instance to LOADING and posts SUCCESS
     * (with the updated dto) or ERROR.
     */
    LiveData<UiState<CajaDto>> cerrarCaja(int id, CerrarCajaRequest request);

    /**
     * Returns the single {@link LiveData} instance that holds the
     * current state of {@link #cerrarCaja(int, CerrarCajaRequest)} calls.
     */
    LiveData<UiState<CajaDto>> getCerrarState();
}

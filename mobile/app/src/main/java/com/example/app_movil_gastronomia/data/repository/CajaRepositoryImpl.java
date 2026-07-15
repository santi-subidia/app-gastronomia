package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.CajaApi;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owns a single {@link MutableLiveData} instance per method — one for
 * each of the 5 repository methods. Every instance is reset to LOADING
 * on its method call and then posted SUCCESS or ERROR. Instances are
 * never reallocated, so observers registered in the ViewModel
 * constructor (via {@code observeForever}) keep receiving emissions
 * across retries without leaking.
 *
 * <p>Spec CAJ-VAL-001: cajas has no client-side validation guards —
 * open/closed transitions are enforced by the server. Every method
 * goes straight to LOADING then the network call. Server-side 4xx
 * errors (e.g. 409 "ya existe una caja abierta") are surfaced through
 * {@link #parseMensaje} on the ERROR branch.</p>
 */
@Singleton
public class CajaRepositoryImpl implements CajaRepository {

    private static final String TAG = "CajaRepositoryImpl";

    private final CajaApi cajaApi;

    private final MutableLiveData<UiState<List<CajaDto>>> _cajasState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<CajaDto>>> _cajasAbiertasState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> _cajaState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> _abrirState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> _cerrarState = new MutableLiveData<>();

    @Inject
    public CajaRepositoryImpl(CajaApi cajaApi) {
        this.cajaApi = cajaApi;
    }

    // ------------------------------------------------------------------
    // getCajas(estado)
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<List<CajaDto>>> getCajas(String estado) {
        // Reset the single shared instance to LOADING before the network call.
        _cajasState.setValue(UiState.loading());

        // Pass through the nullable filter; Retrofit omits a null query
        // param so the same endpoint lists all cajas when estado == null.
        cajaApi.getCajas(estado).enqueue(new Callback<List<CajaDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CajaDto>> call,
                                   @NonNull Response<List<CajaDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cajasState.setValue(UiState.success(response.body()));
                } else {
                    _cajasState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CajaDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetCajas network failure", t);
                _cajasState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCajasState();
    }

    @Override
    public LiveData<UiState<List<CajaDto>>> getCajasState() {
        return _cajasState;
    }

    // ------------------------------------------------------------------
    // getCajasAbiertas()
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<List<CajaDto>>> getCajasAbiertas() {
        // Spec CAJ-ABIERTAS-001: dedicated endpoint, no query params.
        // The server returns 200 with an empty list when no caja is
        // open — we treat that as SUCCESS, not ERROR, so the UI can
        // render "no hay cajas abiertas" cleanly.
        _cajasAbiertasState.setValue(UiState.loading());

        cajaApi.getCajasAbiertas().enqueue(new Callback<List<CajaDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<CajaDto>> call,
                                   @NonNull Response<List<CajaDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cajasAbiertasState.setValue(UiState.success(response.body()));
                } else {
                    _cajasAbiertasState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CajaDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetCajasAbiertas network failure", t);
                _cajasAbiertasState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCajasAbiertasState();
    }

    @Override
    public LiveData<UiState<List<CajaDto>>> getCajasAbiertasState() {
        return _cajasAbiertasState;
    }

    // ------------------------------------------------------------------
    // getCaja(id)
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<CajaDto>> getCaja(int id) {
        _cajaState.setValue(UiState.loading());

        cajaApi.getCaja(id).enqueue(new Callback<CajaDto>() {
            @Override
            public void onResponse(@NonNull Call<CajaDto> call,
                                   @NonNull Response<CajaDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cajaState.setValue(UiState.success(response.body()));
                } else {
                    _cajaState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CajaDto> call, @NonNull Throwable t) {
                Log.e(TAG, "GetCaja network failure", t);
                _cajaState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCajaState();
    }

    @Override
    public LiveData<UiState<CajaDto>> getCajaState() {
        return _cajaState;
    }

    // ------------------------------------------------------------------
    // abrirCaja(request)
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<CajaDto>> abrirCaja(AbrirCajaRequest request) {
        _abrirState.setValue(UiState.loading());

        cajaApi.abrirCaja(request).enqueue(new Callback<CajaDto>() {
            @Override
            public void onResponse(@NonNull Call<CajaDto> call,
                                   @NonNull Response<CajaDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _abrirState.setValue(UiState.success(response.body()));
                } else {
                    _abrirState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CajaDto> call, @NonNull Throwable t) {
                Log.e(TAG, "AbrirCaja network failure", t);
                _abrirState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getAbrirState();
    }

    @Override
    public LiveData<UiState<CajaDto>> getAbrirState() {
        return _abrirState;
    }

    // ------------------------------------------------------------------
    // cerrarCaja(id, request)
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<CajaDto>> cerrarCaja(int id, CerrarCajaRequest request) {
        _cerrarState.setValue(UiState.loading());

        cajaApi.cerrarCaja(id, request).enqueue(new Callback<CajaDto>() {
            @Override
            public void onResponse(@NonNull Call<CajaDto> call,
                                   @NonNull Response<CajaDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cerrarState.setValue(UiState.success(response.body()));
                } else {
                    _cerrarState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<CajaDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CerrarCaja network failure", t);
                _cerrarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCerrarState();
    }

    @Override
    public LiveData<UiState<CajaDto>> getCerrarState() {
        return _cerrarState;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Parses the server's {@code {"mensaje":"..."}} envelope from a Retrofit
     * error body, falling back to {@code fallback} when the body is missing
     * or unparseable. Mirrors the PedidoRepositoryImpl pattern.
     */
    private static String parseMensaje(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                if (errorResponse != null && errorResponse.getMensaje() != null) {
                    return errorResponse.getMensaje();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error parsing error body", e);
        }
        return fallback;
    }
}

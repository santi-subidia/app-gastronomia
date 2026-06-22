package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.DemoraApi;
import com.example.app_movil_gastronomia.data.dto.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.DemoraDto;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
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
 * the demora list and one for each CRUD verb. Every instance is reset
 * to LOADING on its method call and then posted SUCCESS or ERROR.
 * Instances are never reallocated, so observers registered in the
 * ViewModel constructor (via {@code observeForever}) keep receiving
 * emissions across retries without leaking.
 *
 * <p>Mirrors the {@link ProductoRepositoryImpl} pattern: no client-side
 * validation guards, a single {@link #parseMensaje(Response, String)}
 * helper, and {@code setValue} in Retrofit callbacks (verified convention
 * from {@code fix-livedata-pattern}).
 */
@Singleton
public class DemoraRepositoryImpl implements DemoraRepository {

    private static final String TAG = "DemoraRepositoryImpl";

    private final DemoraApi demoraApi;
    private final MutableLiveData<UiState<List<DemoraDto>>> _demorasState = new MutableLiveData<>();
    private final MutableLiveData<UiState<DemoraDto>> _crearState = new MutableLiveData<>();
    private final MutableLiveData<UiState<DemoraDto>> _actualizarState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> _eliminarState = new MutableLiveData<>();

    @Inject
    public DemoraRepositoryImpl(DemoraApi demoraApi) {
        this.demoraApi = demoraApi;
    }

    @Override
    public LiveData<UiState<List<DemoraDto>>> getDemoras(Integer pedidoId) {
        _demorasState.setValue(UiState.loading());

        demoraApi.getDemoras(pedidoId).enqueue(new Callback<List<DemoraDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<DemoraDto>> call,
                                   @NonNull Response<List<DemoraDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _demorasState.setValue(UiState.success(response.body()));
                } else {
                    _demorasState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<DemoraDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetDemoras network failure", t);
                _demorasState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getDemorasState();
    }

    @Override
    public LiveData<UiState<List<DemoraDto>>> getDemorasState() {
        return _demorasState;
    }

    @Override
    public LiveData<UiState<DemoraDto>> crearDemora(CrearDemoraRequest request) {
        _crearState.setValue(UiState.loading());

        demoraApi.crearDemora(request).enqueue(new Callback<DemoraDto>() {
            @Override
            public void onResponse(@NonNull Call<DemoraDto> call,
                                   @NonNull Response<DemoraDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _crearState.setValue(UiState.success(response.body()));
                } else {
                    _crearState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<DemoraDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CrearDemora network failure", t);
                _crearState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCrearState();
    }

    @Override
    public LiveData<UiState<DemoraDto>> getCrearState() {
        return _crearState;
    }

    @Override
    public LiveData<UiState<DemoraDto>> actualizarDemora(int id, ActualizarDemoraRequest request) {
        _actualizarState.setValue(UiState.loading());

        demoraApi.actualizarDemora(id, request).enqueue(new Callback<DemoraDto>() {
            @Override
            public void onResponse(@NonNull Call<DemoraDto> call,
                                   @NonNull Response<DemoraDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _actualizarState.setValue(UiState.success(response.body()));
                } else {
                    _actualizarState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<DemoraDto> call, @NonNull Throwable t) {
                Log.e(TAG, "ActualizarDemora network failure", t);
                _actualizarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getActualizarState();
    }

    @Override
    public LiveData<UiState<DemoraDto>> getActualizarState() {
        return _actualizarState;
    }

    @Override
    public LiveData<UiState<Void>> eliminarDemora(int id) {
        _eliminarState.setValue(UiState.loading());

        demoraApi.eliminarDemora(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    _eliminarState.setValue(UiState.success(null));
                } else {
                    _eliminarState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "EliminarDemora network failure", t);
                _eliminarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getEliminarState();
    }

    @Override
    public LiveData<UiState<Void>> getEliminarState() {
        return _eliminarState;
    }

    /**
     * Parses the server's {@code {"mensaje":"..."}} envelope from a Retrofit
     * error body, falling back to {@code fallback} when the body is missing
     * or unparseable. Mirrors {@link ProductoRepositoryImpl#parseMensaje}.
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

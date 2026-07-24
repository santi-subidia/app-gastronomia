package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.ConfiguracionApi;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.repository.contract.ConfiguracionRepository;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class ConfiguracionRepositoryImpl implements ConfiguracionRepository {

    private static final String TAG = "ConfiguracionRepoImpl";

    private final ConfiguracionApi configuracionApi;
    private final MutableLiveData<UiState<ConfiguracionDto>> _configState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ConfiguracionDto>> _crearState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ConfiguracionDto>> _actualizarState = new MutableLiveData<>();

    @Inject
    public ConfiguracionRepositoryImpl(ConfiguracionApi configuracionApi) {
        this.configuracionApi = configuracionApi;
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> getConfiguracion() {
        _configState.setValue(UiState.loading());

        configuracionApi.getConfiguracion().enqueue(new Callback<ConfiguracionDto>() {
            @Override
            public void onResponse(@NonNull Call<ConfiguracionDto> call,
                                   @NonNull Response<ConfiguracionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _configState.setValue(UiState.success(response.body()));
                } else {
                    _configState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConfiguracionDto> call, @NonNull Throwable t) {
                Log.e(TAG, "GetConfiguracion network failure", t);
                _configState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getConfiguracionState();
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> getConfiguracionState() {
        return _configState;
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> crearConfiguracion(ConfiguracionDto body) {
        _crearState.setValue(UiState.loading());

        configuracionApi.crearConfiguracion(body).enqueue(new Callback<ConfiguracionDto>() {
            @Override
            public void onResponse(@NonNull Call<ConfiguracionDto> call,
                                   @NonNull Response<ConfiguracionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _crearState.setValue(UiState.success(response.body()));
                } else {
                    _crearState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConfiguracionDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CrearConfiguracion network failure", t);
                _crearState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCrearState();
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> getCrearState() {
        return _crearState;
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> actualizarConfiguracion(ConfiguracionDto body) {
        _actualizarState.setValue(UiState.loading());

        configuracionApi.actualizarConfiguracion(body).enqueue(new Callback<ConfiguracionDto>() {
            @Override
            public void onResponse(@NonNull Call<ConfiguracionDto> call,
                                   @NonNull Response<ConfiguracionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _actualizarState.setValue(UiState.success(response.body()));
                } else {
                    _actualizarState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConfiguracionDto> call, @NonNull Throwable t) {
                Log.e(TAG, "ActualizarConfiguracion network failure", t);
                _actualizarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getActualizarState();
    }

    @Override
    public LiveData<UiState<ConfiguracionDto>> getActualizarState() {
        return _actualizarState;
    }

    private static String parseMensaje(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                if (errorResponse != null && errorResponse.getMensaje() != null) {
                    return errorResponse.getMensaje();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error body", e);
        }
        return fallback;
    }
}

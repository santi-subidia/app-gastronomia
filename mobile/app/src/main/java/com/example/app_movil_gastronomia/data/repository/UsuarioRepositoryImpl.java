package com.example.app_movil_gastronomia.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.UsuarioApi;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.dto.usuario.UpdateUserRequest;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.google.gson.Gson;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final UsuarioApi api;
    private final Gson gson;
    
    private final MutableLiveData<UiState<List<UsuarioDto>>> repartidoresState = new MutableLiveData<>();
    private final MutableLiveData<UiState<UsuarioDto>> updateState = new MutableLiveData<>();

    @Inject
    public UsuarioRepositoryImpl(UsuarioApi api, Gson gson) {
        this.api = api;
        this.gson = gson;
    }

    @Override
    public LiveData<UiState<List<UsuarioDto>>> getRepartidoresState() {
        return repartidoresState;
    }

    @Override
    public void fetchRepartidores() {
        repartidoresState.setValue(UiState.loading());
        api.getRepartidores("Repartidor").enqueue(new Callback<List<UsuarioDto>>() {
            @Override
            public void onResponse(Call<List<UsuarioDto>> call, Response<List<UsuarioDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    repartidoresState.setValue(UiState.success(response.body()));
                } else {
                    repartidoresState.setValue(UiState.error(parseError(response)));
                }
            }

            @Override
            public void onFailure(Call<List<UsuarioDto>> call, Throwable t) {
                repartidoresState.setValue(UiState.error(t.getMessage()));
            }
        });
    }

    @Override
    public LiveData<UiState<List<UsuarioDto>>> getRepartidoresDisponiblesState() {
        return repartidoresState;
    }

    @Override
    public void fetchRepartidoresDisponibles() {
        repartidoresState.setValue(UiState.loading());
        api.getRepartidoresDisponibles().enqueue(new Callback<List<UsuarioDto>>() {
            @Override
            public void onResponse(Call<List<UsuarioDto>> call, Response<List<UsuarioDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    repartidoresState.setValue(UiState.success(response.body()));
                } else {
                    repartidoresState.setValue(UiState.error(parseError(response)));
                }
            }

            @Override
            public void onFailure(Call<List<UsuarioDto>> call, Throwable t) {
                repartidoresState.setValue(UiState.error(t.getMessage()));
            }
        });
    }

    @Override
    public LiveData<UiState<UsuarioDto>> getUpdateState() {
        return updateState;
    }

    @Override
    public void updateDisponibilidad(int id, boolean disponible) {
        updateState.setValue(UiState.loading());
        api.actualizarUsuario(id, new UpdateUserRequest(disponible)).enqueue(new Callback<UsuarioDto>() {
            @Override
            public void onResponse(Call<UsuarioDto> call, Response<UsuarioDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateState.setValue(UiState.success(response.body()));
                } else {
                    updateState.setValue(UiState.error(parseError(response)));
                }
            }

            @Override
            public void onFailure(Call<UsuarioDto> call, Throwable t) {
                updateState.setValue(UiState.error(t.getMessage()));
            }
        });
    }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                ErrorResponse err = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
                if (err != null && err.getMensaje() != null) return err.getMensaje();
            }
        } catch (Exception ignored) {}
        return "Error en la peticion";
    }
}


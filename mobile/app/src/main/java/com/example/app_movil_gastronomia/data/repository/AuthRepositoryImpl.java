package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.AuthApi;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;
import com.google.gson.Gson;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owns a single {@link MutableLiveData} instance ({@code _loginState}) that
 * is reset to LOADING on every {@link #login(LoginRequest)} call and then
 * posted SUCCESS or ERROR. The instance is never reallocated, so observers
 * registered in the ViewModel constructor (via {@code observeForever}) keep
 * receiving emissions across retries without leaking.
 */
@Singleton
public class AuthRepositoryImpl implements AuthRepository {

    private static final String TAG = "AuthRepositoryImpl";

    private final AuthApi authApi;
    private final TokenManager tokenManager;
    private final MutableLiveData<UiState<LoginResponse>> _loginState = new MutableLiveData<>();

    @Inject
    public AuthRepositoryImpl(AuthApi authApi, TokenManager tokenManager) {
        this.authApi = authApi;
        this.tokenManager = tokenManager;
    }

    @Override
    public LiveData<UiState<LoginResponse>> login(LoginRequest request) {
        // Reset the single shared instance to LOADING before the network call.
        _loginState.setValue(UiState.loading());

        authApi.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call,
                                   @NonNull Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse body = response.body();
                    tokenManager.saveToken(
                            body.getToken(),
                            body.getRolNombre(),
                            body.getId(),
                            body.getUsuarioNombre()
                    );
                    _loginState.setValue(UiState.success(body));
                } else {
                    String errorMsg = "Error de autenticación";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Gson gson = new Gson();
                            ErrorResponse errorResponse = gson.fromJson(errorBody, ErrorResponse.class);
                            if (errorResponse != null && errorResponse.getMensaje() != null) {
                                errorMsg = errorResponse.getMensaje();
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    _loginState.setValue(UiState.error(errorMsg));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Login network failure", t);
                _loginState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getLoginState();
    }

    @Override
    public LiveData<UiState<LoginResponse>> getLoginState() {
        return _loginState;
    }
}

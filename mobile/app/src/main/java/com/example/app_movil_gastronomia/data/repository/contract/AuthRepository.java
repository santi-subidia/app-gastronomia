package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;

public interface AuthRepository {

    LiveData<UiState<LoginResponse>> login(LoginRequest request);
    LiveData<UiState<LoginResponse>> getLoginState();
    void resetLoginState();
}

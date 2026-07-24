package com.example.app_movil_gastronomia.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;


import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<UiState<LoginResponse>> loginState = new MutableLiveData<>();
    private final Observer<UiState<LoginResponse>> repositoryObserver;

    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
        this.repositoryObserver = loginState::setValue;
        authRepository.getLoginState().observeForever(repositoryObserver);
    }

    public LiveData<UiState<LoginResponse>> getLoginState() {
        return loginState;
    }

    public void login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            loginState.setValue(UiState.error("El usuario es obligatorio"));
            return;
        }
        if (password == null || password.length() < 6) {
            loginState.setValue(UiState.error("La contraseña debe tener al menos 6 caracteres"));
            return;
        }

        LoginRequest request = new LoginRequest(username.trim(), password);
        authRepository.login(request);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        authRepository.getLoginState().removeObserver(repositoryObserver);
    }

}

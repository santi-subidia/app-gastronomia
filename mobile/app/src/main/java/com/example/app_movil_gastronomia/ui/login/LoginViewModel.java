package com.example.app_movil_gastronomia.ui.login;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Bridges the {@link AuthRepository} single-instance login state into a
 * VM-owned LiveData. Registers an {@code observeForever} observer exactly
 * once in the constructor and removes it in {@link #onCleared()} to avoid
 * the per-call observer leak that the previous implementation suffered from.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<UiState<LoginResponse>> loginState = new MutableLiveData<>();
    private final Observer<UiState<LoginResponse>> repositoryObserver;
    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
        // Register ONCE for the lifetime of this ViewModel.
        this.repositoryObserver = loginState::setValue;
        authRepository.getLoginState().observeForever(repositoryObserver);
        observerRegistrationCount.incrementAndGet();
    }

    public LiveData<UiState<LoginResponse>> getLoginState() {
        return loginState;
    }

    /**
     * Attempts login with the provided credentials.
     * Validates fields locally before making the network call.
     * The repository's shared LiveData is observed by the bridge registered
     * in the constructor — calling {@code login()} does NOT register a new
     * observer.
     */
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

    /** Test-only diagnostic: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

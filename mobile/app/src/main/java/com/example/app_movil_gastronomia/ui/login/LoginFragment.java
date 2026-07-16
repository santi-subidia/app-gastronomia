package com.example.app_movil_gastronomia.ui.login;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;
import com.example.app_movil_gastronomia.databinding.FragmentLoginBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    @Inject
    SignalRService signalRService;

    @Inject
    TokenManager tokenManager;

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.buttonLogin.setOnClickListener(v -> attemptLogin());

        viewModel.getLoginState().observe(getViewLifecycleOwner(), this::handleState);
    }

    private void attemptLogin() {
        String username = binding.editUsername.getText() != null
                ? binding.editUsername.getText().toString().trim() : "";
        String password = binding.editPassword.getText() != null
                ? binding.editPassword.getText().toString() : "";

        binding.textError.setVisibility(View.GONE);
        viewModel.login(username, password);
    }

    private void handleState(UiState<LoginResponse> state) {
        if (state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                showLoading(true);
                break;
            case SUCCESS:
                showLoading(false);
                connectSignalR();
                if (getActivity() instanceof com.example.app_movil_gastronomia.MainActivity) {
                    ((com.example.app_movil_gastronomia.MainActivity) getActivity()).onLoginSuccess();
                }
                navigateByRole(state.getData().getRolNombre());
                break;
            case ERROR:
                showLoading(false);
                showError(state.getError());
                break;
        }
    }

    private void connectSignalR() {
        String token = tokenManager.getToken();
        if (token != null && !token.isEmpty()) {
            signalRService.connect(token);
            Log.d(TAG, "SignalR connection initiated");
        } else {
            Log.w(TAG, "SignalR connect skipped — no token available");
        }
    }

    private void navigateByRole(String rolNombre) {
        int destinationId;
        if ("Cajero".equalsIgnoreCase(rolNombre)) {
            destinationId = R.id.action_nav_login_to_nav_cajero_home;
        } else if ("Cocina".equalsIgnoreCase(rolNombre)) {
            destinationId = R.id.action_nav_login_to_nav_cocina_home;
        } else if ("Repartidor".equalsIgnoreCase(rolNombre)) {
            destinationId = R.id.action_nav_login_to_nav_repartidor_home;
        } else {
            // Unknown role — default to Cajero home with a warning
            destinationId = R.id.action_nav_login_to_nav_cajero_home;
        }

        Navigation.findNavController(requireView()).navigate(destinationId);
    }

    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!loading);
    }

    private void showError(String message) {
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
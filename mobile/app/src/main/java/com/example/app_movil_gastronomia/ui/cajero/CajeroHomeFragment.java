package com.example.app_movil_gastronomia.ui.cajero;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.databinding.FragmentCajeroHomeBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CajeroHomeFragment extends Fragment {

    private FragmentCajeroHomeBinding binding;
    private CajeroHomeViewModel viewModel;

    @Nullable
    private UiState<Integer> lastPedidosState;
    @Nullable
    private UiState<Boolean> lastCajaState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCajeroHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CajeroHomeViewModel.class);

        viewModel.getActivePedidosState().observe(getViewLifecycleOwner(), state -> {
            lastPedidosState = state;
            renderDashboard();
        });
        viewModel.getCajaState().observe(getViewLifecycleOwner(), state -> {
            lastCajaState = state;
            renderDashboard();
        });

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());

        binding.buttonPedidos.setOnClickListener(v -> navigateToPedidos());
        binding.buttonProductos.setOnClickListener(v -> navigateToProductos());
        binding.buttonCrearPedido.setOnClickListener(v -> navigateToCrearPedido());
        binding.buttonCaja.setOnClickListener(v -> navigateToCaja());
        binding.buttonConfig.setOnClickListener(v -> navigateToConfiguracion());
    }

    private void renderDashboard() {
        if (binding == null) return;

        if (lastPedidosState == null || lastCajaState == null) {
            showLoading();
            return;
        }

        boolean loading = lastPedidosState.getStatus() == UiState.Status.LOADING
                || lastCajaState.getStatus() == UiState.Status.LOADING;
        if (loading) {
            showLoading();
            return;
        }

        String error = lastPedidosState.getError() != null
                ? lastPedidosState.getError()
                : lastCajaState.getError();
        if (error != null) {
            showError(error);
            return;
        }

        showContent(lastPedidosState.getData(), lastCajaState.getData());
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.cardStats.setVisibility(View.GONE);
        binding.gridDashboard.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(int activePedidos, boolean cajaOpen) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        binding.cardStats.setVisibility(View.VISIBLE);
        binding.gridDashboard.setVisibility(View.VISIBLE);

        binding.textActiveOrders.setText(getString(R.string.active_orders_count, activePedidos));
        binding.textCajaStatus.setText(getString(
                R.string.caja_status,
                getString(cajaOpen ? R.string.caja_open : R.string.caja_closed)));
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.cardStats.setVisibility(View.GONE);
        binding.gridDashboard.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    private void navigateToPedidos() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_cajero_home_to_nav_pedido_list);
    }

    private void navigateToProductos() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_cajero_home_to_nav_cajero_productos);
    }

    private void navigateToCrearPedido() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_cajero_home_to_nav_crear_pedido);
    }

    private void navigateToCaja() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_cajero_home_to_nav_caja);
    }

    private void navigateToConfiguracion() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_cajero_home_to_nav_configuracion);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

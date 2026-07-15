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

/**
 * Cajero dashboard. Two pieces of information up top (active pedidos
 * count and caja open/closed status) plus a 2x2 grid of quick links
 * into the most common cajero flows:
 *
 * <ul>
 *   <li><b>Pedidos</b> → navigates to {@code nav_pedido_list}.</li>
 *   <li><b>Productos</b> → navigates to {@code nav_cajero_productos}
 *       (the new destination that wraps the existing
 *       {@link ProductListFragment}).</li>
 *   <li><b>Caja</b> → navigates to {@code nav_caja}.</li>
 *   <li><b>Configuración</b> → navigates to {@code nav_configuracion}
 *       (the singleton business-configuration form backed by
 *       {@code ConfiguracionFragment} / {@code ConfiguracionViewModel}).</li>
 * </ul>
 *
 * <p>The two stat streams from {@link CajeroHomeViewModel} are
 * independent: the spinner is shown while <em>either</em> is loading
 * and an error message + retry button is shown if <em>either</em>
 * errors. Once both are in SUCCESS, the stats card is rendered with
 * the latest count and open/closed flag.</p>
 */
@AndroidEntryPoint
public class CajeroHomeFragment extends Fragment {

    private FragmentCajeroHomeBinding binding;
    private CajeroHomeViewModel viewModel;

    /**
     * Cached state of the two VM streams so {@link #renderDashboard()}
     * can fold them into a single render. {@code null} means "not
     * emitted yet" (still in LOADING).
     */
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

    /**
     * Folds the two independent state streams into a single render.
     * LOADING on either side hides the stats card and shows the
     * spinner. ERROR on either side shows the error message and the
     * retry button. SUCCESS on both renders the stats card.
     */
    private void renderDashboard() {
        if (binding == null) return;

        // Initial state: neither stream has emitted yet → keep the spinner up.
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

        // Prefer the first non-null error message (pedidos first, then caja).
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

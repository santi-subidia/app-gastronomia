package com.example.app_movil_gastronomia.ui.repartidor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.databinding.FragmentMapaBinding;
import com.example.app_movil_gastronomia.ui.pedido.PedidoAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Repartidor map screen. Shows the pedidos the rider is on the road
 * for and lets them broadcast their current GPS position to the hub
 * either manually (button) or automatically (switch + 8s Handler in
 * the ViewModel).
 *
 * <p>The actual map rendering is intentionally left for a future
 * Google Maps integration. Today this fragment only displays the
 * coordinates as text plus the active deliveries list.</p>
 *
 * <p>Runtime permission for {@code ACCESS_FINE_LOCATION} is requested
 * on first use via the
 * {@link ActivityResultContracts.RequestPermission} contract. If the
 * user denies it, the GPS section shows the "no disponible" label and
 * the "Enviar Posición Ahora" button is disabled.</p>
 */
@AndroidEntryPoint
public class MapaFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private FragmentMapaBinding binding;
    private MapaViewModel viewModel;
    private PedidoAdapter adapter;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    onLocationPermissionGranted();
                } else {
                    onLocationPermissionDenied();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMapaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MapaViewModel.class);

        adapter = new PedidoAdapter();
        binding.recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPedidos.setAdapter(adapter);

        adapter.setOnItemClickListener(this::navigateToDetail);

        viewModel.getPedidosState().observe(getViewLifecycleOwner(), this::handlePedidosState);
        viewModel.getGpsState().observe(getViewLifecycleOwner(), this::handleGpsState);
        viewModel.getLastSentState().observe(getViewLifecycleOwner(), this::handleLastSentState);
        viewModel.getAutoSendEnabled().observe(getViewLifecycleOwner(), this::handleAutoSendToggle);

        binding.buttonSendPosition.setOnClickListener(v -> viewModel.sendPositionNow());
        binding.switchAutoSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                // Only react to user-initiated toggles. Programmatic
                // setChecked() from the observer does not set isPressed.
                onAutoSendToggled(isChecked);
            }
        });
        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());

        // GPS startup: request permission if needed, then subscribe.
        if (hasLocationPermission()) {
            onLocationPermissionGranted();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------------------------------------------
    // Permission flow
    // ------------------------------------------------------------------

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void onLocationPermissionGranted() {
        if (viewModel != null) {
            viewModel.startGpsUpdates();
        }
        binding.buttonSendPosition.setEnabled(true);
    }

    private void onLocationPermissionDenied() {
        if (viewModel != null) {
            viewModel.stopGpsUpdates();
        }
        binding.buttonSendPosition.setEnabled(false);
        binding.textGpsCoords.setText(R.string.gps_unavailable);
        if (binding != null) {
            Snackbar.make(binding.getRoot(),
                    R.string.gps_unavailable,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // Auto-send toggle wiring
    // ------------------------------------------------------------------

    private void onAutoSendToggled(boolean isChecked) {
        if (isChecked && !hasLocationPermission()) {
            // Re-prompt the user before we start broadcasting.
            binding.switchAutoSend.setChecked(false);
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        viewModel.setAutoSendEnabled(isChecked);
    }

    private void handleAutoSendToggle(Boolean enabled) {
        if (binding == null || enabled == null) return;
        if (binding.switchAutoSend.isChecked() != enabled) {
            // Suppress the listener while we sync the visual state.
            binding.switchAutoSend.setOnCheckedChangeListener(null);
            binding.switchAutoSend.setChecked(enabled);
            binding.switchAutoSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    onAutoSendToggled(isChecked);
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // State observers
    // ------------------------------------------------------------------

    private void handlePedidosState(UiState<List<PedidoResumenDto>> state) {
        if (state == null || binding == null) return;

        switch (state.getStatus()) {
            case LOADING:
                showLoading();
                break;
            case SUCCESS:
                showContent(state.getData());
                break;
            case ERROR:
                showError(state.getError());
                break;
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewPedidos.setVisibility(View.GONE);
        binding.textEmptyDeliveries.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(List<PedidoResumenDto> pedidos) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        if (pedidos == null || pedidos.isEmpty()) {
            binding.recyclerViewPedidos.setVisibility(View.GONE);
            binding.textEmptyDeliveries.setVisibility(View.VISIBLE);
        } else {
            binding.textEmptyDeliveries.setVisibility(View.GONE);
            binding.recyclerViewPedidos.setVisibility(View.VISIBLE);
            adapter.submitList(pedidos);
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewPedidos.setVisibility(View.GONE);
        binding.textEmptyDeliveries.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    private void handleGpsState(String coords) {
        if (binding == null) return;
        if (coords == null || coords.isEmpty() || MapaViewModel.GPS_STATE_WAITING.equals(coords)) {
            binding.textGpsCoords.setText(R.string.waiting_gps);
        } else if (MapaViewModel.GPS_STATE_UNAVAILABLE.equals(coords)) {
            binding.textGpsCoords.setText(R.string.gps_unavailable);
        } else {
            binding.textGpsCoords.setText(coords);
        }
    }

    private void handleLastSentState(String lastSent) {
        if (binding == null) return;
        binding.textLastSent.setText(getString(R.string.last_sent,
                lastSent != null ? lastSent : "--:--:--"));
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    private void navigateToDetail(PedidoResumenDto pedido) {
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedido.getId());
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.nav_pedido_detail, args);
    }
}

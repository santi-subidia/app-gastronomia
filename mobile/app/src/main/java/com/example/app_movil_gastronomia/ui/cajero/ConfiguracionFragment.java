package com.example.app_movil_gastronomia.ui.cajero;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.Toast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import org.maplibre.android.MapLibre;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;

import com.example.app_movil_gastronomia.BuildConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.databinding.FragmentConfiguracionBinding;
import com.google.android.material.snackbar.Snackbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConfiguracionFragment extends Fragment {

    private FragmentConfiguracionBinding binding;
    private ConfiguracionViewModel viewModel;

    /**
     * Configuración cacheada para decidir si el próximo guardado crea o actualiza.
     */
    @Nullable
    private ConfiguracionDto lastConfig;

    private Double selectedLat = null;
    private Double selectedLng = null;
    private MapLibreMap mapLibreMap;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    centerMapOnCurrentLocation();
                } else {
                    centerMapOnDefaultLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        MapLibre.getInstance(requireContext());
        binding = FragmentConfiguracionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.mapView.onCreate(savedInstanceState);
        
        // Evita que ScrollView interfiera con el desplazamiento del mapa.
        binding.mapView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    binding.scrollView.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    binding.scrollView.requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        binding.mapView.getMapAsync(map -> {
            mapLibreMap = map;
            String styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=" + BuildConfig.MAPTILER_KEY;
            map.setStyle(styleUrl, style -> {
                // Style loaded
                if (lastConfig != null && lastConfig.getLatitudPartida() != null && lastConfig.getLongitudPartida() != null) {
                    map.setCameraPosition(new CameraPosition.Builder()
                            .target(new LatLng(lastConfig.getLatitudPartida(), lastConfig.getLongitudPartida()))
                            .zoom(15)
                            .build());
                } else {
                    // Try to get current location
                    if (hasLocationPermission()) {
                        centerMapOnCurrentLocation();
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }
            });

            map.addOnCameraIdleListener(() -> {
                if (mapLibreMap != null) {
                    LatLng target = mapLibreMap.getCameraPosition().target;
                    selectedLat = target.getLatitude();
                    selectedLng = target.getLongitude();
                }
            });
        });

        viewModel = new ViewModelProvider(this).get(ConfiguracionViewModel.class);

        viewModel.getConfigState().observe(getViewLifecycleOwner(), this::renderConfigState);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);
        binding.buttonSave.setOnClickListener(v -> submit());
        binding.buttonRetry.setOnClickListener(v -> viewModel.loadConfiguracion());
    }

    // ------------------------------------------------------------------
    // Observadores de estado.
    // ------------------------------------------------------------------

    /**
     * Renderiza el estado actual. LOADING muestra el indicador, ERROR
     * muestra el mensaje y reintento, y SUCCESS muestra el formulario.
     * Un payload null activa el modo creación.
     * non-null payload means "update mode" (prefilled + "Actualizar").
     */
    private void renderConfigState(UiState<ConfiguracionDto> state) {
        if (binding == null || state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.formContainer.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressBar.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                binding.formContainer.setVisibility(View.VISIBLE);

                ConfiguracionDto dto = state.getData();
                lastConfig = dto;
                if (dto == null) {
                    applyCreateMode();
                } else {
                    applyUpdateMode(dto);
                }
                break;
            case ERROR:
                binding.progressBar.setVisibility(View.GONE);
                binding.formContainer.setVisibility(View.GONE);
                binding.textError.setVisibility(View.VISIBLE);
                binding.buttonRetry.setVisibility(View.VISIBLE);
                binding.textError.setText(
                        state.getError() != null ? state.getError() : getString(R.string.error_generic)
                );
                break;
        }
    }

    /**
     * Renderiza el resultado del último guardado. LOADING desactiva el botón,
     * SUCCESS muestra un aviso y vuelve al panel, y ERROR permite reintentar.
     * message.
     */
    private void renderSaveState(UiState<ConfiguracionDto> state) {
        if (binding == null || state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.buttonSave.setEnabled(false);
                break;
            case SUCCESS:
                binding.buttonSave.setEnabled(true);
                Toast.makeText(requireContext(), R.string.config_saved, Toast.LENGTH_SHORT).show();
                viewModel.clearSaveState();
                navigateBack();
                break;
            case ERROR:
                binding.buttonSave.setEnabled(true);
                Snackbar.make(
                        binding.getRoot(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG
                ).show();
                break;
        }
    }

    // ------------------------------------------------------------------
    // Form rendering
    // ------------------------------------------------------------------

    private void applyCreateMode() {
        binding.buttonSave.setText(R.string.save_config);
        binding.inputNombre.setText("");
        binding.inputMaxPedidos.setText("");
        selectedLat = null;
        selectedLng = null;
    }

    private void applyUpdateMode(ConfiguracionDto dto) {
        binding.buttonSave.setText(R.string.update_config);
        binding.inputNombre.setText(
                dto.getNombreGastronomico() != null ? dto.getNombreGastronomico() : ""
        );
        binding.inputMaxPedidos.setText(
                dto.getMaxPedidosPorRepartidor() != null ? String.valueOf(dto.getMaxPedidosPorRepartidor()) : ""
        );
        selectedLat = dto.getLatitudPartida();
        selectedLng = dto.getLongitudPartida();
        
        if (mapLibreMap != null && selectedLat != null && selectedLng != null) {
            mapLibreMap.easeCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(selectedLat, selectedLng), 15
            ));
        }
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    private void submit() {
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setNombreGastronomico(textOf(binding.inputNombre));

        dto.setLatitudPartida(selectedLat);
        dto.setLongitudPartida(selectedLng);
        
        String maxPedidosStr = textOf(binding.inputMaxPedidos);
        dto.setMaxPedidosPorRepartidor(parseInteger(maxPedidosStr));
        
        // El ID no se lee del formulario: el ViewModel lo copia de la configuración
        // cacheada cuando se trata de una actualización.
        viewModel.saveConfiguracion(dto);
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private static String textOf(android.widget.EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    @Nullable
    private static Integer parseInteger(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Double parseDouble(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void navigateBack() {
        NavController controller = Navigation.findNavController(requireView());
        controller.popBackStack();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void centerMapOnCurrentLocation() {
        if (mapLibreMap == null) return;
        
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null && mapLibreMap != null) {
                        mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                .zoom(16)
                                .build());
                    } else {
                        // Fallback in case Google Services can't determine location
                        centerMapOnDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> centerMapOnDefaultLocation());
    }

    private void centerMapOnDefaultLocation() {
        if (mapLibreMap != null) {
            mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(-34.6037, -58.3816))
                    .zoom(12)
                    .build());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (binding != null) binding.mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (binding != null) binding.mapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) binding.mapView.onDestroy();
        binding = null;
    }
}

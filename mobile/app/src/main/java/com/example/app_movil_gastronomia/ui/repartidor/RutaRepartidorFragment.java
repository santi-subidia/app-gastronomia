package com.example.app_movil_gastronomia.ui.repartidor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.app_movil_gastronomia.BuildConfig;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.routing.OsrmRouteResponse;
import com.example.app_movil_gastronomia.databinding.FragmentRutaRepartidorBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.snackbar.Snackbar;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.location.LocationComponentActivationOptions;
import org.maplibre.android.location.modes.CameraMode;
import org.maplibre.android.location.modes.RenderMode;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.LineLayer;
import org.maplibre.android.style.layers.Property;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.LineString;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** MapLibre route screen for one delivery destination. */
@AndroidEntryPoint
public class RutaRepartidorFragment extends Fragment {

    private static final String ROUTE_SOURCE_ID = "delivery-route";
    private static final String ROUTE_LAYER_ID = "delivery-route-line";
    private static final String DESTINATION_SOURCE_ID = "delivery-destination";
    private static final String DESTINATION_LAYER_ID = "delivery-destination-marker";
    private static final String DESTINATION_ICON_ID = "delivery-destination-icon";

    private FragmentRutaRepartidorBinding binding;
    private RutaRepartidorViewModel viewModel;
    private MapLibreMap map;
    private Style style;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean mapReady;
    private boolean permissionDenied;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    permissionDenied = false;
                    startLocationUpdates();
                    activateLocationComponent();
                } else {
                    permissionDenied = true;
                    binding.textRouteStatus.setText(R.string.route_location_permission_denied);
                    Snackbar.make(binding.getRoot(), R.string.route_location_permission_denied,
                            Snackbar.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRutaRepartidorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RutaRepartidorViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        int pedidoId = getArguments() != null ? getArguments().getInt("pedidoId", -1) : -1;
        viewModel.loadPedido(pedidoId);

        viewModel.getDestinationState().observe(getViewLifecycleOwner(), this::handleDestinationState);
        viewModel.getRouteState().observe(getViewLifecycleOwner(), this::handleRouteState);

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(mapLibreMap -> {
            map = mapLibreMap;
            String styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=" + BuildConfig.MAPTILER_KEY;
            map.setStyle(new Style.Builder().fromUri(styleUrl), loadedStyle -> {
                style = loadedStyle;
                mapReady = true;
                addDestinationIcon();
                if (hasLocationPermission()) {
                    activateLocationComponent();
                }
                redrawDestination();
                UiState<OsrmRouteResponse> currentRoute = viewModel.getRouteState().getValue();
                if (currentRoute != null && currentRoute.getStatus() == UiState.Status.SUCCESS) {
                    drawRoute(currentRoute.getData());
                }
            });
        });

        if (hasLocationPermission()) {
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void handleDestinationState(UiState<PedidoDetalleDto> state) {
        if (state == null || binding == null) return;
        if (state.getStatus() == UiState.Status.ERROR) {
            binding.textRouteStatus.setText(state.getError());
        } else if (state.getStatus() == UiState.Status.SUCCESS) {
            binding.textRouteStatus.setText(R.string.route_waiting_location);
            redrawDestination();
        }
    }

    private void handleRouteState(UiState<OsrmRouteResponse> state) {
        if (state == null || binding == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.textRouteStatus.setText(R.string.route_calculating);
                break;
            case SUCCESS:
                binding.textRouteStatus.setText(R.string.route_ready);
                drawRoute(state.getData());
                break;
            case ERROR:
                binding.textRouteStatus.setText(state.getError());
                break;
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission() || fusedLocationClient == null) return;
        if (locationCallback == null) {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                    .setMinUpdateIntervalMillis(1_000L)
                    .build();
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    if (result.getLastLocation() != null) {
                        android.location.Location location = result.getLastLocation();
                        viewModel.onDriverLocationChanged(location.getLatitude(), location.getLongitude());
                    }
                }
            };
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && viewModel != null) {
                viewModel.onDriverLocationChanged(location.getLatitude(), location.getLongitude());
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void activateLocationComponent() {
        if (!mapReady || style == null || !hasLocationPermission()) return;
        org.maplibre.android.location.LocationComponent locationComponent = map.getLocationComponent();
        LocationComponentActivationOptions options = LocationComponentActivationOptions
                .builder(requireContext(), style)
                .useDefaultLocationEngine(true)
                .build();
        locationComponent.activateLocationComponent(options);
        locationComponent.setLocationComponentEnabled(true);
        locationComponent.setRenderMode(RenderMode.COMPASS);
        locationComponent.setCameraMode(CameraMode.TRACKING);
    }

    private void addDestinationIcon() {
        if (style != null && style.getImage(DESTINATION_ICON_ID) == null) {
            Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pin_mapa);
            int size = Math.max(48, drawable != null ? drawable.getIntrinsicWidth() : 48);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            if (drawable != null) {
                drawable.setBounds(0, 0, size, size);
                drawable.draw(new Canvas(bitmap));
            }
            style.addImage(DESTINATION_ICON_ID, bitmap);
        }
    }

    private void redrawDestination() {
        if (!mapReady || style == null || viewModel == null) return;
        UiState<PedidoDetalleDto> state = viewModel.getDestinationState().getValue();
        if (state == null || state.getStatus() != UiState.Status.SUCCESS || state.getData() == null) return;
        PedidoDetalleDto pedido = state.getData();
        if (pedido.getLatitudDestino() == null || pedido.getLongitudDestino() == null) return;

        Point point = Point.fromLngLat(pedido.getLongitudDestino(), pedido.getLatitudDestino());
        GeoJsonSource source = style.getSourceAs(DESTINATION_SOURCE_ID);
        if (source == null) {
            style.addSource(new GeoJsonSource(DESTINATION_SOURCE_ID, Feature.fromGeometry(point)));
        } else {
            source.setGeoJson(Feature.fromGeometry(point));
        }
        if (style.getLayer(DESTINATION_LAYER_ID) == null) {
            style.addLayer(new SymbolLayer(DESTINATION_LAYER_ID, DESTINATION_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconImage(DESTINATION_ICON_ID),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconSize(0.8f)));
        }
        map.setCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(pedido.getLatitudDestino(), pedido.getLongitudDestino()))
                .zoom(14.0)
                .build());
    }

    private void drawRoute(OsrmRouteResponse response) {
        if (!mapReady || style == null || response == null) return;
        List<List<Double>> coordinates = response.getRouteCoordinates();
        if (coordinates.size() < 2) return;

        List<Point> points = new ArrayList<>();
        for (List<Double> coordinate : coordinates) {
            if (coordinate != null && coordinate.size() >= 2) {
                points.add(Point.fromLngLat(coordinate.get(0), coordinate.get(1)));
            }
        }
        if (points.size() < 2) return;

        Feature feature = Feature.fromGeometry(LineString.fromLngLats(points));
        GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);
        if (source == null) {
            style.addSource(new GeoJsonSource(ROUTE_SOURCE_ID, feature));
        } else {
            source.setGeoJson(feature);
        }
        if (style.getLayer(ROUTE_LAYER_ID) == null) {
            style.addLayer(new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)
                    .withProperties(
                            PropertyFactory.lineColor("#1565C0"),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)));
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
        if (binding != null) binding.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (binding != null) binding.mapView.onStop();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (binding != null) binding.mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) binding.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        if (binding != null) binding.mapView.onDestroy();
        map = null;
        style = null;
        binding = null;
        super.onDestroyView();
    }
}

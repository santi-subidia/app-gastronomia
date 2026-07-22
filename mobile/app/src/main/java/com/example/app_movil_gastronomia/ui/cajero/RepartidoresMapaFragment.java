package com.example.app_movil_gastronomia.ui.cajero;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.BuildConfig;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.databinding.FragmentRepartidoresMapaBinding;

import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;
import org.maplibre.android.maps.Style;
import org.maplibre.android.style.layers.PropertyFactory;
import org.maplibre.android.style.layers.SymbolLayer;
import org.maplibre.android.style.sources.GeoJsonSource;
import org.maplibre.geojson.Feature;
import org.maplibre.geojson.FeatureCollection;
import org.maplibre.geojson.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** MapLibre screen that lets the cashier monitor every delivery driver. */
@AndroidEntryPoint
public class RepartidoresMapaFragment extends Fragment {

    private static final String SOURCE_ID = "cashier-drivers";
    private static final String LAYER_ID = "cashier-driver-markers";
    private static final String ICON_ID = "cashier-driver-marker-icon";

    private FragmentRepartidoresMapaBinding binding;
    private RepartidoresMapaViewModel viewModel;
    private RepartidoresMapaAdapter adapter;
    @Nullable
    private MapLibreMap map;
    @Nullable
    private Style style;
    private boolean mapReady;
    private boolean cameraPositioned;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRepartidoresMapaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RepartidoresMapaViewModel.class);
        adapter = new RepartidoresMapaAdapter();
        binding.recyclerViewRepartidores.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewRepartidores.setAdapter(adapter);
        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
        viewModel.getState().observe(getViewLifecycleOwner(), this::handleState);

        binding.mapView.onCreate(savedInstanceState);
        binding.mapView.getMapAsync(mapLibreMap -> {
            if (binding == null) return;
            map = mapLibreMap;
            String styleUrl = "https://api.maptiler.com/maps/streets-v2/style.json?key=" + BuildConfig.MAPTILER_KEY;
            map.setStyle(new Style.Builder().fromUri(styleUrl), loadedStyle -> {
                if (binding == null) return;
                style = loadedStyle;
                mapReady = true;
                addDriverIcon();
                redrawMap(currentDrivers());
                
                // Observe default location once map is ready
                viewModel.getDefaultLocation().observe(getViewLifecycleOwner(), coords -> {
                    if (!cameraPositioned && coords != null && map != null) {
                        cameraPositioned = true;
                        map.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(coords.latitud, coords.longitud))
                                .zoom(12.0)
                                .build());
                    }
                });
            });
        });
    }

    private void handleState(UiState<List<RepartidorUiModel>> state) {
        if (state == null || binding == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressBar.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                List<RepartidorUiModel> drivers = state.getData();
                boolean empty = drivers == null || drivers.isEmpty();
                binding.recyclerViewRepartidores.setVisibility(empty ? View.GONE : View.VISIBLE);
                binding.textEmptyRepartidores.setVisibility(empty ? View.VISIBLE : View.GONE);
                adapter.submitList(drivers);
                redrawMap(drivers);
                break;
            case ERROR:
                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerViewRepartidores.setVisibility(View.GONE);
                binding.textEmptyRepartidores.setVisibility(View.GONE);
                binding.textError.setVisibility(View.VISIBLE);
                binding.textError.setText(state.getError() != null
                        ? state.getError() : getString(R.string.error_generic));
                binding.buttonRetry.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Nullable
    private List<RepartidorUiModel> currentDrivers() {
        if (viewModel == null || viewModel.getState().getValue() == null) return null;
        UiState<List<RepartidorUiModel>> current = viewModel.getState().getValue();
        return current.getStatus() == UiState.Status.SUCCESS ? current.getData() : null;
    }

    private void addDriverIcon() {
        if (style == null || style.getImage(ICON_ID) != null) return;
        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_pin_mapa);
        int size = Math.max(48, drawable != null ? drawable.getIntrinsicWidth() : 48);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if (drawable != null) {
            drawable.setBounds(0, 0, size, size);
            drawable.draw(new Canvas(bitmap));
        }
        style.addImage(ICON_ID, bitmap);
    }

    private void redrawMap(@Nullable List<RepartidorUiModel> drivers) {
        if (!mapReady || style == null || map == null) return;

        List<Feature> features = new ArrayList<>();
        RepartidorUiModel firstLocatedDriver = null;
        List<RepartidorUiModel> safeDrivers = drivers == null
                ? Collections.emptyList() : drivers;
        for (RepartidorUiModel driver : safeDrivers) {
            if (!driver.hasLocation()) continue;
            if (firstLocatedDriver == null) firstLocatedDriver = driver;
            Feature feature = Feature.fromGeometry(Point.fromLngLat(
                    driver.getLongitud(), driver.getLatitud()));
            feature.addNumberProperty("repartidorId", driver.getId());
            feature.addStringProperty("estado", driver.getEstado());
            features.add(feature);
        }

        FeatureCollection collection = FeatureCollection.fromFeatures(
                features.toArray(new Feature[0]));
        GeoJsonSource source = style.getSourceAs(SOURCE_ID);
        if (source == null) {
            style.addSource(new GeoJsonSource(SOURCE_ID, collection));
        } else {
            source.setGeoJson(collection);
        }
        if (style.getLayer(LAYER_ID) == null) {
            style.addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID)
                    .withProperties(
                            PropertyFactory.iconImage(ICON_ID),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconSize(0.8f)));
        }

        if (!cameraPositioned && firstLocatedDriver != null) {
            cameraPositioned = true;
            map.setCameraPosition(new CameraPosition.Builder()
                    .target(new LatLng(firstLocatedDriver.getLatitud(), firstLocatedDriver.getLongitud()))
                    .zoom(12.0)
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
        if (binding != null) binding.mapView.onDestroy();
        map = null;
        style = null;
        mapReady = false;
        cameraPositioned = false;
        binding = null;
        super.onDestroyView();
    }
}

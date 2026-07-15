package com.example.app_movil_gastronomia.ui.repartidor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Backs {@link MapaFragment}. Provides three concerns:
 *
 * <ol>
 *   <li><b>Active deliveries list</b>: bridges the
 *       {@link PedidoRepository} state and filters to the
 *       {@code "En Camino"} subset so the rider only sees the pedidos
 *       they are actually on the road for.</li>
 *   <li><b>Current GPS position</b>: subscribes to
 *       {@link LocationManager#GPS_PROVIDER} updates and caches the
 *       latest fix so it can be broadcast on demand and at the
 *       configured auto-send cadence.</li>
 *   <li><b>Periodic position broadcasting</b>: when auto-send is
 *       enabled, every {@link #AUTO_SEND_INTERVAL_MS} the cached GPS
 *       position is pushed to the hub via
 *       {@link SignalRService#enviarPosicion(int, double, double)}.
 *       The Handler runs on the main looper; the hub call itself is
 *       fire-and-forget, so blocking the main thread for the duration
 *       of a {@code conn.send()} is acceptable.</li>
 * </ol>
 *
 * <p>Observer lifecycle: every {@code observeForever} registration is
 * tracked through {@link #observerRegistrationCount} and torn down in
 * {@link #onCleared()}. The auto-send Handler and the
 * {@link LocationListener} are also detached there so a configuration
 * change (e.g. rotation) does not leak callbacks.</p>
 */
@HiltViewModel
public class MapaViewModel extends ViewModel {

    private static final String TAG = "MapaViewModel";

    /** Auto-send cadence. 8 seconds, per the spec. */
    public static final long AUTO_SEND_INTERVAL_MS = 8_000L;

    /** Minimum interval between GPS fixes from the provider. */
    private static final long GPS_MIN_TIME_MS = 2_000L;

    /** Minimum distance between GPS fixes, in meters. */
    private static final float GPS_MIN_DISTANCE_M = 0f;

    private final Context appContext;
    private final PedidoRepository pedidoRepository;
    private final TokenManager tokenManager;

    /**
     * Optional SignalR transport. Injected when Hilt has wired
     * {@link SignalRService}; may be {@code null} in defensive
     * configurations. When null the VM still drives the UI and the
     * auto-send Handler, but {@code sendPosition()} short-circuits.
     */
    @Nullable
    private final SignalRService signalRService;

    // ---- State ----
    private final MutableLiveData<UiState<List<PedidoResumenDto>>> pedidosState =
            new MutableLiveData<>(UiState.loading());

    private final MutableLiveData<String> gpsState = new MutableLiveData<>();
    private final MutableLiveData<String> lastSentState = new MutableLiveData<>();

    /** True while the periodic Handler is queued. */
    private final MutableLiveData<Boolean> autoSendEnabled = new MutableLiveData<>(false);

    /** Snapshot of the most recent GPS fix. Null until the first fix arrives. */
    @Nullable
    private volatile Location lastKnownLocation;

    /** Pre-formatted time of the last successful hub send, or null. */
    @Nullable
    private volatile String lastSentAt;

    // ---- Handlers / listeners ----
    private final Handler autoSendHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private LocationManager locationManager;
    @Nullable
    private LocationListener locationListener;

    // ---- Observer bookkeeping ----
    private final Observer<UiState<List<PedidoResumenDto>>> repositoryObserver;
    private final Observer<PosicionGPSActualizadaMessage> posicionGpsObserver;
    private final Observer<Boolean> connectedObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public MapaViewModel(@NonNull @dagger.hilt.android.qualifiers.ApplicationContext Context appContext,
                         @NonNull PedidoRepository pedidoRepository,
                         @Nullable SignalRService signalRService,
                         @NonNull TokenManager tokenManager) {
        this.appContext = appContext.getApplicationContext();
        this.pedidoRepository = pedidoRepository;
        this.signalRService = signalRService;
        this.tokenManager = tokenManager;

        // ---- REST: bridge the repository state and filter to "En Camino" ----
        this.repositoryObserver = state -> {
            if (state == null) {
                pedidosState.setValue(UiState.loading());
                return;
            }
            switch (state.getStatus()) {
                case LOADING:
                    pedidosState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    pedidosState.setValue(UiState.success(filterEnCamino(state.getData())));
                    break;
                case ERROR:
                    pedidosState.setValue(UiState.error(state.getError()));
                    break;
            }
        };
        pedidoRepository.getPedidosState().observeForever(repositoryObserver);
        observerRegistrationCount.incrementAndGet();
        pedidoRepository.getPedidos();

        // Initial GPS label until the first fix arrives.
        gpsState.setValue(formatGpsWaiting());

        // ---- SignalR: react to pushed events ----
        if (signalRService != null) {
            this.posicionGpsObserver = msg -> {
                // Echo of our own broadcast (or a peer's). Update the
                // gpsState so the rider can see their last broadcast
                // coordinates reflected back.
                if (msg == null) return;
                int myId = tokenManager.getUserId();
                if (myId > 0 && msg.getRepartidorId() != myId) return;
                gpsState.setValue(formatCoords(msg.getLatitud(), msg.getLongitud()));
            };
            signalRService.getPosicionGPSActualizada().observeForever(posicionGpsObserver);
            observerRegistrationCount.incrementAndGet();

            this.connectedObserver = isConnected -> {
                // No-op for now: auto-send lifecycle is driven by the
                // user toggling the switch, not by hub connectivity.
            };
            signalRService.getConnected().observeForever(connectedObserver);
            observerRegistrationCount.incrementAndGet();
        } else {
            this.posicionGpsObserver = null;
            this.connectedObserver = null;
        }
    }

    // ------------------------------------------------------------------
    // Public state accessors
    // ------------------------------------------------------------------

    public LiveData<UiState<List<PedidoResumenDto>>> getPedidosState() {
        return pedidosState;
    }

    public LiveData<String> getGpsState() {
        return gpsState;
    }

    public LiveData<String> getLastSentState() {
        return lastSentState;
    }

    public LiveData<Boolean> getAutoSendEnabled() {
        return autoSendEnabled;
    }

    /** Reloads the pedido list. Wired to the retry button. */
    public void retry() {
        pedidoRepository.getPedidos();
    }

    // ------------------------------------------------------------------
    // GPS subscription
    // ------------------------------------------------------------------

    /**
     * Starts listening to GPS updates. Idempotent. Should be called
     * from the fragment after the user has granted
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     */
    @SuppressLint("MissingPermission")
    public void startGpsUpdates() {
        if (!hasLocationPermission()) {
            gpsState.setValue(formatGpsUnavailable());
            return;
        }
        LocationManager lm = getLocationManager();
        if (lm == null) {
            gpsState.setValue(formatGpsUnavailable());
            return;
        }
        if (locationListener != null) {
            // Already registered; do nothing.
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                lastKnownLocation = location;
                gpsState.setValue(formatCoords(location.getLatitude(), location.getLongitude()));
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                // No-op: keep showing last known coordinates if any.
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                gpsState.setValue(formatGpsUnavailable());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Deprecated; no-op.
            }
        };

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        GPS_MIN_TIME_MS,
                        GPS_MIN_DISTANCE_M,
                        locationListener,
                        Looper.getMainLooper());
            } else {
                gpsState.setValue(formatGpsUnavailable());
                return;
            }
        } catch (IllegalArgumentException | SecurityException e) {
            Log.w(TAG, "Failed to subscribe to GPS_PROVIDER", e);
            gpsState.setValue(formatGpsUnavailable());
            return;
        }

        // Seed with last known position so the UI shows something
        // useful while we wait for the first fresh fix.
        @SuppressLint("MissingPermission")
        Location cached = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (cached != null) {
            lastKnownLocation = cached;
            gpsState.setValue(formatCoords(cached.getLatitude(), cached.getLongitude()));
        }
    }

    /**
     * Stops listening to GPS updates. Idempotent. Called when the
     * user revokes permission or the fragment goes away.
     */
    public void stopGpsUpdates() {
        LocationManager lm = locationManager;
        LocationListener listener = locationListener;
        if (lm != null && listener != null) {
            try {
                lm.removeUpdates(listener);
            } catch (SecurityException e) {
                Log.w(TAG, "Failed to remove GPS listener", e);
            }
        }
        locationListener = null;
    }

    // ------------------------------------------------------------------
    // Auto-send toggle
    // ------------------------------------------------------------------

    /**
     * Enables or disables the periodic position broadcast. When
     * enabled, the first {@link #sendPositionNow()} is fired
     * immediately and the Handler re-arms itself every
     * {@link #AUTO_SEND_INTERVAL_MS} ms.
     */
    public void setAutoSendEnabled(boolean enabled) {
        Boolean previous = autoSendEnabled.getValue();
        if (previous != null && previous == enabled) {
            return;
        }
        autoSendEnabled.setValue(enabled);
        if (enabled) {
            sendPositionNow();
            scheduleNextAutoSend();
        } else {
            cancelAutoSend();
        }
    }

    /**
     * Manually triggers a single position broadcast. Wires to the
     * "Enviar Posición Ahora" button.
     */
    public void sendPositionNow() {
        if (signalRService == null) {
            lastSentState.setValue(formatLastSentError());
            return;
        }
        int userId = tokenManager.getUserId();
        if (userId <= 0) {
            lastSentState.setValue(formatLastSentError());
            return;
        }
        Location loc = lastKnownLocation;
        if (loc == null) {
            // Try the last known system position one more time so
            // the rider is not punished for the cold-start of GPS.
            LocationManager lm = getLocationManager();
            if (lm != null && hasLocationPermission()) {
                try {
                    @SuppressLint("MissingPermission")
                    Location cached = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (cached != null) {
                        loc = cached;
                        lastKnownLocation = cached;
                    }
                } catch (SecurityException ignored) {
                    // Permission revoked between checks; fall through.
                }
            }
        }
        if (loc == null) {
            lastSentState.setValue(formatLastSentError());
            return;
        }

        try {
            signalRService.enviarPosicion(userId, loc.getLatitude(), loc.getLongitude());
            lastSentAt = nowFormatted();
            lastSentState.setValue(formatLastSent(lastSentAt));
        } catch (Exception e) {
            Log.w(TAG, "enviarPosicion() threw", e);
            lastSentState.setValue(formatLastSentError());
        }
    }

    private void scheduleNextAutoSend() {
        autoSendHandler.removeCallbacks(autoSendRunnable);
        autoSendHandler.postDelayed(autoSendRunnable, AUTO_SEND_INTERVAL_MS);
    }

    private void cancelAutoSend() {
        autoSendHandler.removeCallbacks(autoSendRunnable);
    }

    private final Runnable autoSendRunnable = new Runnable() {
        @Override
        public void run() {
            Boolean enabled = autoSendEnabled.getValue();
            if (enabled == null || !enabled) {
                return;
            }
            sendPositionNow();
            scheduleNextAutoSend();
        }
    };

    // ------------------------------------------------------------------
    // Filter helper
    // ------------------------------------------------------------------

    /**
     * Keeps only pedidos in the {@code "En Camino"} estado. Mirrors
     * {@code RepartidorHomeFragment.filterEnCamino} so both screens
     * show the same subset.
     */
    static List<PedidoResumenDto> filterEnCamino(List<PedidoResumenDto> pedidos) {
        List<PedidoResumenDto> result = new ArrayList<>();
        if (pedidos == null) {
            return result;
        }
        for (PedidoResumenDto p : pedidos) {
            if (isEnCamino(p.getEstado())) {
                result.add(p);
            }
        }
        return result;
    }

    static boolean isEnCamino(String estado) {
        if (estado == null) return false;
        String normalized = estado.trim().toLowerCase();
        return "encamino".equals(normalized) || "en camino".equals(normalized);
    }

    // ------------------------------------------------------------------
    // Formatting helpers
    // ------------------------------------------------------------------

    static String formatCoords(double lat, double lng) {
        return String.format(Locale.US, "%.6f, %.6f", lat, lng);
    }

    static String formatLastSent(String time) {
        return time;
    }

    static String formatLastSentError() {
        return "--:--:--";
    }

    static String formatGpsWaiting() {
        return GPS_STATE_WAITING;
    }

    static String formatGpsUnavailable() {
        return GPS_STATE_UNAVAILABLE;
    }

    /** Marker for "no fix yet". The Fragment converts this to the localized "Esperando GPS..." string. */
    static final String GPS_STATE_WAITING = "—";

    /** Marker for "provider disabled / hardware missing". The Fragment converts this to the localized "GPS no disponible" string. */
    static final String GPS_STATE_UNAVAILABLE = "OFF";

    private static String nowFormatted() {
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return fmt.format(new Date());
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    @Nullable
    private LocationManager getLocationManager() {
        if (locationManager == null) {
            Object svc = appContext.getSystemService(Context.LOCATION_SERVICE);
            if (svc instanceof LocationManager) {
                locationManager = (LocationManager) svc;
            }
        }
        return locationManager;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelAutoSend();
        stopGpsUpdates();

        pedidoRepository.getPedidosState().removeObserver(repositoryObserver);
        if (signalRService != null) {
            if (posicionGpsObserver != null) {
                signalRService.getPosicionGPSActualizada().removeObserver(posicionGpsObserver);
            }
            if (connectedObserver != null) {
                signalRService.getConnected().removeObserver(connectedObserver);
            }
        }
    }

    /** Test-only diagnostic: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

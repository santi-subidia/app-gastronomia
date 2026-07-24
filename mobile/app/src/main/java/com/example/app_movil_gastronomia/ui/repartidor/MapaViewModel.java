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

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MapaViewModel extends ViewModel {

    private static final String TAG = "MapaViewModel";

    public static final long AUTO_SEND_INTERVAL_MS = 8_000L;

    private static final long GPS_MIN_TIME_MS = 2_000L;

    private static final float GPS_MIN_DISTANCE_M = 0f;

    private final Context appContext;
    private final PedidoRepository pedidoRepository;
    private final TokenManager tokenManager;

    @Nullable
    private final SignalRService signalRService;

    private final MutableLiveData<UiState<List<PedidoResumenDto>>> pedidosState =
            new MutableLiveData<>(UiState.loading());

    private final MutableLiveData<String> gpsState = new MutableLiveData<>();
    private final MutableLiveData<String> lastSentState = new MutableLiveData<>();

    private final MutableLiveData<Boolean> autoSendEnabled = new MutableLiveData<>(false);

    @Nullable
    private volatile Location lastKnownLocation;

    @Nullable
    private volatile String lastSentAt;

    private final Handler autoSendHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private LocationManager locationManager;
    @Nullable
    private LocationListener locationListener;

    private final Observer<UiState<List<PedidoResumenDto>>> repositoryObserver;
    private final Observer<PosicionGPSActualizadaMessage> posicionGpsObserver;

    @Inject
    public MapaViewModel(@NonNull @dagger.hilt.android.qualifiers.ApplicationContext Context appContext,
                         @NonNull PedidoRepository pedidoRepository,
                         @Nullable SignalRService signalRService,
                         @NonNull TokenManager tokenManager) {
        this.appContext = appContext.getApplicationContext();
        this.pedidoRepository = pedidoRepository;
        this.signalRService = signalRService;
        this.tokenManager = tokenManager;

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
        pedidoRepository.getPedidos();

        gpsState.setValue(formatGpsWaiting());

        if (signalRService != null) {
            this.posicionGpsObserver = msg -> {
                if (msg == null) return;
                int myId = tokenManager.getUserId();
                if (myId > 0 && msg.getRepartidorId() != myId) return;
                gpsState.setValue(formatCoords(msg.getLatitud(), msg.getLongitud()));
            };
            signalRService.getPosicionGPSActualizada().observeForever(posicionGpsObserver);

        } else {
            this.posicionGpsObserver = null;
        }
    }


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

    public void retry() {
        pedidoRepository.getPedidos();
    }

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
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                gpsState.setValue(formatGpsUnavailable());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
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

        @SuppressLint("MissingPermission")
        Location cached = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (cached != null) {
            lastKnownLocation = cached;
            gpsState.setValue(formatCoords(cached.getLatitude(), cached.getLongitude()));
        }
    }

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

    static final String GPS_STATE_WAITING = "—";

    static final String GPS_STATE_UNAVAILABLE = "OFF";

    private static String nowFormatted() {
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return fmt.format(new Date());
    }


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
        }
    }

}

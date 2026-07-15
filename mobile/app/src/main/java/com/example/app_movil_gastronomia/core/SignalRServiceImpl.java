package com.example.app_movil_gastronomia.core;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.BuildConfig;
import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.TransportEnum;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

/**
 * Singleton implementation of {@link SignalRService} backed by the
 * Microsoft SignalR Java client 8.x. Owns a single {@link HubConnection}
 * for the lifetime of the process and reuses it across the
 * connect/disconnect cycles of a logged-in session.
 *
 * <p>Threading: all {@link MutableLiveData} updates go through
 * {@code postValue}, so hub callbacks running on SignalR's internal
 * thread pool are safe. Reconnect attempts are scheduled on a
 * single-threaded executor and can be cancelled by
 * {@link #disconnect()}.</p>
 *
 * <p>Spec SR-SVC-001.</p>
 */
@Singleton
public class SignalRServiceImpl implements SignalRService {

    private static final String TAG = "SignalRServiceImpl";

    /** Seconds to wait before attempting a reconnection after an unexpected close. */
    private static final long RECONNECT_DELAY_SECONDS = 5L;

    private final String hubUrl;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SignalR-Reconnect");
                t.setDaemon(true);
                return t;
            });

    private final MutableLiveData<NuevoPedidoMessage> _nuevoPedido = new MutableLiveData<>();
    private final MutableLiveData<EstadoCambiadoMessage> _estadoCambiado = new MutableLiveData<>();
    private final MutableLiveData<RepartidorAsignadoMessage> _repartidorAsignado = new MutableLiveData<>();
    private final MutableLiveData<DemoraRegistradaMessage> _demoraRegistrada = new MutableLiveData<>();
    private final MutableLiveData<PosicionGPSActualizadaMessage> _posicionGPSActualizada = new MutableLiveData<>();
    private final MutableLiveData<PedidoFinalizadoMessage> _pedidoFinalizado = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _connected = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>();

    /**
     * Set to {@code true} by {@link #disconnect()} so the
     * {@code onClosed} callback knows the close was intentional and
     * must NOT schedule a reconnect.
     */
    private final AtomicBoolean disconnectedByUser = new AtomicBoolean(false);

    @Nullable
    private volatile HubConnection hubConnection;

    @Nullable
    private volatile String currentToken;

    @Nullable
    private volatile ScheduledFuture<?> pendingReconnect;

    @Inject
    public SignalRServiceImpl() {
        // BuildConfig.API_BASE_URL is the Retrofit base URL (e.g.
        // "https://tu-api-url/"). The SignalR hub lives under
        // hubs/logistica on the same origin.
        this.hubUrl = BuildConfig.API_BASE_URL + "hubs/logistica";
    }

    // ------------------------------------------------------------------
    // Connection lifecycle
    // ------------------------------------------------------------------

    @Override
    public void connect(String token) {
        if (hubConnection != null) {
            Log.d(TAG, "connect() ignored — connection already established");
            return;
        }
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "connect() called with empty token");
            _error.postValue("Token vacío, no se puede conectar al hub");
            return;
        }

        disconnectedByUser.set(false);
        this.currentToken = token;

        final String tokenSnapshot = token;
        try {
            hubConnection = HubConnectionBuilder.create(hubUrl)
                    .withAccessTokenProvider(
                            Single.fromCallable(() -> currentToken == null ? tokenSnapshot : currentToken))
                    .withTransport(TransportEnum.WEBSOCKETS)
                    .build();

            registerHandlers(hubConnection);
            registerLifecycleCallbacks(hubConnection);

            hubConnection.start().blockingAwait();
            _connected.postValue(true);
            Log.d(TAG, "Connected to " + hubUrl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to hub at " + hubUrl, e);
            _error.postValue("No se pudo conectar al hub: " + e.getMessage());
            // null out the half-built connection so the next
            // connect() call can try again from scratch.
            hubConnection = null;
            scheduleReconnect();
        }
    }

    @Override
    public void disconnect() {
        disconnectedByUser.set(true);
        cancelPendingReconnect();

        HubConnection conn = hubConnection;
        hubConnection = null;
        currentToken = null;

        if (conn != null) {
            try {
                conn.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error while stopping hub connection", e);
            }
        }
        _connected.postValue(false);
    }

    // ------------------------------------------------------------------
    // Hub method invocations
    // ------------------------------------------------------------------

    @Override
    public void unirseACocina() {
        invokeOnConnection("UnirseACocina");
    }

    @Override
    public void unirseAPedido(int pedidoId) {
        invokeOnConnection("UnirseAPedido", pedidoId);
    }

    @Override
    public void salirDePedido(int pedidoId) {
        invokeOnConnection("SalirDePedido", pedidoId);
    }

    @Override
    public void enviarPosicion(int repartidorId, double lat, double lng) {
        HubConnection conn = hubConnection;
        if (conn == null) {
            Log.w(TAG, "enviarPosicion() ignored — hub not connected");
            return;
        }
        try {
            // send() is fire-and-forget: no return value needed for
            // a high-frequency GPS broadcast.
            conn.send("EnviarPosicionGPS", repartidorId, lat, lng);
        } catch (Exception e) {
            Log.e(TAG, "Error sending GPS position", e);
        }
    }

    // ------------------------------------------------------------------
    // LiveData getters
    // ------------------------------------------------------------------

    @Override
    public LiveData<NuevoPedidoMessage> getNuevoPedido() {
        return _nuevoPedido;
    }

    @Override
    public LiveData<EstadoCambiadoMessage> getEstadoCambiado() {
        return _estadoCambiado;
    }

    @Override
    public LiveData<RepartidorAsignadoMessage> getRepartidorAsignado() {
        return _repartidorAsignado;
    }

    @Override
    public LiveData<DemoraRegistradaMessage> getDemoraRegistrada() {
        return _demoraRegistrada;
    }

    @Override
    public LiveData<PosicionGPSActualizadaMessage> getPosicionGPSActualizada() {
        return _posicionGPSActualizada;
    }

    @Override
    public LiveData<PedidoFinalizadoMessage> getPedidoFinalizado() {
        return _pedidoFinalizado;
    }

    @Override
    public LiveData<Boolean> getConnected() {
        return _connected;
    }

    @Override
    public LiveData<String> getError() {
        return _error;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Wires the six server-pushed events we listen for. Each
     * handler is a single-line post into its own
     * {@link MutableLiveData}; observers registered at app
     * startup will see every emission.
     */
    private void registerHandlers(HubConnection conn) {
        conn.on("NuevoPedido",
                (NuevoPedidoMessage msg) -> _nuevoPedido.postValue(msg),
                NuevoPedidoMessage.class);

        conn.on("EstadoCambiado",
                (EstadoCambiadoMessage msg) -> _estadoCambiado.postValue(msg),
                EstadoCambiadoMessage.class);

        conn.on("RepartidorAsignado",
                (RepartidorAsignadoMessage msg) -> _repartidorAsignado.postValue(msg),
                RepartidorAsignadoMessage.class);

        conn.on("DemoraRegistrada",
                (DemoraRegistradaMessage msg) -> _demoraRegistrada.postValue(msg),
                DemoraRegistradaMessage.class);

        conn.on("PosicionGPSActualizada",
                (PosicionGPSActualizadaMessage msg) -> _posicionGPSActualizada.postValue(msg),
                PosicionGPSActualizadaMessage.class);

        conn.on("PedidoFinalizado",
                (PedidoFinalizadoMessage msg) -> _pedidoFinalizado.postValue(msg),
                PedidoFinalizadoMessage.class);
    }

    /**
     * Reacts to connection state changes. {@code onClosed} fires on
     * any drop (network loss, server restart, explicit stop). If
     * the drop was NOT initiated by {@link #disconnect()}, schedule
     * a reconnect so the user does not have to re-login.
     */
    private void registerLifecycleCallbacks(HubConnection conn) {
        conn.onClosed(error -> {
            _connected.postValue(false);
            hubConnection = null;
            if (error != null) {
                Log.w(TAG, "Hub connection closed with error", error);
            } else {
                Log.d(TAG, "Hub connection closed");
            }
            if (!disconnectedByUser.get()) {
                scheduleReconnect();
            }
        });
    }

    private void invokeOnConnection(String method, Object... args) {
        HubConnection conn = hubConnection;
        if (conn == null) {
            Log.w(TAG, method + "() ignored — hub not connected");
            return;
        }
        try {
            if (args.length == 0) {
                conn.invoke(method).blockingAwait();
            } else {
                conn.invoke(method, args).blockingAwait();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error invoking " + method, e);
        }
    }

    private void scheduleReconnect() {
        if (disconnectedByUser.get()) {
            return;
        }
        cancelPendingReconnect();
        final String tokenSnapshot = currentToken;
        if (tokenSnapshot == null) {
            Log.d(TAG, "Reconnect skipped — no token available");
            return;
        }
        pendingReconnect = scheduler.schedule(
                () -> connect(tokenSnapshot),
                RECONNECT_DELAY_SECONDS,
                TimeUnit.SECONDS);
    }

    private void cancelPendingReconnect() {
        ScheduledFuture<?> pending = pendingReconnect;
        if (pending != null) {
            pending.cancel(false);
            pendingReconnect = null;
        }
    }
}

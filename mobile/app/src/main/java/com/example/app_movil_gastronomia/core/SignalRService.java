package com.example.app_movil_gastronomia.core;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;

/**
 * Real-time transport for the {@code logistica} SignalR hub. Wraps a
 * long-lived {@code HubConnection} and exposes server-pushed events
 * as {@link LiveData} so ViewModels can observe them with the same
 * lifecycle-aware pattern they use for REST results.
 *
 * <p>Spec SR-SVC-001: one {@code MutableLiveData} per event type,
 * initialized once and never reallocated, so observers that
 * register at app start keep receiving emissions across reconnects.</p>
 */
public interface SignalRService {

    /**
     * Opens the hub connection using the given JWT. Idempotent: if
     * a connection is already open it returns without doing
     * anything. Reconnects are scheduled automatically on
     * disconnect (see {@code onClosed}).
     *
     * @param token the JWT obtained from the login flow
     */
    void connect(String token);

    /** Stops the hub connection and cancels any pending reconnect. */
    void disconnect();

    /**
     * Joins the {@code Cocina} group so the caller starts receiving
     * cocina-scoped events ({@code NuevoPedido}, {@code EstadoCambiado},
     * etc.). Should be called once after {@link #connect(String)}.
     */
    void unirseACocina();

    /**
     * Joins the per-pedido group so the caller starts receiving
     * events scoped to that pedido (e.g. {@code RepartidorAsignado}
     * while the rider is en route).
     *
     * @param pedidoId the pedido to subscribe to
     */
    void unirseAPedido(int pedidoId);

    /**
     * Leaves the per-pedido group. Safe to call even if the caller
     * never joined it.
     *
     * @param pedidoId the pedido to unsubscribe from
     */
    void salirDePedido(int pedidoId);

    /**
     * Sends the repartidor's current GPS position to the hub. Fire
     * and forget — no response is expected.
     *
     * @param repartidorId the repartidor broadcasting the position
     * @param lat          latitude in decimal degrees
     * @param lng          longitude in decimal degrees
     */
    void enviarPosicion(int repartidorId, double lat, double lng);

    /** Emitted when a brand-new pedido is created anywhere in the system. */
    LiveData<NuevoPedidoMessage> getNuevoPedido();

    /** Emitted when a pedido (or one of its details) changes state. */
    LiveData<EstadoCambiadoMessage> getEstadoCambiado();

    /** Emitted when a repartidor is assigned to a pedido. */
    LiveData<RepartidorAsignadoMessage> getRepartidorAsignado();

    /** Emitted when Cocina registers a demora against a pedido. */
    LiveData<DemoraRegistradaMessage> getDemoraRegistrada();

    /** Emitted when a repartidor broadcasts their GPS position. */
    LiveData<PosicionGPSActualizadaMessage> getPosicionGPSActualizada();

    /** Emitted when a pedido reaches a terminal state. */
    LiveData<PedidoFinalizadoMessage> getPedidoFinalizado();

    /** Emits {@code true} while the hub connection is open, {@code false} otherwise. */
    LiveData<Boolean> getConnected();

    /** Emits a human-readable error message on connection failure. */
    LiveData<String> getError();
}

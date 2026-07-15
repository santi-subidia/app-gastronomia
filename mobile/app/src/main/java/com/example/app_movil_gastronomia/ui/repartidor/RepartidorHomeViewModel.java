package com.example.app_movil_gastronomia.ui.repartidor;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Backs {@link RepartidorHomeFragment}. Loads the pedido list via
 * {@link PedidoRepository}, exposes a VM-owned {@link LiveData} for the
 * fragment to observe, and wires the {@link SignalRService} so the UI
 * reacts in real time to three things:
 *
 * <ul>
 *   <li>{@code RepartidorAsignadoMessage} → reload the pedido list so
 *       a new assignment shows up immediately.</li>
 *   <li>{@code PedidoFinalizadoMessage} → surface a toast on the
 *       fragment via a separate {@link LiveData} stream, so the
 *       fragment can render transient UI without polluting the list
 *       state.</li>
 *   <li>{@code getConnected()} flips true (reconnect) → re-join the
 *       per-pedido SignalR group for every pedido currently
 *       {@code "En Camino"} in the visible list, so a transient hub
 *       reconnect after a network blip does not silently drop the
 *       rider's subscription to those events.</li>
 * </ul>
 *
 * <p>Observer lifecycle: every {@code observeForever} registration is
 * tracked through {@link #observerRegistrationCount} and torn down in
 * {@link #onCleared()}. The REST observer and SignalR observers are
 * independent and may register zero, one, two, or three of the SignalR
 * ones depending on whether the SignalR service is available.</p>
 */
@HiltViewModel
public class RepartidorHomeViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;

    /**
     * Optional SignalR transport. Injected when Hilt has wired
     * {@link SignalRService}; may be {@code null} in defensive
     * configurations (e.g. tests, or future modularization where the
     * realtime feature is split out). When null the VM degrades to
     * pure REST polling.
     */
    @Nullable
    private final SignalRService signalRService;

    private final MutableLiveData<UiState<List<PedidoResumenDto>>> state = new MutableLiveData<>();
    private final MutableLiveData<PedidoFinalizadoMessage> pedidoFinalizado = new MutableLiveData<>();

    private final Observer<UiState<List<PedidoResumenDto>>> repositoryObserver;
    private final Observer<RepartidorAsignadoMessage> repartidorAsignadoObserver;
    private final Observer<PedidoFinalizadoMessage> pedidoFinalizadoObserver;
    private final Observer<Boolean> connectedObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public RepartidorHomeViewModel(PedidoRepository pedidoRepository,
                                   @Nullable SignalRService signalRService) {
        this.pedidoRepository = pedidoRepository;
        this.signalRService = signalRService;

        // ---- REST: bridge the repository state into the VM-owned LiveData ----
        this.repositoryObserver = state::setValue;
        pedidoRepository.getPedidosState().observeForever(repositoryObserver);
        observerRegistrationCount.incrementAndGet();
        pedidoRepository.getPedidos();

        // ---- SignalR: react to realtime events ----
        if (signalRService != null) {
            // New assignment → reload the list so the new pedido shows up.
            this.repartidorAsignadoObserver = msg -> pedidoRepository.getPedidos();
            signalRService.getRepartidorAsignado().observeForever(repartidorAsignadoObserver);
            observerRegistrationCount.incrementAndGet();

            // Pedido reached a terminal state → forward to the fragment for a toast.
            // The fragment can choose to also call retry() if it wants to drop the
            // finalizado row from the list immediately.
            this.pedidoFinalizadoObserver = pedidoFinalizado::setValue;
            signalRService.getPedidoFinalizado().observeForever(pedidoFinalizadoObserver);
            observerRegistrationCount.incrementAndGet();

            // Re-join every "En Camino" pedido group on (re)connect so a hub
            // reconnect after a network blip does not silently drop our
            // subscription to per-pedido events.
            this.connectedObserver = isConnected -> {
                if (isConnected != null && isConnected) {
                    rejoinActivePedidoGroups();
                }
            };
            signalRService.getConnected().observeForever(connectedObserver);
            observerRegistrationCount.incrementAndGet();
        } else {
            this.repartidorAsignadoObserver = null;
            this.pedidoFinalizadoObserver = null;
            this.connectedObserver = null;
        }
    }

    public LiveData<UiState<List<PedidoResumenDto>>> getRepartidorState() {
        return state;
    }

    /**
     * Stream of {@link PedidoFinalizadoMessage} events pushed by the
     * hub. The fragment observes this and shows a transient snackbar
     * per emission. The list state itself is not modified here.
     */
    public LiveData<PedidoFinalizadoMessage> getPedidoFinalizado() {
        return pedidoFinalizado;
    }

    /** Reloads the pedido list. Wired to the retry button. */
    public void retry() {
        pedidoRepository.getPedidos();
    }

    /**
     * Iterates the currently displayed pedido list and re-joins the
     * per-pedido SignalR group for every pedido in the
     * {@code "En Camino"} state. Defensive: if the list has not been
     * loaded yet (no SUCCESS emitted) this is a no-op.
     */
    private void rejoinActivePedidoGroups() {
        if (signalRService == null) return;
        UiState<List<PedidoResumenDto>> current = state.getValue();
        if (current == null || current.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        List<PedidoResumenDto> pedidos = current.getData();
        if (pedidos == null) return;
        for (PedidoResumenDto p : pedidos) {
            if (isEnCamino(p.getEstado())) {
                signalRService.unirseAPedido(p.getId());
            }
        }
    }

    /**
     * Case-insensitive matcher for the "En Camino" estado. Accepts
     * both the canonical API value ({@code "EnCamino"}) and the
     * human-friendly display label ({@code "En Camino"}), the same
     * way {@code CocinaHomeFragment} does for its estados.
     */
    static boolean isEnCamino(String estado) {
        if (estado == null) return false;
        String normalized = estado.trim().toLowerCase();
        return "encamino".equals(normalized) || "en camino".equals(normalized);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidosState().removeObserver(repositoryObserver);
        if (signalRService != null) {
            if (repartidorAsignadoObserver != null) {
                signalRService.getRepartidorAsignado().removeObserver(repartidorAsignadoObserver);
            }
            if (pedidoFinalizadoObserver != null) {
                signalRService.getPedidoFinalizado().removeObserver(pedidoFinalizadoObserver);
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

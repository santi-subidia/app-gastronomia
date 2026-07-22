package com.example.app_movil_gastronomia.ui.repartidor;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;

import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;

import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Backs {@link RepartidorHomeFragment}. Loads the pedido list via
 * {@link PedidoRepository}, exposes a VM-owned {@link LiveData} for the
 * fragment to observe, and wires the {@link SignalRService} so the UI
 * reacts in real time to three things:
 *
 * <ul>
 *   <li>{@code RepartidorAsignadoMessage} â†’ reload the pedido list so
 *       a new assignment shows up immediately.</li>
 *   <li>{@code PedidoFinalizadoMessage} â†’ surface a toast on the
 *       fragment via a separate {@link LiveData} stream, so the
 *       fragment can render transient UI without polluting the list
 *       state.</li>
 *   <li>{@code getConnected()} flips true (reconnect) â†’ re-join the
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
    private final UsuarioRepository usuarioRepository;
    private final TokenManager tokenManager;

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


    @Inject
    public RepartidorHomeViewModel(PedidoRepository pedidoRepository,
                                   UsuarioRepository usuarioRepository,
                                   TokenManager tokenManager,
                                   @Nullable SignalRService signalRService) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.tokenManager = tokenManager;
        this.signalRService = signalRService;

        this.repositoryObserver = state::setValue;
        pedidoRepository.getPedidosState().observeForever(repositoryObserver);
        fetchPedidos();

        if (signalRService != null) {
            this.repartidorAsignadoObserver = msg -> fetchPedidos();
            signalRService.getRepartidorAsignado().observeForever(repartidorAsignadoObserver);

            this.pedidoFinalizadoObserver = pedidoFinalizado::setValue;
            signalRService.getPedidoFinalizado().observeForever(pedidoFinalizadoObserver);

            this.connectedObserver = isConnected -> {
                if (isConnected != null && isConnected) {
                    rejoinActivePedidoGroups();
                }
            };
            signalRService.getConnected().observeForever(connectedObserver);
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

    public LiveData<UiState<UsuarioDto>> getUpdateState() {
        return usuarioRepository.getUpdateState();
    }

    public void updateDisponibilidad(boolean disponible) {
        int userId = tokenManager.getUserId();
        if (userId > 0) {
            usuarioRepository.updateDisponibilidad(userId, disponible);
        }
    }

        private void fetchPedidos() {
        int userId = tokenManager.getUserId();
        if (userId > 0) {
            pedidoRepository.getPedidosPorRepartidor(userId);
        } else {
            pedidoRepository.getPedidos();
        }
    }

    public void retry() {
        fetchPedidos();
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
            if (isEnCaminoOrListo(p.getEstado())) {
                signalRService.unirseAPedido(p.getId());
            }
        }
    }

    static boolean isEnCaminoOrListo(String estado) {
        if (estado == null) return false;
        String normalized = estado.trim().toLowerCase();
        return "encamino".equals(normalized) || "en camino".equals(normalized) 
            || "listoparetirar".equals(normalized) || "listo para retirar".equals(normalized);
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

}


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
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;

import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;

import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RepartidorHomeViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TokenManager tokenManager;

    @Nullable
    private final SignalRService signalRService;

    private final MutableLiveData<UiState<List<PedidoResumenDto>>> state = new MutableLiveData<>();
    private final MutableLiveData<PedidoFinalizadoMessage> pedidoFinalizado = new MutableLiveData<>();

    private final Observer<UiState<List<PedidoResumenDto>>> repositoryObserver;
    private final Observer<RepartidorAsignadoMessage> repartidorAsignadoObserver;
    private final Observer<EstadoCambiadoMessage> estadoCambiadoObserver;
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

            this.estadoCambiadoObserver = msg -> fetchPedidos();
            signalRService.getEstadoCambiado().observeForever(estadoCambiadoObserver);

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
            this.estadoCambiadoObserver = null;
            this.pedidoFinalizadoObserver = null;
            this.connectedObserver = null;
        }
    }

    public LiveData<UiState<List<PedidoResumenDto>>> getRepartidorState() {
        return state;
    }

    public LiveData<PedidoFinalizadoMessage> getPedidoFinalizado() {
        return pedidoFinalizado;
    }

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

    static boolean isVisibleOnDashboard(String estado) {
        if (isEnCaminoOrListo(estado)) {
            return true;
        }

        if (estado == null) {
            return false;
        }

        String normalized = estado.trim().toLowerCase();
        return "entregado".equals(normalized);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidosState().removeObserver(repositoryObserver);
        if (signalRService != null) {
            if (repartidorAsignadoObserver != null) {
                signalRService.getRepartidorAsignado().removeObserver(repartidorAsignadoObserver);
            }
            if (estadoCambiadoObserver != null) {
                signalRService.getEstadoCambiado().removeObserver(estadoCambiadoObserver);
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


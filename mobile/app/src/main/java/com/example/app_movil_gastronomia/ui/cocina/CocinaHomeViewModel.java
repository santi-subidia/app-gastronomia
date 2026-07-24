package com.example.app_movil_gastronomia.ui.cocina;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CocinaHomeViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    @Nullable
    private final SignalRService signalRService;
    private final MutableLiveData<UiState<List<PedidoResumenDto>>> state = new MutableLiveData<>();
    private final Observer<UiState<List<PedidoResumenDto>>> repositoryObserver;
    private final Observer<NuevoPedidoMessage> nuevoPedidoObserver;
    private final Observer<com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage> estadoCambiadoObserver;
    private final Observer<Boolean> connectedObserver;


    @Inject
    public CocinaHomeViewModel(PedidoRepository pedidoRepository,
                               @Nullable SignalRService signalRService) {
        this.pedidoRepository = pedidoRepository;
        this.signalRService = signalRService;

        this.repositoryObserver = state::setValue;
        pedidoRepository.getPedidosState().observeForever(repositoryObserver);
        pedidoRepository.getPedidos();

        if (signalRService != null) {
            this.nuevoPedidoObserver = msg -> pedidoRepository.getPedidos();
            signalRService.getNuevoPedido().observeForever(nuevoPedidoObserver);

            this.estadoCambiadoObserver = msg -> pedidoRepository.getPedidos();
            signalRService.getEstadoCambiado().observeForever(estadoCambiadoObserver);

            this.connectedObserver = isConnected -> {
                if (isConnected != null && isConnected) {
                    signalRService.unirseACocina();
                }
            };
            signalRService.getConnected().observeForever(connectedObserver);
        } else {
            this.nuevoPedidoObserver = null;
            this.estadoCambiadoObserver = null;
            this.connectedObserver = null;
        }
    }

    public LiveData<UiState<List<PedidoResumenDto>>> getCocinaState() {
        return state;
    }

    public void retry() {
        pedidoRepository.getPedidos();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidosState().removeObserver(repositoryObserver);
        if (signalRService != null) {
            if (nuevoPedidoObserver != null) {
                signalRService.getNuevoPedido().removeObserver(nuevoPedidoObserver);
            }
            if (estadoCambiadoObserver != null) {
                signalRService.getEstadoCambiado().removeObserver(estadoCambiadoObserver);
            }
            if (connectedObserver != null) {
                signalRService.getConnected().removeObserver(connectedObserver);
            }
        }
    }

}

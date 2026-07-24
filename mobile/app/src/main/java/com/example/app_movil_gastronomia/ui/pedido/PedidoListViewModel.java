package com.example.app_movil_gastronomia.ui.pedido;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PedidoListViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    @Nullable
    private final SignalRService signalRService;
    
    private final MutableLiveData<UiState<List<PedidoResumenDto>>> state = new MutableLiveData<>();

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    private Observer<UiState<List<PedidoResumenDto>>> activeObserver;
    private LiveData<UiState<List<PedidoResumenDto>>> activeSource;
    private EstadoPedidoEnum currentFilter = null;
    
    private final Observer<NuevoPedidoMessage> nuevoPedidoObserver;
    private final Observer<EstadoCambiadoMessage> estadoCambiadoObserver;

    @Inject
    public PedidoListViewModel(PedidoRepository pedidoRepository, @Nullable SignalRService signalRService) {
        this.pedidoRepository = pedidoRepository;
        this.signalRService = signalRService;
        
        if (signalRService != null) {
            this.nuevoPedidoObserver = msg -> retry();
            this.estadoCambiadoObserver = msg -> retry();
            
            signalRService.getNuevoPedido().observeForever(nuevoPedidoObserver);
            signalRService.getEstadoCambiado().observeForever(estadoCambiadoObserver);
        } else {
            this.nuevoPedidoObserver = null;
            this.estadoCambiadoObserver = null;
        }

        switchSource(null);
    }

    public LiveData<UiState<List<PedidoResumenDto>>> getPedidoListState() {
        return state;
    }

    public void filterByEstado(EstadoPedidoEnum estado) {
        if (estado == currentFilter) {
            return;
        }
        switchSource(estado);
    }

    public void retry() {
        switchSource(currentFilter);
    }

    private void switchSource(EstadoPedidoEnum filter) {
        currentFilter = filter;

        if (activeSource != null && activeObserver != null) {
            activeSource.removeObserver(activeObserver);
            activeObserver = null;
            activeSource = null;
        }

        activeObserver = state::setValue;

        if (filter == null) {
            activeSource = pedidoRepository.getPedidosState();
            pedidoRepository.getPedidos();
        } else {
            activeSource = pedidoRepository.getByEstadoState();
            pedidoRepository.getByEstado(filter);
        }

        activeSource.observeForever(activeObserver);
        observerRegistrationCount.incrementAndGet();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (activeSource != null && activeObserver != null) {
            activeSource.removeObserver(activeObserver);
        }
        if (signalRService != null) {
            if (nuevoPedidoObserver != null) {
                signalRService.getNuevoPedido().removeObserver(nuevoPedidoObserver);
            }
            if (estadoCambiadoObserver != null) {
                signalRService.getEstadoCambiado().removeObserver(estadoCambiadoObserver);
            }
        }
    }

    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

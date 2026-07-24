package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CajeroHomeViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    private final CajaRepository cajaRepository;
    @Nullable
    private final SignalRService signalRService;

    private final MutableLiveData<UiState<Integer>> activePedidosState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Boolean>> cajaState = new MutableLiveData<>();

    private final Observer<UiState<List<PedidoResumenDto>>> pedidosRepositoryObserver;
    private final Observer<UiState<List<CajaDto>>> cajasRepositoryObserver;
    
    private final Observer<NuevoPedidoMessage> nuevoPedidoObserver;
    private final Observer<EstadoCambiadoMessage> estadoCambiadoObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public CajeroHomeViewModel(PedidoRepository pedidoRepository,
                               CajaRepository cajaRepository,
                               @Nullable SignalRService signalRService) {
        this.pedidoRepository = pedidoRepository;
        this.cajaRepository = cajaRepository;
        this.signalRService = signalRService;

        if (signalRService != null) {
            this.nuevoPedidoObserver = msg -> pedidoRepository.getPedidos();
            this.estadoCambiadoObserver = msg -> pedidoRepository.getPedidos();
            signalRService.getNuevoPedido().observeForever(nuevoPedidoObserver);
            signalRService.getEstadoCambiado().observeForever(estadoCambiadoObserver);
        } else {
            this.nuevoPedidoObserver = null;
            this.estadoCambiadoObserver = null;
        }

        this.pedidosRepositoryObserver = upstream -> {
            if (upstream == null) return;
            switch (upstream.getStatus()) {
                case LOADING:
                    activePedidosState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    int count = countActive(upstream.getData());
                    activePedidosState.setValue(UiState.success(count));
                    break;
                case ERROR:
                    activePedidosState.setValue(UiState.error(upstream.getError()));
                    break;
            }
        };
        pedidoRepository.getPedidosState().observeForever(pedidosRepositoryObserver);
        observerRegistrationCount.incrementAndGet();
        pedidoRepository.getPedidos();

        this.cajasRepositoryObserver = upstream -> {
            if (upstream == null) return;
            switch (upstream.getStatus()) {
                case LOADING:
                    cajaState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    boolean isOpen = upstream.getData() != null && !upstream.getData().isEmpty();
                    cajaState.setValue(UiState.success(isOpen));
                    break;
                case ERROR:
                    cajaState.setValue(UiState.error(upstream.getError()));
                    break;
            }
        };
        cajaRepository.getCajasState().observeForever(cajasRepositoryObserver);
        observerRegistrationCount.incrementAndGet();
        cajaRepository.getCajas("abierta");
    }

    public LiveData<UiState<Integer>> getActivePedidosState() {
        return activePedidosState;
    }

    public LiveData<UiState<Boolean>> getCajaState() {
        return cajaState;
    }

    public void retry() {
        pedidoRepository.getPedidos();
        cajaRepository.getCajas("abierta");
    }

    static int countActive(List<PedidoResumenDto> pedidos) {
        if (pedidos == null) return 0;
        int count = 0;
        for (PedidoResumenDto p : pedidos) {
            String estado = p.getEstado();
            if (estado == null) continue;
            String normalized = estado.trim().toLowerCase();
            if (!"entregado".equals(normalized) && !"cancelado".equals(normalized)) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidosState().removeObserver(pedidosRepositoryObserver);
        cajaRepository.getCajasState().removeObserver(cajasRepositoryObserver);
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

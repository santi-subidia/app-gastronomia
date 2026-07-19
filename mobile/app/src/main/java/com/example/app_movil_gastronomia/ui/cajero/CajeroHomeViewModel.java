package com.example.app_movil_gastronomia.ui.cajero;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Backs {@link CajeroHomeFragment}. Powers the dashboard with two
 * independent VM-owned {@link LiveData} streams:
 *
 * <ul>
 *   <li>{@link #getActivePedidosState()} — exposes the count of pedidos
 *       whose estado is <em>not</em> a terminal state. Derived
 *       client-side from {@link PedidoRepository#getPedidos()}.</li>
 *   <li>{@link #getCajaState()} — exposes whether there is at least one
 *       caja currently open. Derived from
 *       {@link CajaRepository#getCajas(String)} filtered by
 *       {@code "abierta"}.</li>
 * </ul>
 *
 * <p>The two streams are intentionally separate: they are sourced from
 * two independent REST endpoints with independent lifetimes, and the
 * fragment can render partial data (e.g. active count works but the
 * caja endpoint is down) without one stream masking the other. Both
 * bridge their respective repository {@code *State} streams through an
 * {@code observeForever} observer registered in the constructor and
 * removed in {@link #onCleared()}.</p>
 *
 * <p>Observer lifecycle: every {@code observeForever} registration is
 * tracked through {@link #observerRegistrationCount} and torn down in
 * {@link #onCleared()}.</p>
 */
@HiltViewModel
public class CajeroHomeViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    private final CajaRepository cajaRepository;

    private final MutableLiveData<UiState<Integer>> activePedidosState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Boolean>> cajaState = new MutableLiveData<>();

    private final Observer<UiState<List<PedidoResumenDto>>> pedidosRepositoryObserver;
    private final Observer<UiState<List<CajaDto>>> cajasRepositoryObserver;


    @Inject
    public CajeroHomeViewModel(PedidoRepository pedidoRepository,
                               CajaRepository cajaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.cajaRepository = cajaRepository;

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
        cajaRepository.getCajas("abierta");
    }

    /** VM-owned stream for the active pedidos count. */
    public LiveData<UiState<Integer>> getActivePedidosState() {
        return activePedidosState;
    }

    /** VM-owned stream for whether any caja is currently open. */
    public LiveData<UiState<Boolean>> getCajaState() {
        return cajaState;
    }

    /** Reloads both streams. Wired to the retry button. */
    public void retry() {
        pedidoRepository.getPedidos();
        cajaRepository.getCajas("abierta");
    }

    /**
     * Counts pedidos whose estado is <em>not</em> one of the two
     * terminal values the spec calls out explicitly:
     * {@code "Entregado"} and {@code "Cancelado"}. The match is
     * case-insensitive and trims whitespace so servers that ever
     * return a human-friendly label (e.g. {@code "Entregado "}) are
     * still treated correctly.
     */
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
    }

}

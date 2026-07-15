package com.example.app_movil_gastronomia.ui.pedido;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the pedido detail screen.
 *
 * <p>Bridges three repository LiveData sources into VM-owned state:
 * the detail itself, the cambiarEstado PATCH result, and the
 * asignarRepartidor PATCH result. Each observer is registered once in the
 * constructor and removed in {@link #onCleared()}.</p>
 */
@HiltViewModel
public class PedidoDetailViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;

    private final MutableLiveData<UiState<PedidoDetalleDto>> detailState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> cambiarEstadoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> asignarRepartidorState = new MutableLiveData<>();

    private final Observer<UiState<PedidoDetalleDto>> detailObserver;
    private final Observer<UiState<PedidoDetalleDto>> cambiarEstadoObserver;
    private final Observer<UiState<PedidoDetalleDto>> asignarRepartidorObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public PedidoDetailViewModel(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detailObserver = detailState::setValue;
        this.cambiarEstadoObserver = cambiarEstadoState::setValue;
        this.asignarRepartidorObserver = asignarRepartidorState::setValue;

        pedidoRepository.getPedidoState().observeForever(detailObserver);
        pedidoRepository.getCambiarEstadoState().observeForever(cambiarEstadoObserver);
        pedidoRepository.getAsignarRepartidorState().observeForever(asignarRepartidorObserver);

        observerRegistrationCount.addAndGet(3);
    }

    public LiveData<UiState<PedidoDetalleDto>> getDetailState() {
        return detailState;
    }

    public LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState() {
        return cambiarEstadoState;
    }

    public LiveData<UiState<PedidoDetalleDto>> getAsignarRepartidorState() {
        return asignarRepartidorState;
    }

    public void loadPedido(int id) {
        pedidoRepository.getPedido(id);
    }

    public void cambiarEstado(int id, EstadoPedidoEnum estado) {
        pedidoRepository.cambiarEstado(id, estado);
    }

    public void asignarRepartidor(int id, int repartidorId) {
        pedidoRepository.asignarRepartidor(id, repartidorId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidoState().removeObserver(detailObserver);
        pedidoRepository.getCambiarEstadoState().removeObserver(cambiarEstadoObserver);
        pedidoRepository.getAsignarRepartidorState().removeObserver(asignarRepartidorObserver);
    }

    /** Test-only diagnostic: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

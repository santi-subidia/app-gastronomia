package com.example.app_movil_gastronomia.ui.pedido;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.repository.contract.DemoraRepository;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;
import java.util.List;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class PedidoDetailViewModel extends ViewModel {

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DemoraRepository demoraRepository;

    private final MutableLiveData<UiState<PedidoDetalleDto>> detailState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> cambiarEstadoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> asignarRepartidorState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> reintentarEnCocinaState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<UsuarioDto>>> repartidoresDisponiblesState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<DemoraDto>>> demorasState = new MutableLiveData<>();

    private final Observer<UiState<PedidoDetalleDto>> detailObserver;
    private final Observer<UiState<PedidoDetalleDto>> cambiarEstadoObserver;
    private final Observer<UiState<PedidoDetalleDto>> asignarRepartidorObserver;
    private final Observer<UiState<PedidoDetalleDto>> reintentarEnCocinaObserver;
    private final Observer<UiState<List<UsuarioDto>>> repartidoresDisponiblesObserver;
    private final Observer<UiState<List<DemoraDto>>> demorasObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public PedidoDetailViewModel(PedidoRepository pedidoRepository,
                                 UsuarioRepository usuarioRepository,
                                 DemoraRepository demoraRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.demoraRepository = demoraRepository;
        
        this.detailObserver = detailState::setValue;
        this.cambiarEstadoObserver = cambiarEstadoState::setValue;
        this.asignarRepartidorObserver = asignarRepartidorState::setValue;
        this.reintentarEnCocinaObserver = reintentarEnCocinaState::setValue;
        this.repartidoresDisponiblesObserver = repartidoresDisponiblesState::setValue;
        this.demorasObserver = demorasState::setValue;

        pedidoRepository.getPedidoState().observeForever(detailObserver);
        pedidoRepository.getCambiarEstadoState().observeForever(cambiarEstadoObserver);
        pedidoRepository.getAsignarRepartidorState().observeForever(asignarRepartidorObserver);
        pedidoRepository.getReintentarEnCocinaState().observeForever(reintentarEnCocinaObserver);
        usuarioRepository.getRepartidoresDisponiblesState().observeForever(repartidoresDisponiblesObserver);
        demoraRepository.getDemorasState().observeForever(demorasObserver);

        observerRegistrationCount.addAndGet(6);
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

    public void consumeCambiarEstado() {
        pedidoRepository.resetCambiarEstadoState();
    }

    public void asignarRepartidor(int id, int repartidorId) {
        pedidoRepository.asignarRepartidor(id, repartidorId);
    }

    public void consumeAsignarRepartidor() {
        pedidoRepository.resetAsignarRepartidorState();
    }

    public LiveData<UiState<PedidoDetalleDto>> getReintentarEnCocinaState() {
        return reintentarEnCocinaState;
    }

    public void reintentarEnCocina(int id) {
        pedidoRepository.reintentarEnCocina(id);
    }

    public void consumeReintentarEnCocina() {
        pedidoRepository.resetReintentarEnCocinaState();
    }

    public LiveData<UiState<List<UsuarioDto>>> getRepartidoresDisponiblesState() {
        return repartidoresDisponiblesState;
    }

    public void fetchRepartidoresDisponibles() {
        usuarioRepository.fetchRepartidoresDisponibles();
    }

    public LiveData<UiState<List<DemoraDto>>> getDemorasState() {
        return demorasState;
    }

    public void loadDemoras(int pedidoId) {
        demoraRepository.getDemoras(pedidoId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        pedidoRepository.getPedidoState().removeObserver(detailObserver);
        pedidoRepository.getCambiarEstadoState().removeObserver(cambiarEstadoObserver);
        pedidoRepository.getAsignarRepartidorState().removeObserver(asignarRepartidorObserver);
        pedidoRepository.getReintentarEnCocinaState().removeObserver(reintentarEnCocinaObserver);
        usuarioRepository.getRepartidoresDisponiblesState().removeObserver(repartidoresDisponiblesObserver);
        demoraRepository.getDemorasState().removeObserver(demorasObserver);
    }

    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

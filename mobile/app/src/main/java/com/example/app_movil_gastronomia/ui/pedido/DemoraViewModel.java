package com.example.app_movil_gastronomia.ui.pedido;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.demora.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;
import com.example.app_movil_gastronomia.data.repository.contract.DemoraRepository;


import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * VM for the "Registrar Demora" form. Bridges the single
 * {@link DemoraRepository#getCrearState()} stream into a VM-owned
 * {@link #crearState} LiveData so the fragment can observe a
 * fragment-scoped, lifecycle-safe instance.
 *
 * <p>Follows the {@code CocinaHomeViewModel} pattern: the observer is
 * registered once with {@code observeForever} in the constructor and
 * removed in {@link #onCleared()}.</p>
 *
 * <p>Note: the {@code CrearDemoraRequest} DTO does NOT carry a
 * {@code usuarioId} field by design (see {@code CrearDemoraRequest}
 * docstring) — the server derives the caller from the auth token.
 * Therefore this VM does not need {@code TokenManager}; the auth
 * interceptor attaches the JWT automatically.</p>
 */
@HiltViewModel
public class DemoraViewModel extends ViewModel {

    private final DemoraRepository demoraRepository;

    private final MutableLiveData<UiState<DemoraDto>> crearState = new MutableLiveData<>();

    private final Observer<UiState<DemoraDto>> crearObserver;
    private final LiveData<UiState<DemoraDto>> crearSource;


    @Inject
    public DemoraViewModel(DemoraRepository demoraRepository) {
        this.demoraRepository = demoraRepository;

        this.crearObserver = crearState::setValue;
        this.crearSource = demoraRepository.getCrearState();
        crearSource.observeForever(crearObserver);
    }

    public LiveData<UiState<DemoraDto>> getCrearState() {
        return crearState;
    }

    /**
     * Builds a {@link CrearDemoraRequest} and dispatches it to
     * {@link DemoraRepository#crearDemora(CrearDemoraRequest)}. The
     * result (LOADING / SUCCESS / ERROR) is mirrored on
     * {@link #getCrearState()}.
     *
     * <p>v2: the {@code sector} field was removed from the wire
     * contract — the server derives it from the auth token. Only
     * the minutes of delay and optional observaciones are sent.</p>
     *
     * @param pedidoId      target pedido id
     * @param demoraMinutos positive minutes of delay
     * @param observaciones free-text notes (optional, pass empty string if none)
     */
    public void registrarDemora(
            int pedidoId,
            int demoraMinutos,
            String observaciones
    ) {
        CrearDemoraRequest request = new CrearDemoraRequest(
                pedidoId,
                demoraMinutos,
                observaciones != null ? observaciones : ""
        );
        demoraRepository.crearDemora(request);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (crearSource != null && crearObserver != null) {
            crearSource.removeObserver(crearObserver);
        }
    }

}

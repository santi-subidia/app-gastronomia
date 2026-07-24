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

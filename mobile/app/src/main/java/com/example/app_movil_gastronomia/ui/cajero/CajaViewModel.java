package com.example.app_movil_gastronomia.ui.cajero;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CajaViewModel extends ViewModel {

    private final CajaRepository cajaRepository;

    private final MutableLiveData<UiState<CajaDto>> cajaState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> abrirState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> cerrarState = new MutableLiveData<>();

    private final Observer<UiState<List<CajaDto>>> cajasRepositoryObserver;
    private final Observer<UiState<CajaDto>> abrirRepositoryObserver;
    private final Observer<UiState<CajaDto>> cerrarRepositoryObserver;


    @Inject
    public CajaViewModel(CajaRepository cajaRepository) {
        this.cajaRepository = cajaRepository;

        this.cajasRepositoryObserver = upstream -> {
            if (upstream == null) return;
            switch (upstream.getStatus()) {
                case LOADING:
                    cajaState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    List<CajaDto> list = upstream.getData();
                    CajaDto open = (list != null && !list.isEmpty()) ? list.get(0) : null;
                    cajaState.setValue(UiState.success(open));
                    break;
                case ERROR:
                    cajaState.setValue(UiState.error(upstream.getError()));
                    break;
            }
        };
        cajaRepository.getCajasState().observeForever(cajasRepositoryObserver);

        this.abrirRepositoryObserver = upstream -> {
            if (upstream == null) return;
            abrirState.setValue(upstream);
            if (upstream.getStatus() == UiState.Status.SUCCESS) {
                loadCajaStatus();
            }
        };
        cajaRepository.getAbrirState().observeForever(abrirRepositoryObserver);

        this.cerrarRepositoryObserver = upstream -> {
            if (upstream == null) return;
            cerrarState.setValue(upstream);
            if (upstream.getStatus() == UiState.Status.SUCCESS) {
                loadCajaStatus();
            }
        };
        cajaRepository.getCerrarState().observeForever(cerrarRepositoryObserver);

        loadCajaStatus();
    }

    public LiveData<UiState<CajaDto>> getCajaState() {
        return cajaState;
    }

    public LiveData<UiState<CajaDto>> getAbrirState() {
        return abrirState;
    }

    public LiveData<UiState<CajaDto>> getCerrarState() {
        return cerrarState;
    }

    public void consumeAbrirState() {
        cajaRepository.resetAbrirState();
    }

    public void consumeCerrarState() {
        cajaRepository.resetCerrarState();
    }

    public void loadCajaStatus() {
        cajaRepository.getCajas("abiertas");
    }

    public void retry() {
        loadCajaStatus();
    }

    public void abrirCaja(double montoApertura) {
        AbrirCajaRequest request = new AbrirCajaRequest(montoApertura);
        cajaRepository.abrirCaja(request);
    }

    public void cerrarCaja(CajaDto caja, double montoCierreReal) {
        if (caja == null) return;
        CerrarCajaRequest request = new CerrarCajaRequest(
                caja.getMontoApertura() + caja.getIngresosEfectivo(),
                montoCierreReal
        );
        cajaRepository.cerrarCaja(caja.getId(), request);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cajaRepository.getCajasState().removeObserver(cajasRepositoryObserver);
        cajaRepository.getAbrirState().removeObserver(abrirRepositoryObserver);
        cajaRepository.getCerrarState().removeObserver(cerrarRepositoryObserver);
    }
}

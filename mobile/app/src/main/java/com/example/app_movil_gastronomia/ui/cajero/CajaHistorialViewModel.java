package com.example.app_movil_gastronomia.ui.cajero;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialDetalleDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialResumenDto;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CajaHistorialViewModel extends ViewModel {
    private final CajaRepository repository;
    private final MutableLiveData<UiState<List<CajaHistorialResumenDto>>> historialState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaHistorialDetalleDto>> detalleState = new MutableLiveData<>();
    private final Observer<UiState<List<CajaHistorialResumenDto>>> historialObserver;
    private final Observer<UiState<CajaHistorialDetalleDto>> detalleObserver;

    @Inject
    public CajaHistorialViewModel(CajaRepository repository) {
        this.repository = repository;
        historialObserver = historialState::setValue;
        detalleObserver = detalleState::setValue;
        repository.getHistorialState().observeForever(historialObserver);
        repository.getHistorialDetalleState().observeForever(detalleObserver);
        cargarHistorial();
    }

    public LiveData<UiState<List<CajaHistorialResumenDto>>> getHistorialState() { return historialState; }
    public LiveData<UiState<CajaHistorialDetalleDto>> getDetalleState() { return detalleState; }
    public void cargarHistorial() { repository.getHistorial(); }
    public void cargarDetalle(int cajaId) { repository.getHistorialDetalle(cajaId); }

    @Override
    protected void onCleared() {
        repository.getHistorialState().removeObserver(historialObserver);
        repository.getHistorialDetalleState().removeObserver(detalleObserver);
        super.onCleared();
    }
}

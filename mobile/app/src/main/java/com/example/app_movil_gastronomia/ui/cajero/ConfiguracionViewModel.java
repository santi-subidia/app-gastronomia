package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.ConfiguracionRepository;

import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ConfiguracionViewModel extends ViewModel {

    private final ConfiguracionRepository repository;
    private final CatalogoRepository catalogoRepository;

    private final MutableLiveData<UiState<ConfiguracionDto>> configState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ConfiguracionDto>> saveState = new MutableLiveData<>();

    private final Observer<UiState<ConfiguracionDto>> getConfigObserver;
    private final Observer<UiState<ConfiguracionDto>> crearObserver;
    private final Observer<UiState<ConfiguracionDto>> actualizarObserver;

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);
    private boolean isSaving = false;

    @Inject
    public ConfiguracionViewModel(ConfiguracionRepository repository, CatalogoRepository catalogoRepository) {
        this.repository = repository;
        this.catalogoRepository = catalogoRepository;

        this.getConfigObserver = state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    configState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    configState.setValue(UiState.success(state.getData()));
                    break;
                case ERROR:
                    String error = state.getError();
                    if (isNotFoundMessage(error)) {
                        configState.setValue(UiState.success(null));
                    } else {
                        configState.setValue(UiState.error(error));
                    }
                    break;
            }
        };
        repository.getConfiguracionState().observeForever(getConfigObserver);
        observerRegistrationCount.incrementAndGet();

        this.crearObserver = state -> bridgeSave(state, true);
        repository.getCrearState().observeForever(crearObserver);
        observerRegistrationCount.incrementAndGet();

        this.actualizarObserver = state -> bridgeSave(state, true);
        repository.getActualizarState().observeForever(actualizarObserver);
        observerRegistrationCount.incrementAndGet();

        repository.getConfiguracion();
    }

    public LiveData<UiState<ConfiguracionDto>> getConfigState() {
        return configState;
    }

    public LiveData<UiState<ConfiguracionDto>> getSaveState() {
        return saveState;
    }

    public void clearSaveState() {
        saveState.setValue(null);
    }

    public LiveData<List<CatalogoItemDto>> getMetodosPago() {
        return catalogoRepository.getMetodosPago();
    }

    public int resolveMetodoPagoId(String nombre) {
        if (!catalogoRepository.isReady()) return -1;
        return catalogoRepository.resolveMetodoPagoId(nombre);
    }

    public void loadConfiguracion() {
        repository.getConfiguracion();
    }

    public void saveConfiguracion(ConfiguracionDto dto) {
        if (dto == null) return;
        isSaving = true;
        UiState<ConfiguracionDto> current = configState.getValue();
        if (current != null
                && current.getStatus() == UiState.Status.SUCCESS
                && current.getData() != null) {
            dto.setId(current.getData().getId());
            repository.actualizarConfiguracion(dto);
        } else {
            repository.crearConfiguracion(dto);
        }
    }

    private void bridgeSave(UiState<ConfiguracionDto> state, boolean reloadOnSuccess) {
        if (state == null || !isSaving) return;
        switch (state.getStatus()) {
            case LOADING:
                saveState.setValue(UiState.loading());
                break;
            case SUCCESS:
                isSaving = false;
                saveState.setValue(UiState.success(state.getData()));
                if (reloadOnSuccess) {
                    repository.getConfiguracion();
                }
                break;
            case ERROR:
                isSaving = false;
                saveState.setValue(UiState.error(state.getError()));
                break;
        }
    }

    @VisibleForTesting
    static boolean isNotFoundMessage(String error) {
        if (error == null) return false;
        String lower = error.toLowerCase();
        return lower.contains("no encontrada")
                || lower.contains("not found");
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getConfiguracionState().removeObserver(getConfigObserver);
        repository.getCrearState().removeObserver(crearObserver);
        repository.getActualizarState().removeObserver(actualizarObserver);
    }

    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;
import com.example.app_movil_gastronomia.data.repository.contract.ConfiguracionRepository;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Coordinates the cashier's initial driver list and live GPS updates. */
@HiltViewModel
public class RepartidoresMapaViewModel extends ViewModel {

    private static final String STATUS_OUT_OF_SERVICE = "Fuera de Servicio";
    private static final String STATUS_AVAILABLE = "Disponible";
    private static final String STATUS_BUSY = "Ocupado";

    private final UsuarioRepository usuarioRepository;
    private final ConfiguracionRepository configuracionRepository;
    @Nullable
    private final SignalRService signalRService;
    
    private final MutableLiveData<UiState<List<RepartidorUiModel>>> state =
            new MutableLiveData<>(UiState.loading());
            
    private final MutableLiveData<Coordinates> defaultLocation = new MutableLiveData<>();
            
    private final Map<Integer, Coordinates> latestCoordinates = new HashMap<>();

    private final Observer<UiState<List<UsuarioDto>>> repositoryObserver;
    private final Observer<UiState<ConfiguracionDto>> configObserver;
    @Nullable
    private final Observer<PosicionGPSActualizadaMessage> positionObserver;

    @Inject
    public RepartidoresMapaViewModel(@NonNull UsuarioRepository usuarioRepository,
                                     @NonNull ConfiguracionRepository configuracionRepository,
                                     @Nullable SignalRService signalRService) {
        this.usuarioRepository = usuarioRepository;
        this.configuracionRepository = configuracionRepository;
        this.signalRService = signalRService;

        repositoryObserver = repositoryState -> {
            if (repositoryState == null) {
                state.setValue(UiState.loading());
                return;
            }
            switch (repositoryState.getStatus()) {
                case LOADING:
                    state.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    state.setValue(UiState.success(toUiModels(repositoryState.getData())));
                    break;
                case ERROR:
                    state.setValue(UiState.error(repositoryState.getError()));
                    break;
            }
        };
        usuarioRepository.getRepartidoresState().observeForever(repositoryObserver);

        configObserver = configState -> {
            if (configState != null && configState.getStatus() == UiState.Status.SUCCESS) {
                ConfiguracionDto data = configState.getData();
                if (data != null && data.getLatitudPartida() != null && data.getLongitudPartida() != null) {
                    defaultLocation.setValue(new Coordinates(data.getLatitudPartida(), data.getLongitudPartida()));
                }
            }
        };
        configuracionRepository.getConfiguracionState().observeForever(configObserver);

        if (signalRService != null) {
            positionObserver = message -> {
                if (message == null || !isValidCoordinates(message.getLatitud(), message.getLongitud())) {
                    return;
                }
                latestCoordinates.put(message.getRepartidorId(),
                        new Coordinates(message.getLatitud(), message.getLongitud()));
                UiState<List<RepartidorUiModel>> current = state.getValue();
                if (current != null && current.getStatus() == UiState.Status.SUCCESS) {
                    state.setValue(UiState.success(toUiModelsFromCurrent(current.getData())));
                }
            };
            signalRService.getPosicionGPSActualizada().observeForever(positionObserver);
        } else {
            positionObserver = null;
        }

        fetchRepartidores();
        configuracionRepository.getConfiguracion();
    }

    public LiveData<UiState<List<RepartidorUiModel>>> getState() {
        return state;
    }
    
    public LiveData<Coordinates> getDefaultLocation() {
        return defaultLocation;
    }

    public void retry() {
        fetchRepartidores();
        configuracionRepository.getConfiguracion();
    }

    private void fetchRepartidores() {
        usuarioRepository.fetchRepartidores();
    }

    private List<RepartidorUiModel> toUiModels(@Nullable List<UsuarioDto> drivers) {
        List<RepartidorUiModel> result = new ArrayList<>();
        if (drivers == null) {
            return result;
        }
        for (UsuarioDto driver : drivers) {
            if (driver != null) {
                result.add(toUiModel(driver));
            }
        }
        return result;
    }

    private List<RepartidorUiModel> toUiModelsFromCurrent(@Nullable List<RepartidorUiModel> current) {
        List<RepartidorUiModel> result = new ArrayList<>();
        if (current == null) {
            return result;
        }
        for (RepartidorUiModel driver : current) {
            Coordinates coordinates = latestCoordinates.get(driver.getId());
            if (coordinates == null && driver.hasLocation()) {
                result.add(driver);
            } else {
                result.add(new RepartidorUiModel(
                        driver.getId(), driver.getNombre(), driver.getEstado(),
                        driver.isFueraDeServicio(),
                        coordinates != null ? coordinates.latitud : null,
                        coordinates != null ? coordinates.longitud : null));
            }
        }
        return result;
    }

    private RepartidorUiModel toUiModel(UsuarioDto driver) {
        Coordinates coordinates = latestCoordinates.get(driver.getId());
        String status;
        if (driver.isFueraDeServicio()) {
            status = STATUS_OUT_OF_SERVICE;
        } else if (driver.isDisponible()) {
            status = STATUS_AVAILABLE;
        } else {
            status = STATUS_BUSY;
        }
        return new RepartidorUiModel(
                driver.getId(),
                driver.getUsuarioNombre(),
                status,
                driver.isFueraDeServicio(),
                coordinates != null ? coordinates.latitud : null,
                coordinates != null ? coordinates.longitud : null);
    }

    static boolean isValidCoordinates(double latitud, double longitud) {
        return latitud >= -90.0 && latitud <= 90.0
                && longitud >= -180.0 && longitud <= 180.0;
    }

    @Override
    protected void onCleared() {
        usuarioRepository.getRepartidoresState().removeObserver(repositoryObserver);
        configuracionRepository.getConfiguracionState().removeObserver(configObserver);
        if (signalRService != null && positionObserver != null) {
            signalRService.getPosicionGPSActualizada().removeObserver(positionObserver);
        }
        super.onCleared();
    }

    public static final class Coordinates {
        public final double latitud;
        public final double longitud;

        Coordinates(double latitud, double longitud) {
            this.latitud = latitud;
            this.longitud = longitud;
        }
    }
}

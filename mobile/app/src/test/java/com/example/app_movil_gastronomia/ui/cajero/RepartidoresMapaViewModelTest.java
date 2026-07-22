package com.example.app_movil_gastronomia.ui.cajero;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;
import com.example.app_movil_gastronomia.data.dto.usuario.UpdateUserRequest;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RepartidoresMapaViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void initialLoadKeepsAvailableAndOutOfServiceDriversVisible() {
        FakeUsuarioRepository repository = new FakeUsuarioRepository();
        repository.drivers = Arrays.asList(driver(1, true, false), driver(2, false, true));

        RepartidoresMapaViewModel viewModel =
                new RepartidoresMapaViewModel(repository, null);

        UiState<List<RepartidorUiModel>> state = viewModel.getState().getValue();
        assertEquals(UiState.Status.SUCCESS, state.getStatus());
        assertEquals(2, state.getData().size());
        assertEquals("Fuera de Servicio", state.getData().get(1).getEstado());
    }

    @Test
    public void livePositionUpdatesOnlyTheMatchingDriver() {
        FakeUsuarioRepository repository = new FakeUsuarioRepository();
        repository.drivers = Arrays.asList(driver(1, true, false), driver(2, true, false));
        FakeSignalRService signalR = new FakeSignalRService();
        RepartidoresMapaViewModel viewModel =
                new RepartidoresMapaViewModel(repository, signalR);

        PosicionGPSActualizadaMessage message = new PosicionGPSActualizadaMessage();
        message.setRepartidorId(2);
        message.setLatitud(-34.6037);
        message.setLongitud(-58.3816);
        signalR.position.setValue(message);

        List<RepartidorUiModel> result = viewModel.getState().getValue().getData();
        assertFalse(result.get(0).hasLocation());
        assertTrue(result.get(1).hasLocation());
        assertEquals(-34.6037, result.get(1).getLatitud(), 0.000001);
    }

    @Test
    public void invalidLiveCoordinatesAreIgnored() {
        FakeUsuarioRepository repository = new FakeUsuarioRepository();
        repository.drivers = Collections.singletonList(driver(1, true, false));
        FakeSignalRService signalR = new FakeSignalRService();
        RepartidoresMapaViewModel viewModel =
                new RepartidoresMapaViewModel(repository, signalR);

        PosicionGPSActualizadaMessage message = new PosicionGPSActualizadaMessage();
        message.setRepartidorId(1);
        message.setLatitud(91.0);
        message.setLongitud(0.0);
        signalR.position.setValue(message);

        assertFalse(viewModel.getState().getValue().getData().get(0).hasLocation());
    }

    private static UsuarioDto driver(int id, boolean available, boolean outOfService) {
        UsuarioDto driver = new UsuarioDto();
        driver.setId(id);
        driver.setUsuarioNombre("Driver " + id);
        driver.setDisponible(available);
        driver.setFueraDeServicio(outOfService);
        return driver;
    }

    private static final class FakeUsuarioRepository implements UsuarioRepository {
        private final MutableLiveData<UiState<List<UsuarioDto>>> driversState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<List<UsuarioDto>>> availableState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<UsuarioDto>> updateState = new MutableLiveData<>();
        private List<UsuarioDto> drivers = Collections.emptyList();

        @Override
        public LiveData<UiState<List<UsuarioDto>>> getRepartidoresState() {
            return driversState;
        }

        @Override
        public void fetchRepartidores() {
            driversState.setValue(UiState.success(drivers));
        }

        @Override
        public LiveData<UiState<List<UsuarioDto>>> getRepartidoresDisponiblesState() {
            return availableState;
        }

        @Override
        public void fetchRepartidoresDisponibles() {
        }

        @Override
        public LiveData<UiState<UsuarioDto>> getUpdateState() {
            return updateState;
        }

        @Override
        public void updateDisponibilidad(int id, boolean disponible) {
        }
    }

    private static final class FakeSignalRService implements SignalRService {
        private final MutableLiveData<PosicionGPSActualizadaMessage> position = new MutableLiveData<>();
        private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
        private final MutableLiveData<String> error = new MutableLiveData<>();
        private final MutableLiveData<NuevoPedidoMessage> nuevoPedido = new MutableLiveData<>();
        private final MutableLiveData<EstadoCambiadoMessage> estadoCambiado = new MutableLiveData<>();
        private final MutableLiveData<RepartidorAsignadoMessage> repartidorAsignado = new MutableLiveData<>();
        private final MutableLiveData<DemoraRegistradaMessage> demoraRegistrada = new MutableLiveData<>();
        private final MutableLiveData<PedidoFinalizadoMessage> pedidoFinalizado = new MutableLiveData<>();

        @Override public void connect(String token) { }
        @Override public void disconnect() { }
        @Override public void unirseACocina() { }
        @Override public void unirseAPedido(int pedidoId) { }
        @Override public void salirDePedido(int pedidoId) { }
        @Override public void enviarPosicion(int repartidorId, double lat, double lng) { }
        @Override public LiveData<NuevoPedidoMessage> getNuevoPedido() { return nuevoPedido; }
        @Override public LiveData<EstadoCambiadoMessage> getEstadoCambiado() { return estadoCambiado; }
        @Override public LiveData<RepartidorAsignadoMessage> getRepartidorAsignado() { return repartidorAsignado; }
        @Override public LiveData<DemoraRegistradaMessage> getDemoraRegistrada() { return demoraRegistrada; }
        @Override public LiveData<PosicionGPSActualizadaMessage> getPosicionGPSActualizada() { return position; }
        @Override public LiveData<PedidoFinalizadoMessage> getPedidoFinalizado() { return pedidoFinalizado; }
        @Override public LiveData<Boolean> getConnected() { return connected; }
        @Override public LiveData<String> getError() { return error; }
    }
}

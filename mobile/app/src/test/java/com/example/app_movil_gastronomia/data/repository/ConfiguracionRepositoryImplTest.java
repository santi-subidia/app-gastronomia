package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.ConfiguracionApi;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Spec coverage for the 3 new repository methods. Mirrors the
 * DemoraRepositoryImplTest pattern: FakeApi + FakeCall + InstantTaskExecutorRule.
 *
 * Spec mapping:
 *  - CONF-GET-001:       getConfiguracion emits LOADING -> SUCCESS on 2xx,
 *                        ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - CONF-CREAR-001:     crearConfiguracion emits LOADING -> SUCCESS on 2xx,
 *                        ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - CONF-ACTUALIZAR-001: actualizarConfiguracion emits LOADING -> SUCCESS on 2xx,
 *                        ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - CONF-DI-001:        single-instance state getters; pairwise distinct across
 *                        the 3 states; ViewModel can register once and keep
 *                        receiving emissions.
 */
public class ConfiguracionRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // getConfiguracion
    // ------------------------------------------------------------------

    @Test
    public void getConfiguracionEmitsLoadingThenSuccessOn2xx() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        ConfiguracionDto dto = newConfig(1, "La Esquina", -34.6037, -58.3816);
        api.getConfiguracionResponse = Response.success(dto);
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getConfiguracionState();
        EmissionRecorder<UiState<ConfiguracionDto>> recorder = recordEmissions(state);
        try {
            repo.getConfiguracion();

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getConfiguracionEmitsErrorWithParsedMensajeOn4xx() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.getConfiguracionResponse = errorResponse(404, "{\"mensaje\":\"Configuracion no encontrada\"}");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getConfiguracionState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getConfiguracion();

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Configuracion no encontrada", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getConfiguracionEmitsNetworkErrorOnIOException() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.getConfiguracionFailure = new IOException("boom");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getConfiguracionState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getConfiguracion();

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getConfiguracionStateReturnsSameInstanceAcrossCalls() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.getConfiguracionResponse = Response.success(newConfig(1, "x", null, null));
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> first = repo.getConfiguracionState();
        LiveData<UiState<ConfiguracionDto>> second = repo.getConfiguracionState();
        assertSame(
                "getConfiguracionState() must return the same LiveData instance every call",
                first, second
        );

        repo.getConfiguracion();
        assertSame(first, repo.getConfiguracionState());
    }

    // ------------------------------------------------------------------
    // crearConfiguracion
    // ------------------------------------------------------------------

    @Test
    public void crearConfiguracionEmitsLoadingThenSuccessOn2xx() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        ConfiguracionDto created = newConfig(10, "Buen Sabor", 10.5, 20.75);
        api.crearConfiguracionResponse = Response.success(created);
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getCrearState();
        EmissionRecorder<UiState<ConfiguracionDto>> recorder = recordEmissions(state);
        try {
            repo.crearConfiguracion(newConfig(0, "Buen Sabor", 10.5, 20.75));

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void crearConfiguracionEmitsErrorWithParsedMensajeOn400() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.crearConfiguracionResponse = errorResponse(400, "{\"mensaje\":\"nombreGastronomico es requerido\"}");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getCrearState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearConfiguracion(new ConfiguracionDto());

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("nombreGastronomico es requerido", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearConfiguracionEmitsNetworkErrorOnIOException() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.crearConfiguracionFailure = new IOException("boom");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getCrearState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearConfiguracion(newConfig(0, "x", null, null));

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearConfiguracionForwardsBodyToApi() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.crearConfiguracionResponse = Response.success(newConfig(1, "x", null, null));
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        ConfiguracionDto sent = newConfig(0, "El Bodegon", -31.4, -64.1);
        repo.crearConfiguracion(sent);

        assertSame("repo must forward the exact DTO to the API", sent, api.lastCrearBody);
    }

    @Test
    public void crearStateReturnsSameInstanceAcrossCalls() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.crearConfiguracionResponse = Response.success(newConfig(1, "x", null, null));
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> first = repo.getCrearState();
        LiveData<UiState<ConfiguracionDto>> second = repo.getCrearState();
        assertSame(first, second);

        repo.crearConfiguracion(newConfig(0, "x", null, null));
        assertSame(first, repo.getCrearState());
    }

    // ------------------------------------------------------------------
    // actualizarConfiguracion
    // ------------------------------------------------------------------

    @Test
    public void actualizarConfiguracionEmitsLoadingThenSuccessOn2xx() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        ConfiguracionDto updated = newConfig(1, "El Bodegon", -31.4, -64.1);
        api.actualizarConfiguracionResponse = Response.success(updated);
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getActualizarState();
        EmissionRecorder<UiState<ConfiguracionDto>> recorder = recordEmissions(state);
        try {
            repo.actualizarConfiguracion(updated);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void actualizarConfiguracionEmitsErrorWithParsedMensajeOn4xx() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.actualizarConfiguracionResponse = errorResponse(400, "{\"mensaje\":\"Datos invalidos\"}");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getActualizarState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.actualizarConfiguracion(newConfig(1, "", null, null));

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Datos invalidos", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarConfiguracionEmitsNetworkErrorOnIOException() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.actualizarConfiguracionFailure = new IOException("boom");
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> state = repo.getActualizarState();
        AtomicReference<UiState<ConfiguracionDto>> latest = new AtomicReference<>();
        Observer<UiState<ConfiguracionDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.actualizarConfiguracion(newConfig(1, "x", null, null));

            UiState<ConfiguracionDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarConfiguracionForwardsBodyToApi() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.actualizarConfiguracionResponse = Response.success(newConfig(1, "x", null, null));
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        ConfiguracionDto sent = newConfig(1, "La Esquina", -34.6, -58.4);
        repo.actualizarConfiguracion(sent);

        assertSame("repo must forward the exact DTO to the API", sent, api.lastActualizarBody);
    }

    @Test
    public void actualizarStateReturnsSameInstanceAcrossCalls() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        api.actualizarConfiguracionResponse = Response.success(newConfig(1, "x", null, null));
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> first = repo.getActualizarState();
        LiveData<UiState<ConfiguracionDto>> second = repo.getActualizarState();
        assertSame(first, second);

        repo.actualizarConfiguracion(newConfig(1, "x", null, null));
        assertSame(first, repo.getActualizarState());
    }

    // ------------------------------------------------------------------
    // Pairwise distinct state instances (CONF-DI-001)
    // ------------------------------------------------------------------

    @Test
    public void allThreeStateInstancesArePairwiseDistinct() {
        FakeConfiguracionApi api = new FakeConfiguracionApi();
        ConfiguracionRepositoryImpl repo = new ConfiguracionRepositoryImpl(api);

        LiveData<UiState<ConfiguracionDto>> config = repo.getConfiguracionState();
        LiveData<UiState<ConfiguracionDto>> crear = repo.getCrearState();
        LiveData<UiState<ConfiguracionDto>> actualizar = repo.getActualizarState();

        assertNotEquals(config, crear);
        assertNotEquals(config, actualizar);
        assertNotEquals(crear, actualizar);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static ConfiguracionDto newConfig(int id, String nombre, Double lat, Double lng) {
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setId(id);
        dto.setNombreGastronomico(nombre);
        dto.setLatitudPartida(lat);
        dto.setLongitudPartida(lng);
        return dto;
    }

    private static <T> EmissionRecorder<UiState<T>> recordEmissions(LiveData<UiState<T>> state) {
        List<UiState.Status> seen = new ArrayList<>();
        Observer<UiState<T>> observer = s -> {
            if (s != null) seen.add(s.getStatus());
        };
        state.observeForever(observer);
        return new EmissionRecorder<>(observer, seen);
    }

    static final class EmissionRecorder<S> {
        final Observer<S> observer;
        final List<UiState.Status> seen;

        EmissionRecorder(Observer<S> observer, List<UiState.Status> seen) {
            this.observer = observer;
            this.seen = seen;
        }

        void cleanup(LiveData<S> state) {
            state.removeObserver(observer);
        }
    }

    private static <T> Response<T> errorResponse(int code, String jsonBody) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        ResponseBody body = ResponseBody.create(mediaType, jsonBody);
        return Response.error(code, body);
    }

    // -- Fakes ------------------------------------------------------------

    static final class FakeConfiguracionApi implements ConfiguracionApi {
        // get
        Response<ConfiguracionDto> getConfiguracionResponse;
        Throwable getConfiguracionFailure;
        @Override
        public Call<ConfiguracionDto> getConfiguracion() {
            return new FakeCall<>(getConfiguracionResponse, getConfiguracionFailure);
        }

        // create
        Response<ConfiguracionDto> crearConfiguracionResponse;
        Throwable crearConfiguracionFailure;
        ConfiguracionDto lastCrearBody;
        @Override
        public Call<ConfiguracionDto> crearConfiguracion(ConfiguracionDto body) {
            this.lastCrearBody = body;
            return new FakeCall<>(crearConfiguracionResponse, crearConfiguracionFailure);
        }

        // update
        Response<ConfiguracionDto> actualizarConfiguracionResponse;
        Throwable actualizarConfiguracionFailure;
        ConfiguracionDto lastActualizarBody;
        @Override
        public Call<ConfiguracionDto> actualizarConfiguracion(ConfiguracionDto body) {
            this.lastActualizarBody = body;
            return new FakeCall<>(actualizarConfiguracionResponse, actualizarConfiguracionFailure);
        }
    }

    /**
     * Minimal Call<T> stand-in that synchronously delivers a queued
     * Response or Throwable to the registered Callback.
     */
    static final class FakeCall<T> implements Call<T> {
        private final Response<T> response;
        private final Throwable failure;
        boolean executed;
        boolean canceled;

        FakeCall(Response<T> response, Throwable failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public Response<T> execute() throws IOException {
            executed = true;
            if (failure != null) {
                if (failure instanceof IOException) throw (IOException) failure;
                if (failure instanceof RuntimeException) throw (RuntimeException) failure;
                throw new RuntimeException(failure);
            }
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            executed = true;
            if (failure != null) {
                callback.onFailure(this, failure);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override public boolean isExecuted() { return executed; }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public Call<T> clone() { return new FakeCall<>(response, failure); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public okio.Timeout timeout() { return okio.Timeout.NONE; }
    }
}

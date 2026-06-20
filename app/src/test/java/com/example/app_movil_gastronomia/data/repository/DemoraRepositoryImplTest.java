package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.DemoraApi;
import com.example.app_movil_gastronomia.data.dto.ActualizarDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.CrearDemoraRequest;
import com.example.app_movil_gastronomia.data.dto.DemoraDto;

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
 * Spec coverage for the 4 new repository methods. Mirrors the
 * ProductoRepositoryImplTest pattern: FakeApi + FakeCall + InstantTaskExecutorRule.
 *
 * Spec mapping:
 *  - DEM-LIST-001:      getDemoras emits LOADING -> SUCCESS on 2xx,
 *                       ERROR(parsed mensaje) on 4xx, network error on IOException,
 *                       and captures pedidoId (null + value).
 *  - DEM-CREAR-001:     crearDemora emits LOADING -> SUCCESS on 2xx,
 *                       ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - DEM-ACTUALIZAR-001: actualizarDemora emits LOADING -> SUCCESS on 2xx,
 *                       ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - DEM-ELIMINAR-001:  eliminarDemora emits LOADING -> SUCCESS(null) on 2xx,
 *                       ERROR(parsed mensaje) on 4xx, network error on IOException.
 *  - DEM-DI-001:        single-instance state getters; pairwise distinct across
 *                       the 4 states; ViewModel can register once and keep
 *                       receiving emissions.
 */
public class DemoraRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // getDemoras
    // ------------------------------------------------------------------

    @Test
    public void getDemorasEmitsLoadingThenSuccessOn2xx() {
        FakeDemoraApi api = new FakeDemoraApi();
        List<DemoraDto> serverList = new ArrayList<>();
        DemoraDto d1 = new DemoraDto();
        d1.setId(1);
        d1.setPedidoId(7);
        d1.setDemoraMinutos(10);
        serverList.add(d1);
        api.getDemorasResponse = Response.success(serverList);
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<List<DemoraDto>>> state = repo.getDemorasState();
        EmissionRecorder<UiState<List<DemoraDto>>> recorder = recordEmissions(state);
        try {
            repo.getDemoras(7);

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getDemorasEmitsErrorWithParsedMensajeOn404() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.getDemorasResponse = errorResponse(404, "{\"mensaje\":\"Pedido no encontrado\"}");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<List<DemoraDto>>> state = repo.getDemorasState();
        AtomicReference<UiState<List<DemoraDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<DemoraDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getDemoras(99);

            UiState<List<DemoraDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Pedido no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getDemorasEmitsNetworkErrorOnIOException() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.getDemorasFailure = new IOException("boom");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<List<DemoraDto>>> state = repo.getDemorasState();
        AtomicReference<UiState<List<DemoraDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<DemoraDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getDemoras(1);

            UiState<List<DemoraDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getDemorasStateReturnsSameInstanceAcrossCalls() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.getDemorasResponse = Response.success(new ArrayList<>());
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<List<DemoraDto>>> first = repo.getDemorasState();
        LiveData<UiState<List<DemoraDto>>> second = repo.getDemorasState();
        assertSame(
                "getDemorasState() must return the same LiveData instance every call",
                first, second
        );

        repo.getDemoras(1);
        assertSame(first, repo.getDemorasState());
    }

    @Test
    public void getDemorasForwardsNullPedidoIdToApi() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.getDemorasResponse = Response.success(new ArrayList<>());
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        repo.getDemoras(null);

        assertNull("API must receive null pedidoId when caller passes null",
                api.lastGetDemorasPedidoId);
    }

    @Test
    public void getDemorasForwardsPedidoIdValueToApi() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.getDemorasResponse = Response.success(new ArrayList<>());
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        repo.getDemoras(7);

        assertEquals(Integer.valueOf(7), api.lastGetDemorasPedidoId);
    }

    // ------------------------------------------------------------------
    // crearDemora
    // ------------------------------------------------------------------

    @Test
    public void crearDemoraEmitsLoadingThenSuccessOn201() {
        FakeDemoraApi api = new FakeDemoraApi();
        DemoraDto created = new DemoraDto();
        created.setId(10);
        created.setPedidoId(20);
        created.setDemoraMinutos(15);
        api.crearDemoraResponse = Response.success(created);
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getCrearState();
        EmissionRecorder<UiState<DemoraDto>> recorder = recordEmissions(state);
        try {
            repo.crearDemora(new CrearDemoraRequest(20, 15, "cocina", "sin papas"));

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void crearDemoraEmitsErrorWithParsedMensajeOn400() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.crearDemoraResponse = errorResponse(400, "{\"mensaje\":\"demoraMinutos debe ser positivo\"}");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getCrearState();
        AtomicReference<UiState<DemoraDto>> latest = new AtomicReference<>();
        Observer<UiState<DemoraDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearDemora(new CrearDemoraRequest(20, 0, "cocina", ""));

            UiState<DemoraDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("demoraMinutos debe ser positivo", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearDemoraEmitsNetworkErrorOnIOException() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.crearDemoraFailure = new IOException("boom");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getCrearState();
        AtomicReference<UiState<DemoraDto>> latest = new AtomicReference<>();
        Observer<UiState<DemoraDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearDemora(new CrearDemoraRequest(20, 15, "cocina", ""));

            UiState<DemoraDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearStateReturnsSameInstanceAcrossCalls() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.crearDemoraResponse = Response.success(new DemoraDto());
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> first = repo.getCrearState();
        LiveData<UiState<DemoraDto>> second = repo.getCrearState();
        assertSame(first, second);

        repo.crearDemora(new CrearDemoraRequest(1, 5, "x", ""));
        assertSame(first, repo.getCrearState());
    }

    // ------------------------------------------------------------------
    // actualizarDemora
    // ------------------------------------------------------------------

    @Test
    public void actualizarDemoraEmitsLoadingThenSuccessOn2xx() {
        FakeDemoraApi api = new FakeDemoraApi();
        DemoraDto updated = new DemoraDto();
        updated.setId(5);
        updated.setDemoraMinutos(30);
        api.actualizarDemoraResponse = Response.success(updated);
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getActualizarState();
        EmissionRecorder<UiState<DemoraDto>> recorder = recordEmissions(state);
        try {
            ActualizarDemoraRequest req = new ActualizarDemoraRequest();
            req.setDemoraMinutos(30);
            repo.actualizarDemora(5, req);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void actualizarDemoraEmitsErrorWithParsedMensajeOn404() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.actualizarDemoraResponse = errorResponse(404, "{\"mensaje\":\"Demora no encontrada\"}");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getActualizarState();
        AtomicReference<UiState<DemoraDto>> latest = new AtomicReference<>();
        Observer<UiState<DemoraDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            ActualizarDemoraRequest req = new ActualizarDemoraRequest();
            req.setDemoraMinutos(30);
            repo.actualizarDemora(99, req);

            UiState<DemoraDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Demora no encontrada", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarDemoraEmitsNetworkErrorOnIOException() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.actualizarDemoraFailure = new IOException("boom");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> state = repo.getActualizarState();
        AtomicReference<UiState<DemoraDto>> latest = new AtomicReference<>();
        Observer<UiState<DemoraDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            ActualizarDemoraRequest req = new ActualizarDemoraRequest();
            req.setDemoraMinutos(30);
            repo.actualizarDemora(5, req);

            UiState<DemoraDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarStateReturnsSameInstanceAcrossCalls() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.actualizarDemoraResponse = Response.success(new DemoraDto());
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<DemoraDto>> first = repo.getActualizarState();
        LiveData<UiState<DemoraDto>> second = repo.getActualizarState();
        assertSame(first, second);

        ActualizarDemoraRequest req = new ActualizarDemoraRequest();
        req.setDemoraMinutos(1);
        repo.actualizarDemora(5, req);
        assertSame(first, repo.getActualizarState());
    }

    // ------------------------------------------------------------------
    // eliminarDemora
    // ------------------------------------------------------------------

    @Test
    public void eliminarDemoraEmitsLoadingThenSuccessOn2xx() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.eliminarDemoraResponse = Response.success(null);
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        EmissionRecorder<UiState<Void>> recorder = recordEmissions(state);
        try {
            repo.eliminarDemora(5);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void eliminarDemoraEmitsErrorWithParsedMensajeOn404() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.eliminarDemoraResponse = errorResponse(404, "{\"mensaje\":\"Demora no encontrada\"}");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        AtomicReference<UiState<Void>> latest = new AtomicReference<>();
        Observer<UiState<Void>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.eliminarDemora(99);

            UiState<Void> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Demora no encontrada", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void eliminarDemoraEmitsNetworkErrorOnIOException() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.eliminarDemoraFailure = new IOException("boom");
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        AtomicReference<UiState<Void>> latest = new AtomicReference<>();
        Observer<UiState<Void>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.eliminarDemora(1);

            UiState<Void> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void eliminarStateReturnsSameInstanceAcrossCalls() {
        FakeDemoraApi api = new FakeDemoraApi();
        api.eliminarDemoraResponse = Response.success(null);
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<Void>> first = repo.getEliminarState();
        LiveData<UiState<Void>> second = repo.getEliminarState();
        assertSame(first, second);

        repo.eliminarDemora(5);
        assertSame(first, repo.getEliminarState());
    }

    // ------------------------------------------------------------------
    // Pairwise distinct state instances (DEM-DI-001)
    // ------------------------------------------------------------------

    @Test
    public void allFourStateInstancesArePairwiseDistinct() {
        FakeDemoraApi api = new FakeDemoraApi();
        DemoraRepositoryImpl repo = new DemoraRepositoryImpl(api);

        LiveData<UiState<List<DemoraDto>>> demoras = repo.getDemorasState();
        LiveData<UiState<DemoraDto>> crear = repo.getCrearState();
        LiveData<UiState<DemoraDto>> actualizar = repo.getActualizarState();
        LiveData<UiState<Void>> eliminar = repo.getEliminarState();

        assertNotEquals(demoras, crear);
        assertNotEquals(demoras, actualizar);
        assertNotEquals(demoras, eliminar);
        assertNotEquals(crear, actualizar);
        assertNotEquals(crear, eliminar);
        assertNotEquals(actualizar, eliminar);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

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

    static final class FakeDemoraApi implements DemoraApi {
        // list
        Response<List<DemoraDto>> getDemorasResponse;
        Throwable getDemorasFailure;
        Integer lastGetDemorasPedidoId;
        @Override
        public Call<List<DemoraDto>> getDemoras(Integer pedidoId) {
            this.lastGetDemorasPedidoId = pedidoId;
            return new FakeCall<>(getDemorasResponse, getDemorasFailure);
        }

        // create
        Response<DemoraDto> crearDemoraResponse;
        Throwable crearDemoraFailure;
        @Override
        public Call<DemoraDto> crearDemora(CrearDemoraRequest request) {
            return new FakeCall<>(crearDemoraResponse, crearDemoraFailure);
        }

        // update
        Response<DemoraDto> actualizarDemoraResponse;
        Throwable actualizarDemoraFailure;
        ActualizarDemoraRequest lastActualizarRequest;
        int lastActualizarId = -1;
        @Override
        public Call<DemoraDto> actualizarDemora(int id, ActualizarDemoraRequest request) {
            this.lastActualizarId = id;
            this.lastActualizarRequest = request;
            return new FakeCall<>(actualizarDemoraResponse, actualizarDemoraFailure);
        }

        // delete
        Response<Void> eliminarDemoraResponse;
        Throwable eliminarDemoraFailure;
        int lastEliminarId = -1;
        @Override
        public Call<Void> eliminarDemora(int id) {
            this.lastEliminarId = id;
            return new FakeCall<>(eliminarDemoraResponse, eliminarDemoraFailure);
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

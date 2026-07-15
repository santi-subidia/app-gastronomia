package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.ProductoApi;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Spec coverage for the 4 new repository methods. Mirrors the
 * AuthRepositoryImplTest pattern: FakeApi + FakeCall + InstantTaskExecutorRule.
 *
 * Spec mapping:
 *  - PROD-CRUD-001: getProducto emits LOADING -> SUCCESS on 2xx
 *  - PROD-CRUD-002: crearProducto emits LOADING -> SUCCESS on 201
 *  - PROD-CRUD-003: actualizarProducto emits LOADING -> SUCCESS on 2xx
 *  - PROD-CRUD-004: eliminarProducto emits LOADING -> SUCCESS on 2xx
 *  - PROD-CRUD-005: actualizarProducto partial-update — verified at DTO level
 *  - PROD-CRUD-006: every method emits ERROR on 4xx/IOException
 *  - PROD-CRUD-007: same LiveData instance across calls per method
 */
public class ProductoRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // getProducto
    // ------------------------------------------------------------------

    @Test
    public void getProductoEmitsLoadingThenSuccessOn2xx() {
        FakeProductoApi api = new FakeProductoApi();
        ProductoDto dto = new ProductoDto();
        dto.setId(5);
        dto.setNombre("Milanesa");
        api.getProductoResponse = Response.success(dto);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getProductoState();
        EmissionRecorder<UiState<ProductoDto>> recorder = recordEmissions(state);
        try {
            repo.getProducto(5);

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getProductoEmitsErrorWithParsedMensajeOn404() {
        FakeProductoApi api = new FakeProductoApi();
        api.getProductoResponse = errorResponse(404, "{\"mensaje\":\"Producto no encontrado\"}");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getProductoState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getProducto(99);

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Producto no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getProductoEmitsNetworkErrorOnIOException() {
        FakeProductoApi api = new FakeProductoApi();
        api.getProductoFailure = new IOException("boom");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getProductoState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getProducto(1);

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getProductoStateReturnsSameInstanceAcrossCalls() {
        FakeProductoApi api = new FakeProductoApi();
        api.getProductoResponse = Response.success(new ProductoDto());
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> first = repo.getProductoState();
        LiveData<UiState<ProductoDto>> second = repo.getProductoState();
        assertSame(
                "getProductoState() must return the same LiveData instance every call",
                first, second
        );

        repo.getProducto(1);
        assertSame(first, repo.getProductoState());
    }

    // ------------------------------------------------------------------
    // crearProducto
    // ------------------------------------------------------------------

    @Test
    public void crearProductoEmitsLoadingThenSuccessOn201() {
        FakeProductoApi api = new FakeProductoApi();
        ProductoDto created = new ProductoDto();
        created.setId(10);
        created.setNombre("Pizza");
        api.crearProductoResponse = Response.success(created);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getCrearState();
        EmissionRecorder<UiState<ProductoDto>> recorder = recordEmissions(state);
        try {
            repo.crearProducto(new CrearProductoRequest("Pizza", 3500.0, 25));

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void crearProductoEmitsErrorWithParsedMensajeOn409() {
        FakeProductoApi api = new FakeProductoApi();
        api.crearProductoResponse = errorResponse(409, "{\"mensaje\":\"Ya existe un producto con ese nombre\"}");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getCrearState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearProducto(new CrearProductoRequest("Pizza", 3500.0, 25));

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Ya existe un producto con ese nombre", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearProductoEmitsNetworkErrorOnIOException() {
        FakeProductoApi api = new FakeProductoApi();
        api.crearProductoFailure = new IOException("boom");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getCrearState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearProducto(new CrearProductoRequest("Pizza", 3500.0, 25));

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearStateReturnsSameInstanceAcrossCalls() {
        FakeProductoApi api = new FakeProductoApi();
        api.crearProductoResponse = Response.success(new ProductoDto());
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> first = repo.getCrearState();
        LiveData<UiState<ProductoDto>> second = repo.getCrearState();
        assertSame(first, second);

        repo.crearProducto(new CrearProductoRequest("Pizza", 3500.0, 25));
        assertSame(first, repo.getCrearState());
    }

    // ------------------------------------------------------------------
    // actualizarProducto
    // ------------------------------------------------------------------

    @Test
    public void actualizarProductoEmitsLoadingThenSuccessOn2xx() {
        FakeProductoApi api = new FakeProductoApi();
        ProductoDto updated = new ProductoDto();
        updated.setId(5);
        updated.setNombre("Milanesa");
        updated.setPrecio(2000.0);
        api.actualizarProductoResponse = Response.success(updated);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getActualizarState();
        EmissionRecorder<UiState<ProductoDto>> recorder = recordEmissions(state);
        try {
            ActualizarProductoRequest req = new ActualizarProductoRequest();
            req.setPrecio(2000.0);
            repo.actualizarProducto(5, req);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void actualizarProductoSendsOnlyChangedFieldsInRequestBody() {
        FakeProductoApi api = new FakeProductoApi();
        api.actualizarProductoResponse = Response.success(new ProductoDto());
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        // Only set the price — verify the captured request body matches.
        ActualizarProductoRequest req = new ActualizarProductoRequest();
        req.setPrecio(2000.0);
        repo.actualizarProducto(5, req);

        assertNotNull("repository must have captured the request", api.lastActualizarRequest);
        assertNull(api.lastActualizarRequest.getNombre());
        assertEquals(Double.valueOf(2000.0), api.lastActualizarRequest.getPrecio());
        assertNull(api.lastActualizarRequest.getDemora());
    }

    @Test
    public void actualizarProductoEmitsErrorWithParsedMensajeOn404() {
        FakeProductoApi api = new FakeProductoApi();
        api.actualizarProductoResponse = errorResponse(404, "{\"mensaje\":\"Producto no encontrado\"}");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getActualizarState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            ActualizarProductoRequest req = new ActualizarProductoRequest();
            req.setPrecio(2000.0);
            repo.actualizarProducto(99, req);

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Producto no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarProductoEmitsNetworkErrorOnIOException() {
        FakeProductoApi api = new FakeProductoApi();
        api.actualizarProductoFailure = new IOException("boom");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> state = repo.getActualizarState();
        AtomicReference<UiState<ProductoDto>> latest = new AtomicReference<>();
        Observer<UiState<ProductoDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            ActualizarProductoRequest req = new ActualizarProductoRequest();
            req.setPrecio(2000.0);
            repo.actualizarProducto(5, req);

            UiState<ProductoDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void actualizarStateReturnsSameInstanceAcrossCalls() {
        FakeProductoApi api = new FakeProductoApi();
        api.actualizarProductoResponse = Response.success(new ProductoDto());
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<ProductoDto>> first = repo.getActualizarState();
        LiveData<UiState<ProductoDto>> second = repo.getActualizarState();
        assertSame(first, second);

        ActualizarProductoRequest req = new ActualizarProductoRequest();
        req.setPrecio(2000.0);
        repo.actualizarProducto(5, req);
        assertSame(first, repo.getActualizarState());
    }

    // ------------------------------------------------------------------
    // eliminarProducto
    // ------------------------------------------------------------------

    @Test
    public void eliminarProductoEmitsLoadingThenSuccessOn2xx() {
        FakeProductoApi api = new FakeProductoApi();
        api.eliminarProductoResponse = Response.success(null);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        EmissionRecorder<UiState<Void>> recorder = recordEmissions(state);
        try {
            repo.eliminarProducto(5);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void eliminarProductoEmitsErrorWithParsedMensajeOn404() {
        FakeProductoApi api = new FakeProductoApi();
        api.eliminarProductoResponse = errorResponse(404, "{\"mensaje\":\"Producto no encontrado\"}");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        AtomicReference<UiState<Void>> latest = new AtomicReference<>();
        Observer<UiState<Void>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.eliminarProducto(99);

            UiState<Void> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Producto no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void eliminarProductoEmitsNetworkErrorOnIOException() {
        FakeProductoApi api = new FakeProductoApi();
        api.eliminarProductoFailure = new IOException("boom");
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<Void>> state = repo.getEliminarState();
        AtomicReference<UiState<Void>> latest = new AtomicReference<>();
        Observer<UiState<Void>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.eliminarProducto(1);

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
        FakeProductoApi api = new FakeProductoApi();
        api.eliminarProductoResponse = Response.success(null);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<Void>> first = repo.getEliminarState();
        LiveData<UiState<Void>> second = repo.getEliminarState();
        assertSame(first, second);

        repo.eliminarProducto(5);
        assertSame(first, repo.getEliminarState());
    }

    // ------------------------------------------------------------------
    // Regression: getProductos() must stay untouched (PROD-CRUD-007/separate).
    // ------------------------------------------------------------------

    @Test
    public void getProductosListStateIsNotAffectedByNewMethodStates() {
        FakeProductoApi api = new FakeProductoApi();
        api.getProductoResponse = Response.success(new ProductoDto());
        api.crearProductoResponse = Response.success(new ProductoDto());
        api.actualizarProductoResponse = Response.success(new ProductoDto());
        api.eliminarProductoResponse = Response.success(null);
        ProductoRepositoryImpl repo = new ProductoRepositoryImpl(api);

        LiveData<UiState<List<ProductoDto>>> listState = repo.getProductListState();
        assertSame(listState, repo.getProductListState());

        // Triggering other methods must NOT mutate the list-state instance.
        repo.getProducto(1);
        repo.crearProducto(new CrearProductoRequest("X", 1.0, 1));
        ActualizarProductoRequest req = new ActualizarProductoRequest();
        req.setPrecio(1.0);
        repo.actualizarProducto(1, req);
        repo.eliminarProducto(1);

        assertSame("getProductListState() must stay a single instance untouched by other methods",
                listState, repo.getProductListState());
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
        BufferedSource source = Okio.buffer(Okio.source(new java.io.ByteArrayInputStream(
                jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        ResponseBody body = ResponseBody.create(mediaType, jsonBody);
        return Response.error(code, body);
    }

    // -- Fakes ------------------------------------------------------------

    static final class FakeProductoApi implements ProductoApi {
        // list
        @Override
        public Call<List<ProductoDto>> getProductos() {
            return new FakeCall<>(Response.success(new ArrayList<>()), null);
        }

        // get
        Response<ProductoDto> getProductoResponse;
        Throwable getProductoFailure;
        @Override
        public Call<ProductoDto> getProducto(int id) {
            return new FakeCall<>(getProductoResponse, getProductoFailure);
        }

        // create
        Response<ProductoDto> crearProductoResponse;
        Throwable crearProductoFailure;
        @Override
        public Call<ProductoDto> crearProducto(CrearProductoRequest request) {
            return new FakeCall<>(crearProductoResponse, crearProductoFailure);
        }

        // update
        Response<ProductoDto> actualizarProductoResponse;
        Throwable actualizarProductoFailure;
        ActualizarProductoRequest lastActualizarRequest;
        int lastActualizarId = -1;
        @Override
        public Call<ProductoDto> actualizarProducto(int id, ActualizarProductoRequest request) {
            this.lastActualizarId = id;
            this.lastActualizarRequest = request;
            return new FakeCall<>(actualizarProductoResponse, actualizarProductoFailure);
        }

        // delete
        Response<Void> eliminarProductoResponse;
        Throwable eliminarProductoFailure;
        int lastEliminarId = -1;
        @Override
        public Call<Void> eliminarProducto(int id) {
            this.lastEliminarId = id;
            return new FakeCall<>(eliminarProductoResponse, eliminarProductoFailure);
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

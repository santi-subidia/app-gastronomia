package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.CajaApi;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;

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
 * Spec coverage for the cajas repository.
 *
 * Spec mapping:
 *  - CAJ-LIST-001:  getCajas LOADING -> SUCCESS on 2xx
 *  - CAJ-LIST-002:  getCajas ERROR(parsed mensaje) on 4xx
 *  - CAJ-LIST-003:  getCajas ERROR("No hay conexion a internet") on IOException
 *  - CAJ-LIST-004:  getCajasState returns the same instance across calls
 *  - CAJ-LIST-005:  getCajas passes estado=null through to the API call
 *  - CAJ-LIST-006:  getCajas passes estado="Abierta" through to the API call
 *  - CAJ-ABIERTAS-001: getCajasAbiertas LOADING -> SUCCESS on 2xx (non-empty)
 *  - CAJ-ABIERTAS-002: getCajasAbiertas SUCCESS with empty list (no open cajas)
 *  - CAJ-ABIERTAS-003: getCajasAbiertas ERROR(parsed mensaje) on 5xx
 *  - CAJ-ABIERTAS-004: getCajasAbiertas ERROR("No hay conexion a internet") on IOException
 *  - CAJ-ABIERTAS-005: getCajasAbiertasState returns the same instance across calls
 *  - CAJ-GET-001:   getCaja(id) LOADING -> SUCCESS on 2xx
 *  - CAJ-GET-002:   getCaja(id) ERROR(parsed mensaje) on 4xx
 *  - CAJ-GET-003:   getCaja(id) ERROR("No hay conexion a internet") on IOException
 *  - CAJ-GET-004:   getCajaState returns the same instance across calls
 *  - CAJ-ABRIR-001: abrirCaja(request) LOADING -> SUCCESS on 2xx
 *  - CAJ-ABRIR-002: abrirCaja(request) ERROR(parsed mensaje) on 4xx
 *  - CAJ-ABRIR-003: abrirCaja(request) ERROR("No hay conexion a internet") on IOException
 *  - CAJ-ABRIR-004: abrirState returns the same instance across calls
 *  - CAJ-CERRAR-001: cerrarCaja(id, request) LOADING -> SUCCESS on 2xx
 *  - CAJ-CERRAR-002: cerrarCaja(id, request) ERROR(parsed mensaje) on 4xx
 *  - CAJ-CERRAR-003: cerrarCaja(id, request) ERROR("No hay conexion a internet") on IOException
 *  - CAJ-CERRAR-004: cerrarState returns the same instance across calls
 *  - CAJ-DI-001:    all 5 state instances must be pairwise distinct
 *
 * <p>The cajas entity has no client-side validation guards: open/closed
 * transitions are enforced by the server and surfaced through
 * {@code parseMensaje}. Spec CAJ-VAL-001: this test does not assert
 * any pre-flight validation on the repo.</p>
 */
public class CajaRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // getCajas(estado)
    // ------------------------------------------------------------------

    @Test
    public void getCajasEmitsLoadingThenSuccessOn2xx() {
        FakeCajaApi api = new FakeCajaApi();
        List<CajaDto> data = new ArrayList<>();
        CajaDto c = new CajaDto();
        c.setId(1);
        c.setEstado("Abierta");
        data.add(c);
        api.getCajasResponse = Response.success(data);
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasState();
        EmissionRecorder<UiState<List<CajaDto>>> recorder = recordEmissions(state);
        try {
            repo.getCajas(null);

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getCajasEmitsErrorWithParsedMensajeOn500() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasResponse = errorResponse(500, "{\"mensaje\":\"Falla interna del servidor\"}");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasState();
        AtomicReference<UiState<List<CajaDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<CajaDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCajas(null);

            UiState<List<CajaDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Falla interna del servidor", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajasEmitsNetworkErrorOnIOException() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasFailure = new IOException("boom");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasState();
        AtomicReference<UiState<List<CajaDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<CajaDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCajas(null);

            UiState<List<CajaDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajasStateReturnsSameInstanceAcrossCalls() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasResponse = Response.success(new ArrayList<>());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> first = repo.getCajasState();
        LiveData<UiState<List<CajaDto>>> second = repo.getCajasState();
        assertSame(first, second);

        repo.getCajas(null);
        assertSame(first, repo.getCajasState());
    }

    @Test
    public void getCajasPassesNullEstadoThroughToApi() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasResponse = Response.success(new ArrayList<>());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        repo.getCajas(null);

        assertEquals(null, api.lastCajasEstado);
    }

    @Test
    public void getCajasPassesAbiertaEstadoThroughToApi() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasResponse = Response.success(new ArrayList<>());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        repo.getCajas("Abierta");

        assertEquals("Abierta", api.lastCajasEstado);
    }

    // ------------------------------------------------------------------
    // getCaja(id)
    // ------------------------------------------------------------------

    @Test
    public void getCajaEmitsLoadingThenSuccessOn2xx() {
        FakeCajaApi api = new FakeCajaApi();
        CajaDto dto = new CajaDto();
        dto.setId(7);
        dto.setEstado("Abierta");
        api.getCajaResponse = Response.success(dto);
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getCajaState();
        EmissionRecorder<UiState<CajaDto>> recorder = recordEmissions(state);
        try {
            repo.getCaja(7);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(7, api.lastGetCajaId);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getCajaEmitsErrorWithParsedMensajeOn404() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajaResponse = errorResponse(404, "{\"mensaje\":\"Caja no encontrada\"}");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getCajaState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCaja(99);

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Caja no encontrada", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajaEmitsNetworkErrorOnIOException() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajaFailure = new IOException("boom");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getCajaState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCaja(1);

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajaStateReturnsSameInstanceAcrossCalls() {
        FakeCajaApi api = new FakeCajaApi();
        api.getCajaResponse = Response.success(new CajaDto());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> first = repo.getCajaState();
        LiveData<UiState<CajaDto>> second = repo.getCajaState();
        assertSame(first, second);

        repo.getCaja(1);
        assertSame(first, repo.getCajaState());
    }

    // ------------------------------------------------------------------
    // abrirCaja(request)
    // ------------------------------------------------------------------

    @Test
    public void abrirCajaEmitsLoadingThenSuccessOn2xx() {
        FakeCajaApi api = new FakeCajaApi();
        CajaDto created = new CajaDto();
        created.setId(11);
        created.setEstado("Abierta");
        api.abrirCajaResponse = Response.success(created);
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        AbrirCajaRequest req = new AbrirCajaRequest(1000.0);
        LiveData<UiState<CajaDto>> state = repo.getAbrirState();
        EmissionRecorder<UiState<CajaDto>> recorder = recordEmissions(state);
        try {
            repo.abrirCaja(req);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertNotNull("API must have been called", api.lastAbrirCajaRequest);
            assertEquals(1000.0, api.lastAbrirCajaRequest.getMontoApertura(), 0.0);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void abrirCajaEmitsErrorWithParsedMensajeOn409() {
        FakeCajaApi api = new FakeCajaApi();
        api.abrirCajaResponse = errorResponse(409, "{\"mensaje\":\"Ya existe una caja abierta\"}");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getAbrirState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.abrirCaja(new AbrirCajaRequest(1000.0));

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Ya existe una caja abierta", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void abrirCajaEmitsNetworkErrorOnIOException() {
        FakeCajaApi api = new FakeCajaApi();
        api.abrirCajaFailure = new IOException("boom");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getAbrirState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.abrirCaja(new AbrirCajaRequest(1000.0));

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void abrirStateReturnsSameInstanceAcrossCalls() {
        FakeCajaApi api = new FakeCajaApi();
        api.abrirCajaResponse = Response.success(new CajaDto());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> first = repo.getAbrirState();
        LiveData<UiState<CajaDto>> second = repo.getAbrirState();
        assertSame(first, second);

        repo.abrirCaja(new AbrirCajaRequest(1000.0));
        assertSame(first, repo.getAbrirState());
    }

    // ------------------------------------------------------------------
    // cerrarCaja(id, request)
    // ------------------------------------------------------------------

    @Test
    public void cerrarCajaEmitsLoadingThenSuccessOn2xx() {
        FakeCajaApi api = new FakeCajaApi();
        CajaDto closed = new CajaDto();
        closed.setId(11);
        closed.setEstado("Cerrada");
        api.cerrarCajaResponse = Response.success(closed);
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        CerrarCajaRequest req = new CerrarCajaRequest(1500.0, 1480.0);
        LiveData<UiState<CajaDto>> state = repo.getCerrarState();
        EmissionRecorder<UiState<CajaDto>> recorder = recordEmissions(state);
        try {
            repo.cerrarCaja(11, req);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(11, api.lastCerrarCajaId);
            assertNotNull(api.lastCerrarCajaRequest);
            assertEquals(1500.0, api.lastCerrarCajaRequest.getMontoCierreTeorico(), 0.0);
            assertEquals(1480.0, api.lastCerrarCajaRequest.getMontoCierreReal(), 0.0);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void cerrarCajaEmitsErrorWithParsedMensajeOn409() {
        FakeCajaApi api = new FakeCajaApi();
        api.cerrarCajaResponse = errorResponse(409, "{\"mensaje\":\"La caja ya esta cerrada\"}");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getCerrarState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.cerrarCaja(11, new CerrarCajaRequest(1500.0, 1480.0));

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("La caja ya esta cerrada", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void cerrarCajaEmitsNetworkErrorOnIOException() {
        FakeCajaApi api = new FakeCajaApi();
        api.cerrarCajaFailure = new IOException("boom");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> state = repo.getCerrarState();
        AtomicReference<UiState<CajaDto>> latest = new AtomicReference<>();
        Observer<UiState<CajaDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.cerrarCaja(11, new CerrarCajaRequest(1500.0, 1480.0));

            UiState<CajaDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void cerrarStateReturnsSameInstanceAcrossCalls() {
        FakeCajaApi api = new FakeCajaApi();
        api.cerrarCajaResponse = Response.success(new CajaDto());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<CajaDto>> first = repo.getCerrarState();
        LiveData<UiState<CajaDto>> second = repo.getCerrarState();
        assertSame(first, second);

        repo.cerrarCaja(11, new CerrarCajaRequest(1500.0, 1480.0));
        assertSame(first, repo.getCerrarState());
    }

    // ------------------------------------------------------------------
    // Regression: state instances must be pairwise distinct across methods
    // ------------------------------------------------------------------

    @Test
    public void allFourStateInstancesArePairwiseDistinct() {
        FakeCajaApi api = new FakeCajaApi();
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        List<LiveData<?>> all = new ArrayList<>();
        all.add(repo.getCajasState());
        all.add(repo.getCajasAbiertasState());
        all.add(repo.getCajaState());
        all.add(repo.getAbrirState());
        all.add(repo.getCerrarState());

        // The five state instances must be pairwise distinct so that a
        // SUCCESS/ERROR on one verb does not appear on another.
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                assertNotNull("state " + i + " must not be null", all.get(i));
                assertTrue("states " + i + " and " + j + " must be distinct instances",
                        all.get(i) != all.get(j));
            }
        }
    }

    // ------------------------------------------------------------------
    // getCajasAbiertas()  --  spec CAJ-ABIERTAS-001
    // ------------------------------------------------------------------

    @Test
    public void getCajasAbiertasEmitsLoadingThenSuccessOn2xx() {
        // Triangulation: non-empty success path.
        FakeCajaApi api = new FakeCajaApi();
        List<CajaDto> data = new ArrayList<>();
        CajaDto a = new CajaDto();
        a.setId(1);
        a.setEstado("Abierta");
        CajaDto b = new CajaDto();
        b.setId(2);
        b.setEstado("Abierta");
        data.add(a);
        data.add(b);
        api.getCajasAbiertasResponse = Response.success(data);
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasAbiertasState();
        EmissionRecorder<UiState<List<CajaDto>>> recorder = recordEmissions(state);
        try {
            repo.getCajasAbiertas();

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(1, api.getCajasAbiertasCallCount);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getCajasAbiertasEmitsSuccessWithEmptyListWhenNoCajasAbiertas() {
        // Spec CAJ-ABIERTAS-002: empty list (200) is a valid success
        // state — the UI should render "no hay cajas abiertas".
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasAbiertasResponse = Response.success(new ArrayList<>());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasAbiertasState();
        AtomicReference<UiState<List<CajaDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<CajaDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCajasAbiertas();

            UiState<List<CajaDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.SUCCESS, after.getStatus());
            assertNotNull(after.getData());
            assertTrue("empty-list success must have an empty data payload",
                    after.getData().isEmpty());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajasAbiertasEmitsErrorWithParsedMensajeOn500() {
        // Spec CAJ-ABIERTAS-003: server error (e.g. 500) must surface
        // the parsed `mensaje` from the error envelope.
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasAbiertasResponse = errorResponse(500, "{\"mensaje\":\"Falla interna del servidor\"}");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasAbiertasState();
        AtomicReference<UiState<List<CajaDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<CajaDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCajasAbiertas();

            UiState<List<CajaDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Falla interna del servidor", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajasAbiertasEmitsNetworkErrorOnIOException() {
        // Spec CAJ-ABIERTAS-004: network failure must emit the
        // "No hay conexión a internet" message.
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasAbiertasFailure = new IOException("boom");
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> state = repo.getCajasAbiertasState();
        AtomicReference<UiState<List<CajaDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<CajaDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getCajasAbiertas();

            UiState<List<CajaDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getCajasAbiertasStateReturnsSameInstanceAcrossCalls() {
        // Single-instance pattern: the state LiveData must be the same
        // across calls so observers registered in the ViewModel ctor
        // keep receiving emissions across retries.
        FakeCajaApi api = new FakeCajaApi();
        api.getCajasAbiertasResponse = Response.success(new ArrayList<>());
        CajaRepositoryImpl repo = new CajaRepositoryImpl(api);

        LiveData<UiState<List<CajaDto>>> first = repo.getCajasAbiertasState();
        LiveData<UiState<List<CajaDto>>> second = repo.getCajasAbiertasState();
        assertSame(first, second);

        repo.getCajasAbiertas();
        assertSame(first, repo.getCajasAbiertasState());
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

    private static <T> Response<T> errorResponse(int code, String jsonBody) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        ResponseBody body = ResponseBody.create(mediaType, jsonBody);
        return Response.error(code, body);
    }

    // -- Fakes ------------------------------------------------------------

    static final class FakeCajaApi implements CajaApi {
        Response<List<CajaDto>> getCajasResponse;
        Throwable getCajasFailure;
        String lastCajasEstado;
        @Override
        public Call<List<CajaDto>> getCajas(String estado) {
            this.lastCajasEstado = estado;
            return new FakeCall<>(getCajasResponse, getCajasFailure);
        }

        Response<List<CajaDto>> getCajasAbiertasResponse;
        Throwable getCajasAbiertasFailure;
        int getCajasAbiertasCallCount;
        @Override
        public Call<List<CajaDto>> getCajasAbiertas() {
            this.getCajasAbiertasCallCount++;
            return new FakeCall<>(getCajasAbiertasResponse, getCajasAbiertasFailure);
        }

        Response<CajaDto> getCajaResponse;
        Throwable getCajaFailure;
        int lastGetCajaId = -1;
        @Override
        public Call<CajaDto> getCaja(int id) {
            this.lastGetCajaId = id;
            return new FakeCall<>(getCajaResponse, getCajaFailure);
        }

        Response<CajaDto> abrirCajaResponse;
        Throwable abrirCajaFailure;
        AbrirCajaRequest lastAbrirCajaRequest;
        @Override
        public Call<CajaDto> abrirCaja(AbrirCajaRequest request) {
            this.lastAbrirCajaRequest = request;
            return new FakeCall<>(abrirCajaResponse, abrirCajaFailure);
        }

        Response<CajaDto> cerrarCajaResponse;
        Throwable cerrarCajaFailure;
        int lastCerrarCajaId = -1;
        CerrarCajaRequest lastCerrarCajaRequest;
        @Override
        public Call<CajaDto> cerrarCaja(int id, CerrarCajaRequest request) {
            this.lastCerrarCajaId = id;
            this.lastCerrarCajaRequest = request;
            return new FakeCall<>(cerrarCajaResponse, cerrarCajaFailure);
        }
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

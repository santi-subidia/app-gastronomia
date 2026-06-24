package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.PedidoApi;
import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;
import com.example.app_movil_gastronomia.data.dto.pedido.AsignarRepartidorRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearDetalleRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Spec coverage for the pedidos repository.
 *
 * Spec mapping:
 *  - PED-CRUD-001: getPedidos / getPedido / getByEstado LOADING -> SUCCESS
 *  - PED-CRUD-002: crearPedido LOADING -> SUCCESS on 201
 *  - PED-CRUD-003: cambiarEstado / asignarRepartidor LOADING -> SUCCESS on 2xx
 *  - PED-CRUD-004: every method emits ERROR(parsed mensaje) on 4xx
 *  - PED-CRUD-005: every method emits ERROR("No hay conexion a internet") on IOException
 *  - PED-CRUD-006: same LiveData instance across calls per method (single-instance pattern)
 *  - PED-VAL-001: crearPedido rejects empty/null detalles BEFORE any API call
 *  - PED-VAL-002: crearPedido rejects metodoVentaId=1 without coords BEFORE any API call
 *  - PED-VAL-003: valid crearPedido still calls the API
 *  - PED-ENUM-001: getByEstado uses EstadoPedidoEnum.getApiValue() in the path
 *  - PED-ENUM-002 (v2): cambiarEstado resolves the enum via CatalogoRepository and
 *                       sends a RAW int body (no CambiarEstadoRequest wrapper)
 */
public class PedidoRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // getPedidos
    // ------------------------------------------------------------------

    @Test
    public void getPedidosEmitsLoadingThenSuccessOn2xx() {
        FakePedidoApi api = new FakePedidoApi();
        List<PedidoResumenDto> data = new ArrayList<>();
        PedidoResumenDto r = new PedidoResumenDto();
        r.setId(1);
        api.getPedidosResponse = Response.success(data);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getPedidosState();
        EmissionRecorder<UiState<List<PedidoResumenDto>>> recorder = recordEmissions(state);
        try {
            repo.getPedidos();

            assertTrue("expected LOADING, got: " + recorder.seen, recorder.seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + recorder.seen, recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getPedidosEmitsErrorWithParsedMensajeOn500() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidosResponse = errorResponse(500, "{\"mensaje\":\"Falla interna del servidor\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getPedidosState();
        AtomicReference<UiState<List<PedidoResumenDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<PedidoResumenDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getPedidos();

            UiState<List<PedidoResumenDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Falla interna del servidor", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getPedidosEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidosFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getPedidosState();
        AtomicReference<UiState<List<PedidoResumenDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<PedidoResumenDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getPedidos();

            UiState<List<PedidoResumenDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getPedidosStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidosResponse = Response.success(new ArrayList<>());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> first = repo.getPedidosState();
        LiveData<UiState<List<PedidoResumenDto>>> second = repo.getPedidosState();
        assertSame(first, second);

        repo.getPedidos();
        assertSame(first, repo.getPedidosState());
    }

    // ------------------------------------------------------------------
    // getPedido(id)
    // ------------------------------------------------------------------

    @Test
    public void getPedidoEmitsLoadingThenSuccessOn2xx() {
        FakePedidoApi api = new FakePedidoApi();
        PedidoDetalleDto dto = new PedidoDetalleDto();
        dto.setId(7);
        api.getPedidoResponse = Response.success(dto);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getPedidoState();
        EmissionRecorder<UiState<PedidoDetalleDto>> recorder = recordEmissions(state);
        try {
            repo.getPedido(7);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(7, api.lastGetPedidoId);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getPedidoEmitsErrorWithParsedMensajeOn404() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidoResponse = errorResponse(404, "{\"mensaje\":\"Pedido no encontrado\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getPedidoState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getPedido(99);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Pedido no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getPedidoEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidoFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getPedidoState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getPedido(1);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getPedidoStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.getPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> first = repo.getPedidoState();
        LiveData<UiState<PedidoDetalleDto>> second = repo.getPedidoState();
        assertSame(first, second);

        repo.getPedido(1);
        assertSame(first, repo.getPedidoState());
    }

    // ------------------------------------------------------------------
    // getByEstado(estado)
    // ------------------------------------------------------------------

    @Test
    public void getByEstadoEmitsLoadingThenSuccessOn2xx() {
        FakePedidoApi api = new FakePedidoApi();
        api.getByEstadoResponse = Response.success(new ArrayList<>());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getByEstadoState();
        EmissionRecorder<UiState<List<PedidoResumenDto>>> recorder = recordEmissions(state);
        try {
            repo.getByEstado(EstadoPedidoEnum.EN_PREPARACION);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            // PED-ENUM-001: repo must call getApiValue() before passing the path param
            assertEquals("EnPreparacion", api.lastByEstadoPath);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void getByEstadoEmitsErrorWithParsedMensajeOn500() {
        FakePedidoApi api = new FakePedidoApi();
        api.getByEstadoResponse = errorResponse(500, "{\"mensaje\":\"No se pudo filtrar\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getByEstadoState();
        AtomicReference<UiState<List<PedidoResumenDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<PedidoResumenDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getByEstado(EstadoPedidoEnum.PENDIENTE);

            UiState<List<PedidoResumenDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No se pudo filtrar", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getByEstadoEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.getByEstadoFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> state = repo.getByEstadoState();
        AtomicReference<UiState<List<PedidoResumenDto>>> latest = new AtomicReference<>();
        Observer<UiState<List<PedidoResumenDto>>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.getByEstado(EstadoPedidoEnum.LISTO_PARA_RETIRAR);

            UiState<List<PedidoResumenDto>> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void getByEstadoStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.getByEstadoResponse = Response.success(new ArrayList<>());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<List<PedidoResumenDto>>> first = repo.getByEstadoState();
        LiveData<UiState<List<PedidoResumenDto>>> second = repo.getByEstadoState();
        assertSame(first, second);

        repo.getByEstado(EstadoPedidoEnum.EN_CAMINO);
        assertSame(first, repo.getByEstadoState());
    }

    // ------------------------------------------------------------------
    // crearPedido(request)
    // ------------------------------------------------------------------

    @Test
    public void crearPedidoEmitsLoadingThenSuccessOn201() {
        FakePedidoApi api = new FakePedidoApi();
        PedidoDetalleDto created = new PedidoDetalleDto();
        created.setId(42);
        api.crearPedidoResponse = Response.success(created);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        EmissionRecorder<UiState<PedidoDetalleDto>> recorder = recordEmissions(state);
        try {
            repo.crearPedido(req);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertNotNull("API must have been called", api.lastCrearPedidoRequest);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void crearPedidoEmitsErrorWithParsedMensajeOn400() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = errorResponse(400, "{\"mensaje\":\"Caja no valida\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(validDeliveryRequest());

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Caja no valida", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(validDeliveryRequest());

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoRejectsNullDetallesBeforeApiCall() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        req.setDetalles(null);

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(req);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("El pedido debe tener al menos un producto", after.getError());
            assertNull("API must NOT be called when validation fails", api.lastCrearPedidoRequest);
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoRejectsEmptyDetallesBeforeApiCall() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        req.setDetalles(new ArrayList<>());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(req);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("El pedido debe tener al menos un producto", after.getError());
            assertNull("API must NOT be called when validation fails", api.lastCrearPedidoRequest);
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoRejectsDeliveryWithoutLatitud() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        req.setLatitudDestino(null);

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(req);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Las coordenadas de destino son requeridas para Delivery", after.getError());
            assertNull("API must NOT be called when validation fails", api.lastCrearPedidoRequest);
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoRejectsDeliveryWithoutLongitud() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        req.setLongitudDestino(null);

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCrearState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.crearPedido(req);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Las coordenadas de destino son requeridas para Delivery", after.getError());
            assertNull("API must NOT be called when validation fails", api.lastCrearPedidoRequest);
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void crearPedidoAllowsNonDeliveryWithoutCoords() {
        // metodoVentaId != 1 -> validation guard for delivery coords must NOT trigger
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        CrearPedidoRequest req = validDeliveryRequest();
        req.setMetodoVentaId(2); // take-away / salon — no coords required
        req.setLatitudDestino(null);
        req.setLongitudDestino(null);

        repo.crearPedido(req);

        assertNotNull("API must be called for non-delivery even without coords",
                api.lastCrearPedidoRequest);
    }

    @Test
    public void crearStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.crearPedidoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> first = repo.getCrearState();
        LiveData<UiState<PedidoDetalleDto>> second = repo.getCrearState();
        assertSame(first, second);

        repo.crearPedido(validDeliveryRequest());
        assertSame(first, repo.getCrearState());
    }

    // ------------------------------------------------------------------
    // cambiarEstado(id, estado) — v2 contract
    // ------------------------------------------------------------------

    /**
     * The v2 test catalog wires every EstadoPedidoEnum apiValue to a
     * deterministic, distinct catalog ID so the resolver has something
     * to return. The IDs are intentionally NOT 1:1 with the enum order
     * to make any accidental "we sent the index instead of the id"
     * regression obvious.
     */
    private static FakeCatalogoRepository readyCatalog() {
        Map<String, Integer> estados = new HashMap<>();
        estados.put("Pendiente", 100);
        estados.put("EnPreparacion", 200);
        estados.put("ListoParaRetirar", 300);
        estados.put("EnCamino", 400);
        estados.put("Entregado", 500);
        estados.put("Retirado", 550);
        estados.put("Cancelado", 600);
        estados.put("Devuelto", 700);
        return new FakeCatalogoRepository(estados, true);
    }

    @Test
    public void cambiarEstadoEmitsLoadingThenSuccessOn2xx() {
        FakePedidoApi api = new FakePedidoApi();
        PedidoDetalleDto updated = new PedidoDetalleDto();
        updated.setId(3);
        api.cambiarEstadoResponse = Response.success(updated);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCambiarEstadoState();
        EmissionRecorder<UiState<PedidoDetalleDto>> recorder = recordEmissions(state);
        try {
            repo.cambiarEstado(3, EstadoPedidoEnum.LISTO_PARA_RETIRAR);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(3, api.lastCambiarEstadoId);
            // PED-ENUM-002 (v2): body is the catalog-resolved int, not a wrapper object.
            assertEquals(300, api.lastCambiarEstadoBody);
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void cambiarEstadoResolvesEachEnumToItsOwnCatalogId() {
        // Triangulation: every enum hits the API with a different int.
        for (EstadoPedidoEnum e : EstadoPedidoEnum.values()) {
            FakePedidoApi api = new FakePedidoApi();
            api.cambiarEstadoResponse = Response.success(new PedidoDetalleDto());
            PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

            repo.cambiarEstado(1, e);

            int expected = readyCatalog().resolveEstadoId(e.getApiValue());
            assertEquals("enum " + e.name() + " must resolve to its catalog id",
                    expected, api.lastCambiarEstadoBody);
        }
    }

    @Test
    public void cambiarEstadoEmitsErrorWithParsedMensajeOn409() {
        FakePedidoApi api = new FakePedidoApi();
        api.cambiarEstadoResponse = errorResponse(409, "{\"mensaje\":\"Transicion de estado invalida\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCambiarEstadoState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.cambiarEstado(1, EstadoPedidoEnum.ENTREGADO);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Transicion de estado invalida", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void cambiarEstadoEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.cambiarEstadoFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCambiarEstadoState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.cambiarEstado(1, EstadoPedidoEnum.CANCELADO);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void cambiarEstadoEmitsErrorWhenCatalogNotReady() {
        // Spec: when the catalog is not yet loaded the repo must NOT
        // call the API and must surface a clear error.
        FakePedidoApi api = new FakePedidoApi();
        api.cambiarEstadoResponse = Response.success(new PedidoDetalleDto());
        FakeCatalogoRepository notReady = new FakeCatalogoRepository(
                Collections.<String, Integer>emptyMap(), false);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, notReady);

        LiveData<UiState<PedidoDetalleDto>> state = repo.getCambiarEstadoState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.cambiarEstado(1, EstadoPedidoEnum.EN_PREPARACION);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertNotNull(after.getError());
            assertTrue("error must mention catalog, got: " + after.getError(),
                    after.getError().toLowerCase().contains("cat"));
            assertEquals(-1, api.lastCambiarEstadoId);
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void cambiarEstadoStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.cambiarEstadoResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> first = repo.getCambiarEstadoState();
        LiveData<UiState<PedidoDetalleDto>> second = repo.getCambiarEstadoState();
        assertSame(first, second);

        repo.cambiarEstado(1, EstadoPedidoEnum.PENDIENTE);
        assertSame(first, repo.getCambiarEstadoState());
    }

    // ------------------------------------------------------------------
    // asignarRepartidor(id, repartidorId)
    // ------------------------------------------------------------------

    @Test
    public void asignarRepartidorEmitsLoadingThenSuccessOn2xx() {
        FakePedidoApi api = new FakePedidoApi();
        PedidoDetalleDto updated = new PedidoDetalleDto();
        updated.setId(3);
        api.asignarRepartidorResponse = Response.success(updated);
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getAsignarRepartidorState();
        EmissionRecorder<UiState<PedidoDetalleDto>> recorder = recordEmissions(state);
        try {
            repo.asignarRepartidor(3, 12);

            assertTrue(recorder.seen.contains(UiState.Status.LOADING));
            assertTrue(recorder.seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, recorder.seen.get(0));
            assertEquals(UiState.Status.SUCCESS, recorder.seen.get(recorder.seen.size() - 1));
            assertEquals(3, api.lastAsignarRepartidorId);
            assertNotNull(api.lastAsignarRepartidorRequest);
            assertEquals(12, api.lastAsignarRepartidorRequest.getRepartidorId());
        } finally {
            recorder.cleanup(state);
        }
    }

    @Test
    public void asignarRepartidorEmitsErrorWithParsedMensajeOn404() {
        FakePedidoApi api = new FakePedidoApi();
        api.asignarRepartidorResponse = errorResponse(404, "{\"mensaje\":\"Repartidor no encontrado\"}");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getAsignarRepartidorState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.asignarRepartidor(1, 99);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("Repartidor no encontrado", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void asignarRepartidorEmitsNetworkErrorOnIOException() {
        FakePedidoApi api = new FakePedidoApi();
        api.asignarRepartidorFailure = new IOException("boom");
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> state = repo.getAsignarRepartidorState();
        AtomicReference<UiState<PedidoDetalleDto>> latest = new AtomicReference<>();
        Observer<UiState<PedidoDetalleDto>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.asignarRepartidor(1, 5);

            UiState<PedidoDetalleDto> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    @Test
    public void asignarRepartidorStateReturnsSameInstanceAcrossCalls() {
        FakePedidoApi api = new FakePedidoApi();
        api.asignarRepartidorResponse = Response.success(new PedidoDetalleDto());
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        LiveData<UiState<PedidoDetalleDto>> first = repo.getAsignarRepartidorState();
        LiveData<UiState<PedidoDetalleDto>> second = repo.getAsignarRepartidorState();
        assertSame(first, second);

        repo.asignarRepartidor(1, 5);
        assertSame(first, repo.getAsignarRepartidorState());
    }

    // ------------------------------------------------------------------
    // Regression: state instances must be unique across methods
    // ------------------------------------------------------------------

    @Test
    public void allSixStateInstancesArePairwiseDistinct() {
        FakePedidoApi api = new FakePedidoApi();
        PedidoRepositoryImpl repo = new PedidoRepositoryImpl(api, readyCatalog());

        List<LiveData<?>> all = new ArrayList<>();
        all.add(repo.getPedidosState());
        all.add(repo.getPedidoState());
        all.add(repo.getByEstadoState());
        all.add(repo.getCrearState());
        all.add(repo.getCambiarEstadoState());
        all.add(repo.getAsignarRepartidorState());

        // The six state instances must be pairwise distinct so that a
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
    // Helpers
    // ------------------------------------------------------------------

    private static CrearPedidoRequest validDeliveryRequest() {
        CrearPedidoRequest req = new CrearPedidoRequest();
        req.setCajaId(1);
        req.setMetodoPagoId(1);
        req.setMetodoVentaId(1); // Delivery
        req.setClienteNombre("Juan");
        req.setClienteDireccion("Av. Siempre Viva 742");
        req.setLatitudDestino(-34.6037);
        req.setLongitudDestino(-58.3816);
        req.setTotalEstimado(1500.0);
        req.setDemoraAprox(30);
        req.setDetalles(Collections.singletonList(
                new CrearDetalleRequest(1, "Milanesa", 1500.0, 1)
        ));
        return req;
    }

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

    static final class FakePedidoApi implements PedidoApi {
        Response<List<PedidoResumenDto>> getPedidosResponse;
        Throwable getPedidosFailure;
        @Override
        public Call<List<PedidoResumenDto>> getPedidos() {
            return new FakeCall<>(getPedidosResponse, getPedidosFailure);
        }

        Response<PedidoDetalleDto> getPedidoResponse;
        Throwable getPedidoFailure;
        int lastGetPedidoId = -1;
        @Override
        public Call<PedidoDetalleDto> getPedido(int id) {
            this.lastGetPedidoId = id;
            return new FakeCall<>(getPedidoResponse, getPedidoFailure);
        }

        Response<List<PedidoResumenDto>> getByEstadoResponse;
        Throwable getByEstadoFailure;
        String lastByEstadoPath;
        @Override
        public Call<List<PedidoResumenDto>> getByEstado(String estado) {
            this.lastByEstadoPath = estado;
            return new FakeCall<>(getByEstadoResponse, getByEstadoFailure);
        }

        Response<PedidoDetalleDto> crearPedidoResponse;
        Throwable crearPedidoFailure;
        CrearPedidoRequest lastCrearPedidoRequest;
        @Override
        public Call<PedidoDetalleDto> crearPedido(CrearPedidoRequest request) {
            this.lastCrearPedidoRequest = request;
            return new FakeCall<>(crearPedidoResponse, crearPedidoFailure);
        }

        Response<PedidoDetalleDto> cambiarEstadoResponse;
        Throwable cambiarEstadoFailure;
        int lastCambiarEstadoId = -1;
        int lastCambiarEstadoBody = Integer.MIN_VALUE;
        @Override
        public Call<PedidoDetalleDto> cambiarEstado(int id, int nuevoEstadoId) {
            this.lastCambiarEstadoId = id;
            this.lastCambiarEstadoBody = nuevoEstadoId;
            return new FakeCall<>(cambiarEstadoResponse, cambiarEstadoFailure);
        }

        Response<PedidoDetalleDto> asignarRepartidorResponse;
        Throwable asignarRepartidorFailure;
        int lastAsignarRepartidorId = -1;
        AsignarRepartidorRequest lastAsignarRepartidorRequest;
        @Override
        public Call<PedidoDetalleDto> asignarRepartidor(int id, AsignarRepartidorRequest request) {
            this.lastAsignarRepartidorId = id;
            this.lastAsignarRepartidorRequest = request;
            return new FakeCall<>(asignarRepartidorResponse, asignarRepartidorFailure);
        }
    }

    /**
     * Minimal in-memory {@link CatalogoRepository} for unit tests.
     * Holds a name->id map for estados and a flag for {@link #isReady()}.
     * The other two catalogs are unused by the pedidos tests, so they
     * return empty lists / -1 / false.
     */
    static final class FakeCatalogoRepository implements CatalogoRepository {
        private final Map<String, Integer> estadoIds;
        private final boolean ready;

        FakeCatalogoRepository(Map<String, Integer> estadoIds, boolean ready) {
            this.estadoIds = estadoIds;
            this.ready = ready;
        }

        @Override
        public LiveData<List<CatalogoItemDto>> getEstadosPedido() {
            MutableLiveData<List<CatalogoItemDto>> live = new MutableLiveData<>();
            live.setValue(Collections.<CatalogoItemDto>emptyList());
            return live;
        }

        @Override
        public LiveData<List<CatalogoItemDto>> getMetodosPago() {
            MutableLiveData<List<CatalogoItemDto>> live = new MutableLiveData<>();
            live.setValue(Collections.<CatalogoItemDto>emptyList());
            return live;
        }

        @Override
        public LiveData<List<CatalogoItemDto>> getMetodosVenta() {
            MutableLiveData<List<CatalogoItemDto>> live = new MutableLiveData<>();
            live.setValue(Collections.<CatalogoItemDto>emptyList());
            return live;
        }

        @Override
        public int resolveEstadoId(String nombre) {
            if (!ready) {
                throw new IllegalStateException("catalog not ready");
            }
            Integer id = estadoIds.get(nombre);
            return id == null ? -1 : id;
        }

        @Override
        public int resolveMetodoPagoId(String nombre) {
            return -1;
        }

        @Override
        public int resolveMetodoVentaId(String nombre) {
            return -1;
        }

        @Override
        public boolean isReady() {
            return ready;
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

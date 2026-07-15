package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.data.api.EstadosPedidoApi;
import com.example.app_movil_gastronomia.data.api.MetodoPagoApi;
import com.example.app_movil_gastronomia.data.api.MetodoVentaApi;
import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Verifies the eager-loaded, in-memory cache behavior of
 * {@link CatalogoRepositoryImpl} (Spec CAT-REP-001).
 *
 * <p>Three catalogs (estados, pagos, ventas) are loaded from the API
 * the moment the repository is constructed. While the loads are in
 * flight {@code isReady()} returns false and the resolve methods
 * throw {@link IllegalStateException}. Once all three loads resolve
 * the resolve methods look up names in the in-memory
 * {@code Map<String, Integer>} cache.</p>
 *
 * <p>Failure modes:</p>
 * <ul>
 *   <li>Per-catalog failure: that catalog's LiveData is posted as
 *       empty, other catalogs can still succeed. {@code isReady()}
 *       only flips to true when ALL three succeed.</li>
 *   <li>Unknown name on a fully loaded catalog: resolves to -1
 *       (caller treats that as a misconfiguration, never throws on
 *       a loaded cache).</li>
 * </ul>
 */
public class CatalogoRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ------------------------------------------------------------------
    // Happy path: all three catalogs load successfully
    // ------------------------------------------------------------------

    @Test
    public void constructorEagerlyLoadsAllThreeCatalogs() {
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Pendiente"),
                new CatalogoItemDto(2, "EnPreparacion"),
                new CatalogoItemDto(3, "Listo")
        ));
        FakeMetodoPagoApi pagos = new FakeMetodoPagoApi();
        pagos.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Efectivo"),
                new CatalogoItemDto(2, "Tarjeta")
        ));
        FakeMetodoVentaApi ventas = new FakeMetodoVentaApi();
        ventas.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Delivery"),
                new CatalogoItemDto(2, "Salon")
        ));

        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(estados, pagos, ventas);

        assertTrue("all three catalogs loaded -> isReady() must be true", repo.isReady());
        assertEquals(2, repo.resolveEstadoId("EnPreparacion"));
        assertEquals(1, repo.resolveMetodoPagoId("Efectivo"));
        assertEquals(2, repo.resolveMetodoVentaId("Salon"));
    }

    @Test
    public void getEstadosPedidoLiveDataEmitsLoadedList() {
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Pendiente"),
                new CatalogoItemDto(2, "EnPreparacion")
        ));
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                estados, successPagos(), successVentas());

        LiveData<List<CatalogoItemDto>> data = repo.getEstadosPedido();
        AtomicReference<List<CatalogoItemDto>> latest = new AtomicReference<>();
        Observer<List<CatalogoItemDto>> observer = latest::set;
        data.observeForever(observer);
        try {
            List<CatalogoItemDto> emitted = latest.get();
            assertNotNull("LiveData must have emitted a value", emitted);
            assertEquals(2, emitted.size());
            assertEquals("Pendiente", emitted.get(0).getNombre());
            assertEquals("EnPreparacion", emitted.get(1).getNombre());
        } finally {
            data.removeObserver(observer);
        }
    }

    @Test
    public void getMetodosPagoAndVentasLiveDataEmitLoadedLists() {
        FakeMetodoPagoApi pagos = new FakeMetodoPagoApi();
        pagos.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Efectivo")
        ));
        FakeMetodoVentaApi ventas = new FakeMetodoVentaApi();
        ventas.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(1, "Delivery")
        ));
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                successEstados(), pagos, ventas);

        assertEquals(1, repo.getMetodosPago().getValue().size());
        assertEquals("Efectivo", repo.getMetodosPago().getValue().get(0).getNombre());
        assertEquals(1, repo.getMetodosVenta().getValue().size());
        assertEquals("Delivery", repo.getMetodosVenta().getValue().get(0).getNombre());
    }

    @Test
    public void resolveEstadoIdReturnsCachedId() {
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(5, "Retirado"),
                new CatalogoItemDto(6, "Entregado")
        ));
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                estados, successPagos(), successVentas());

        assertEquals(5, repo.resolveEstadoId("Retirado"));
        assertEquals(6, repo.resolveEstadoId("Entregado"));
    }

    @Test
    public void resolveMetodoPagoIdAndMetodoVentaIdReturnCachedIds() {
        FakeMetodoPagoApi pagos = new FakeMetodoPagoApi();
        pagos.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(10, "MercadoPago"),
                new CatalogoItemDto(11, "Transferencia")
        ));
        FakeMetodoVentaApi ventas = new FakeMetodoVentaApi();
        ventas.nextResponse = Response.success(Arrays.asList(
                new CatalogoItemDto(20, "Mostrador")
        ));
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                successEstados(), pagos, ventas);

        assertEquals(10, repo.resolveMetodoPagoId("MercadoPago"));
        assertEquals(11, repo.resolveMetodoPagoId("Transferencia"));
        assertEquals(20, repo.resolveMetodoVentaId("Mostrador"));
    }

    @Test
    public void resolveReturnsNegativeOneForUnknownNameOnLoadedCache() {
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                successEstados(), successPagos(), successVentas());

        assertEquals(-1, repo.resolveEstadoId("NoExiste"));
        assertEquals(-1, repo.resolveMetodoPagoId("NoExiste"));
        assertEquals(-1, repo.resolveMetodoVentaId("NoExiste"));
    }

    // ------------------------------------------------------------------
    // LiveData identity contract — single instance per getter
    // ------------------------------------------------------------------

    @Test
    public void eachLiveDataGetterReturnsSameInstanceAcrossCalls() {
        CatalogoRepository repo = new CatalogoRepositoryImpl(
                successEstados(), successPagos(), successVentas());

        assertSame(repo.getEstadosPedido(), repo.getEstadosPedido());
        assertSame(repo.getMetodosPago(), repo.getMetodosPago());
        assertSame(repo.getMetodosVenta(), repo.getMetodosVenta());
    }

    @Test
    public void theThreeLiveDataInstancesArePairwiseDistinct() {
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                successEstados(), successPagos(), successVentas());

        assertTrue("estados vs pagos must be distinct",
                repo.getEstadosPedido() != repo.getMetodosPago());
        assertTrue("estados vs ventas must be distinct",
                repo.getEstadosPedido() != repo.getMetodosVenta());
        assertTrue("pagos vs ventas must be distinct",
                repo.getMetodosPago() != repo.getMetodosVenta());
    }

    // ------------------------------------------------------------------
    // Failure modes
    // ------------------------------------------------------------------

    @Test
    public void isReadyIsFalseWhenEstadosLoadFails() {
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextFailure = new IOException("down");
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                estados, successPagos(), successVentas());

        assertFalse("network failure on estados must keep isReady() false",
                repo.isReady());
    }

    @Test
    public void isReadyIsFalseWhenAnyCatalogLoadFails() {
        FakeMetodoPagoApi pagos = new FakeMetodoPagoApi();
        pagos.nextFailure = new IOException("down");
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                successEstados(), pagos, successVentas());

        assertFalse(repo.isReady());
    }

    @Test
    public void failedCatalogEmitsEmptyListOnItsLiveData() {
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextFailure = new IOException("down");
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                estados, successPagos(), successVentas());

        List<CatalogoItemDto> estadosValue = repo.getEstadosPedido().getValue();
        assertNotNull("even a failed catalog must emit (not stay null) so observers can react",
                estadosValue);
        assertTrue("failed catalog must emit an empty list", estadosValue.isEmpty());

        // The other catalogs still loaded their data (successPagos and
        // successVentas return an empty list, but the important thing
        // is that they emitted non-null — meaning the LiveData is
        // "ready for observation" and the failure didn't poison them).
        assertNotNull(repo.getMetodosPago().getValue());
        assertNotNull(repo.getMetodosVenta().getValue());
    }

    @Test
    public void resolveEstadoIdThrowsWhileCatalogIsStillLoading() {
        // Force the API to never call back synchronously by giving it
        // a response that is null (no callback fires) — simulates a
        // long-running request. isReady must stay false and resolve
        // must throw.
        FakeEstadosPedidoApi estados = new FakeEstadosPedidoApi();
        estados.nextResponse = null; // callback never fires
        CatalogoRepositoryImpl repo = new CatalogoRepositoryImpl(
                estados, successPagos(), successVentas());

        assertFalse(repo.isReady());
        try {
            repo.resolveEstadoId("Anything");
            fail("expected IllegalStateException while catalog is still loading");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    // ------------------------------------------------------------------
    // helpers — provide one-shot success APIs so each test sets up
    // only the catalogs it cares about.
    // ------------------------------------------------------------------

    private static FakeEstadosPedidoApi successEstados() {
        FakeEstadosPedidoApi api = new FakeEstadosPedidoApi();
        api.nextResponse = Response.success(new ArrayList<CatalogoItemDto>());
        return api;
    }

    private static FakeMetodoPagoApi successPagos() {
        FakeMetodoPagoApi api = new FakeMetodoPagoApi();
        api.nextResponse = Response.success(new ArrayList<CatalogoItemDto>());
        return api;
    }

    private static FakeMetodoVentaApi successVentas() {
        FakeMetodoVentaApi api = new FakeMetodoVentaApi();
        api.nextResponse = Response.success(new ArrayList<CatalogoItemDto>());
        return api;
    }

    // -- Fakes -----------------------------------------------------------

    static class FakeEstadosPedidoApi implements EstadosPedidoApi {
        Response<List<CatalogoItemDto>> nextResponse;
        Throwable nextFailure;

        @Override
        public Call<List<CatalogoItemDto>> getEstados() {
            return new FakeCall<>(nextResponse, nextFailure);
        }
    }

    static class FakeMetodoPagoApi implements MetodoPagoApi {
        Response<List<CatalogoItemDto>> nextResponse;
        Throwable nextFailure;

        @Override
        public Call<List<CatalogoItemDto>> getMetodosPago() {
            return new FakeCall<>(nextResponse, nextFailure);
        }
    }

    static class FakeMetodoVentaApi implements MetodoVentaApi {
        Response<List<CatalogoItemDto>> nextResponse;
        Throwable nextFailure;

        @Override
        public Call<List<CatalogoItemDto>> getMetodosVenta() {
            return new FakeCall<>(nextResponse, nextFailure);
        }
    }

    static final class FakeCall<T> implements Call<T> {
        private final Response<T> response;
        private final Throwable failure;

        FakeCall(Response<T> response, Throwable failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public Response<T> execute() throws IOException {
            if (failure != null) {
                if (failure instanceof IOException) throw (IOException) failure;
                throw new RuntimeException(failure);
            }
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            // The "never resolves" simulator requires BOTH response and
            // failure to be null. A failure-only setup (response == null,
            // failure != null) must still fire onFailure so the
            // repository can post its empty-list fallback.
            if (response == null && failure == null) {
                return;
            }
            if (failure != null) {
                callback.onFailure(this, failure);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override public boolean isExecuted() { return false; }
        @Override public void cancel() { }
        @Override public boolean isCanceled() { return false; }
        @Override public Call<T> clone() { return new FakeCall<>(response, failure); }
        @Override public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }
        @Override public okio.Timeout timeout() { return okio.Timeout.NONE; }
    }
}

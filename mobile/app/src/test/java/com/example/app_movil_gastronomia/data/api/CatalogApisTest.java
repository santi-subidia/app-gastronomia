package com.example.app_movil_gastronomia.data.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Verifies the wire-level contract of the three catalog API interfaces
 * declared for backend v2:
 *
 * <ul>
 *   <li>{@link EstadosPedidoApi} → {@code GET api/catalogo/estados-pedido}</li>
 *   <li>{@link MetodoPagoApi}    → {@code GET api/catalogo/metodos-pago}</li>
 *   <li>{@link MetodoVentaApi}   → {@code GET api/catalogo/metodos-venta}</li>
 * </ul>
 *
 * <p>Spec CAT-API-001: each interface must compile to a Retrofit proxy
 * that sends a GET request to the exact relative path documented in
 * {@code openspec/.../specs/catalogo-endpoints/spec.md} and returns
 * {@code Call<List<CatalogoItemDto>>}.</p>
 *
 * <p>The test builds a real Retrofit proxy against a dummy base URL
 * and inspects the {@link Request} that the call would execute. The
 * call is never enqueued — we only need the URL composition that
 * Retrofit computes from the {@code @GET} annotation.</p>
 */
public class CatalogApisTest {

    private static final String DUMMY_BASE_URL = "https://api.example.com/";

    private Retrofit retrofit;

    @Before
    public void setUp() {
        retrofit = new Retrofit.Builder()
                .baseUrl(DUMMY_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // ------------------------------------------------------------------
    // EstadosPedidoApi
    // ------------------------------------------------------------------

    @Test
    public void estadosPedidoApiSendsGetToEstadosPedidoPath() throws Exception {
        EstadosPedidoApi api = retrofit.create(EstadosPedidoApi.class);
        Call<List<CatalogoItemDto>> call = api.getEstados();

        assertNotNull("Call must not be null", call);
        Request request = call.request();
        assertEquals("GET", request.method());
        assertEquals("/api/catalogo/estados-pedido", request.url().encodedPath());
    }

    // ------------------------------------------------------------------
    // MetodoPagoApi
    // ------------------------------------------------------------------

    @Test
    public void metodoPagoApiSendsGetToMetodosPagoPath() throws Exception {
        MetodoPagoApi api = retrofit.create(MetodoPagoApi.class);
        Call<List<CatalogoItemDto>> call = api.getMetodosPago();

        assertNotNull("Call must not be null", call);
        Request request = call.request();
        assertEquals("GET", request.method());
        assertEquals("/api/catalogo/metodos-pago", request.url().encodedPath());
    }

    // ------------------------------------------------------------------
    // MetodoVentaApi
    // ------------------------------------------------------------------

    @Test
    public void metodoVentaApiSendsGetToMetodosVentaPath() throws Exception {
        MetodoVentaApi api = retrofit.create(MetodoVentaApi.class);
        Call<List<CatalogoItemDto>> call = api.getMetodosVenta();

        assertNotNull("Call must not be null", call);
        Request request = call.request();
        assertEquals("GET", request.method());
        assertEquals("/api/catalogo/metodos-venta", request.url().encodedPath());
    }

    // ------------------------------------------------------------------
    // Triangulation: every catalog API must hit a path under
    // /api/catalogo/ (defends against a typo in @GET on a new
    // catalog later).
    // ------------------------------------------------------------------

    @Test
    public void everyCatalogApiPathIsUnderCatalogNamespace() throws Exception {
        String[] paths = new String[]{
                retrofit.create(EstadosPedidoApi.class).getEstados().request().url().encodedPath(),
                retrofit.create(MetodoPagoApi.class).getMetodosPago().request().url().encodedPath(),
                retrofit.create(MetodoVentaApi.class).getMetodosVenta().request().url().encodedPath()
        };
        for (String path : paths) {
            assertTrue("path must end with api/catalogo/<resource>, got: " + path,
                    path.startsWith("/api/catalogo/") && !path.equals("/api/catalogo/"));
        }
    }
}

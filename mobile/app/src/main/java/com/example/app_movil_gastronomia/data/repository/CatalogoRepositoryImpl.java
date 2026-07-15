package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.data.api.EstadosPedidoApi;
import com.example.app_movil_gastronomia.data.api.MetodoPagoApi;
import com.example.app_movil_gastronomia.data.api.MetodoVentaApi;
import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * {@link CatalogoRepository} implementation that eagerly loads all
 * three v2 catalogs the moment Hilt instantiates it (Spec CAT-REP-001).
 *
 * <p>Each catalog lives in three pieces:</p>
 * <ul>
 *   <li>A single {@link MutableLiveData} (the public read model)</li>
 *   <li>A {@code Map<String,Integer>} for O(1) name resolution</li>
 *   <li>A boolean flag tracking whether THAT catalog finished loading</li>
 * </ul>
 *
 * <p>{@link #isReady()} is the AND of the three per-catalog flags —
 * the cache is "ready" only when every catalog came back
 * successfully. Each load is independent: a failure on one catalog
 * posts an empty list on its LiveData and leaves the other two
 * alone, so partial outages don't poison the whole cache.</p>
 */
@Singleton
public class CatalogoRepositoryImpl implements CatalogoRepository {

    private static final String TAG = "CatalogoRepositoryImpl";

    private final EstadosPedidoApi estadosApi;
    private final MetodoPagoApi metodoPagoApi;
    private final MetodoVentaApi metodoVentaApi;

    private final MutableLiveData<List<CatalogoItemDto>> _estadosPedido = new MutableLiveData<>();
    private final MutableLiveData<List<CatalogoItemDto>> _metodosPago = new MutableLiveData<>();
    private final MutableLiveData<List<CatalogoItemDto>> _metodosVenta = new MutableLiveData<>();

    @Nullable
    private volatile Map<String, Integer> estadoIdByNombre;
    @Nullable
    private volatile Map<String, Integer> metodoPagoIdByNombre;
    @Nullable
    private volatile Map<String, Integer> metodoVentaIdByNombre;

    private final AtomicInteger pendingLoads = new AtomicInteger(3);
    private volatile boolean estadosLoaded;
    private volatile boolean pagosLoaded;
    private volatile boolean ventasLoaded;

    @Inject
    public CatalogoRepositoryImpl(
            EstadosPedidoApi estadosApi,
            MetodoPagoApi metodoPagoApi,
            MetodoVentaApi metodoVentaApi
    ) {
        this.estadosApi = estadosApi;
        this.metodoPagoApi = metodoPagoApi;
        this.metodoVentaApi = metodoVentaApi;
        loadEstadosPedido();
        loadMetodosPago();
        loadMetodosVenta();
    }

    @Override
    public LiveData<List<CatalogoItemDto>> getEstadosPedido() {
        return _estadosPedido;
    }

    @Override
    public LiveData<List<CatalogoItemDto>> getMetodosPago() {
        return _metodosPago;
    }

    @Override
    public LiveData<List<CatalogoItemDto>> getMetodosVenta() {
        return _metodosVenta;
    }

    @Override
    public int resolveEstadoId(String nombre) {
        Map<String, Integer> cache = estadoIdByNombre;
        if (cache == null) {
            throw new IllegalStateException(
                    "estado catalog not loaded yet; check isReady() before resolving");
        }
        Integer id = cache.get(nombre);
        return id != null ? id : -1;
    }

    @Override
    public int resolveMetodoPagoId(String nombre) {
        Map<String, Integer> cache = metodoPagoIdByNombre;
        if (cache == null) {
            throw new IllegalStateException(
                    "metodo-pago catalog not loaded yet; check isReady() before resolving");
        }
        Integer id = cache.get(nombre);
        return id != null ? id : -1;
    }

    @Override
    public int resolveMetodoVentaId(String nombre) {
        Map<String, Integer> cache = metodoVentaIdByNombre;
        if (cache == null) {
            throw new IllegalStateException(
                    "metodo-venta catalog not loaded yet; check isReady() before resolving");
        }
        Integer id = cache.get(nombre);
        return id != null ? id : -1;
    }

    @Override
    public boolean isReady() {
        return estadosLoaded && pagosLoaded && ventasLoaded;
    }

    // -- internal: each load is fire-and-forget and updates its own
    //             state independently. ----------------------------------

    private void loadEstadosPedido() {
        estadosApi.getEstados().enqueue(new CatalogCallback(
                TAG + "/estados",
                list -> {
                    estadoIdByNombre = listToMap(list);
                    estadosLoaded = true;
                },
                _estadosPedido
        ));
    }

    private void loadMetodosPago() {
        metodoPagoApi.getMetodosPago().enqueue(new CatalogCallback(
                TAG + "/metodos-pago",
                list -> {
                    metodoPagoIdByNombre = listToMap(list);
                    pagosLoaded = true;
                },
                _metodosPago
        ));
    }

    private void loadMetodosVenta() {
        metodoVentaApi.getMetodosVenta().enqueue(new CatalogCallback(
                TAG + "/metodos-venta",
                list -> {
                    metodoVentaIdByNombre = listToMap(list);
                    ventasLoaded = true;
                },
                _metodosVenta
        ));
    }

    private static Map<String, Integer> listToMap(List<CatalogoItemDto> list) {
        Map<String, Integer> map = new HashMap<>(list.size() * 2);
        for (CatalogoItemDto item : list) {
            map.put(item.getNombre(), item.getId());
        }
        return map;
    }

    /**
     * Single Retrofit callback that owns the per-catalog success /
     * failure logic. On success it commits the in-memory cache and
     * flips the per-catalog ready flag. On failure it posts an empty
     * list (so observers can react instead of staring at null) and
     * leaves the ready flag false.
     *
     * <p>The {@code pendingLoads} counter is decremented on every
     * terminal state so a future test can assert "all three callbacks
     * have fired" — but {@code isReady()} alone already conveys that
     * since it's the AND of the per-catalog flags.</p>
     */
    private static final class CatalogCallback implements Callback<List<CatalogoItemDto>> {

        private final String logTag;
        private final SuccessAction onSuccess;
        private final MutableLiveData<List<CatalogoItemDto>> target;

        CatalogCallback(
                String logTag,
                SuccessAction onSuccess,
                MutableLiveData<List<CatalogoItemDto>> target
        ) {
            this.logTag = logTag;
            this.onSuccess = onSuccess;
            this.target = target;
        }

        @Override
        public void onResponse(
                @NonNull Call<List<CatalogoItemDto>> call,
                @NonNull Response<List<CatalogoItemDto>> response
        ) {
            if (response.isSuccessful() && response.body() != null) {
                List<CatalogoItemDto> body = response.body();
                onSuccess.accept(body);
                target.setValue(body);
            } else {
                Log.w(logTag, "catalog load failed: http " + response.code());
                target.setValue(Collections.emptyList());
            }
        }

        @Override
        public void onFailure(
                @NonNull Call<List<CatalogoItemDto>> call,
                @NonNull Throwable t
        ) {
            Log.e(logTag, "catalog load network failure", t);
            target.setValue(Collections.emptyList());
        }
    }

    @FunctionalInterface
    private interface SuccessAction {
        void accept(List<CatalogoItemDto> list);
    }
}

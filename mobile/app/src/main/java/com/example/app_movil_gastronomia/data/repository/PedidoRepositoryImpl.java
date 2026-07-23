package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.PedidoApi;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.dto.pedido.AsignarRepartidorRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owns a single {@link MutableLiveData} instance per method â€” one for
 * each of the 6 repository methods. Every instance is reset to LOADING
 * on its method call and then posted SUCCESS or ERROR. Instances are
 * never reallocated, so observers registered in the ViewModel
 * constructor (via {@code observeForever}) keep receiving emissions
 * across retries without leaking.
 *
 * <p>Spec PED-VAL-001 / PED-VAL-002: {@link #crearPedido(CrearPedidoRequest)}
 * performs client-side validation (non-empty detalles, delivery coords
 * when {@code metodoVentaId == 1}) BEFORE any network call and emits
 * ERROR directly without going through Retrofit.</p>
 *
 * <p>Spec PED-ENUM-002 (v2): {@link #cambiarEstado(int, EstadoPedidoEnum)}
 * resolves the enum to a catalog ID via {@link CatalogoRepository}
 * <b>before</b> hitting the API. The v2 backend expects a raw int
 * body, so the resolution has to happen on the client side. The
 * {@link CatalogoRepository} is injected so the dependency is
 * explicit and the repo can fail fast (clear error, no API call)
 * if the catalog is not yet loaded.</p>
 */
@Singleton
public class PedidoRepositoryImpl implements PedidoRepository {

    private static final String TAG = "PedidoRepositoryImpl";

    /**
     * Server identifier for the Delivery sales method. Spec PED-VAL-002:
     * when {@code metodoVentaId == DELIVERY_ID} the request MUST carry
     * both {@code latitudDestino} and {@code longitudDestino}.
     */
    private static final int DELIVERY_ID = 1;

    private final PedidoApi pedidoApi;
    private final CatalogoRepository catalogoRepository;

    private final MutableLiveData<UiState<List<PedidoResumenDto>>> _pedidosState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> _pedidoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<PedidoResumenDto>>> _byEstadoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> _crearState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> _cambiarEstadoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> _asignarRepartidorState = new MutableLiveData<>();

    @Inject
    public PedidoRepositoryImpl(PedidoApi pedidoApi, CatalogoRepository catalogoRepository) {
        this.pedidoApi = pedidoApi;
        this.catalogoRepository = catalogoRepository;
    }

    // ------------------------------------------------------------------
    // getPedidos
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<List<PedidoResumenDto>>> getPedidos() {
        // Reset the single shared instance to LOADING before the network call.
        _pedidosState.setValue(UiState.loading());

        pedidoApi.getPedidos().enqueue(new Callback<List<PedidoResumenDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PedidoResumenDto>> call,
                                   @NonNull Response<List<PedidoResumenDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _pedidosState.setValue(UiState.success(response.body()));
                } else {
                    _pedidosState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente mÃ¡s tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PedidoResumenDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetPedidos network failure", t);
                _pedidosState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getPedidosState();
    }

    @Override
    public LiveData<UiState<List<PedidoResumenDto>>> getPedidosPorRepartidor(int repartidorId) {
        _pedidosState.setValue(UiState.loading());
        pedidoApi.getPedidosPorRepartidor(repartidorId).enqueue(new Callback<List<PedidoResumenDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PedidoResumenDto>> call, @NonNull Response<List<PedidoResumenDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _pedidosState.setValue(UiState.success(response.body()));
                } else {
                    _pedidosState.setValue(UiState.error(parseMensaje(response, "Error obteniendo pedidos del repartidor")));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<PedidoResumenDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "getPedidosPorRepartidor failure", t);
                _pedidosState.setValue(UiState.error("No hay conexion a internet"));
            }
        });
        return getPedidosState();
    }

    @Override
    public LiveData<UiState<List<PedidoResumenDto>>> getPedidosState() {
        return _pedidosState;
    }

    // ------------------------------------------------------------------
    // getPedido
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<PedidoDetalleDto>> getPedido(int id) {
        _pedidoState.setValue(UiState.loading());

        pedidoApi.getPedido(id).enqueue(new Callback<PedidoDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PedidoDetalleDto> call,
                                   @NonNull Response<PedidoDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _pedidoState.setValue(UiState.success(response.body()));
                } else {
                    _pedidoState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente mÃ¡s tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PedidoDetalleDto> call, @NonNull Throwable t) {
                Log.e(TAG, "GetPedido network failure", t);
                _pedidoState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getPedidoState();
    }

    @Override
    public LiveData<UiState<PedidoDetalleDto>> getPedidoState() {
        return _pedidoState;
    }

    // ------------------------------------------------------------------
    // getByEstado
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<List<PedidoResumenDto>>> getByEstado(EstadoPedidoEnum estado) {
        _byEstadoState.setValue(UiState.loading());

        // Resolve the enum to its API string at the repo boundary so the
        // PedidoApi interface stays free of generic converters.
        final String estadoPath = estado.getApiValue();

        pedidoApi.getByEstado(estadoPath).enqueue(new Callback<List<PedidoResumenDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PedidoResumenDto>> call,
                                   @NonNull Response<List<PedidoResumenDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _byEstadoState.setValue(UiState.success(response.body()));
                } else {
                    _byEstadoState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente mÃ¡s tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PedidoResumenDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetByEstado network failure", t);
                _byEstadoState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getByEstadoState();
    }

    @Override
    public LiveData<UiState<List<PedidoResumenDto>>> getByEstadoState() {
        return _byEstadoState;
    }

    // ------------------------------------------------------------------
    // crearPedido
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<PedidoDetalleDto>> crearPedido(CrearPedidoRequest request) {
        // Spec PED-VAL-001 / PED-VAL-002: validate BEFORE any API call.
        // When validation fails the API is never called and the state
        // is set to ERROR directly (skipping LOADING).
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            _crearState.setValue(UiState.error("El pedido debe tener al menos un producto"));
            return getCrearState();
        }
        if (request.getMetodoVentaId() == DELIVERY_ID
                && (request.getLatitudDestino() == null || request.getLongitudDestino() == null)) {
            _crearState.setValue(UiState.error("Las coordenadas de destino son requeridas para Delivery"));
            return getCrearState();
        }

        _crearState.setValue(UiState.loading());

        pedidoApi.crearPedido(request).enqueue(new Callback<PedidoDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PedidoDetalleDto> call,
                                   @NonNull Response<PedidoDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _crearState.setValue(UiState.success(response.body()));
                } else {
                    ParsedError error = parseCrearError(response);
                    _crearState.setValue(UiState.error(error.message, error.code));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PedidoDetalleDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CrearPedido network failure", t);
                _crearState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getCrearState();
    }

    @Override
    public LiveData<UiState<PedidoDetalleDto>> getCrearState() {
        return _crearState;
    }

    @Override
    public void resetCrearState() {
        _crearState.setValue(null);
    }

    // ------------------------------------------------------------------
    // cambiarEstado
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<PedidoDetalleDto>> cambiarEstado(int id, EstadoPedidoEnum estado) {
        // Spec PED-ENUM-002 (v2): the backend wants a raw int body.
        // Resolve the enum via the catalog BEFORE any network call so
        // a missing / unloaded cache fails fast with a clear error.
        if (!catalogoRepository.isReady()) {
            _cambiarEstadoState.setValue(UiState.error(
                    "El catÃ¡logo de estados aÃºn no estÃ¡ disponible, intente nuevamente"));
            return getCambiarEstadoState();
        }
        final int nuevoEstadoId = catalogoRepository.resolveEstadoId(estado.getDbValue());
        if (nuevoEstadoId <= 0) {
            _cambiarEstadoState.setValue(UiState.error(
                    "Estado no reconocido por el catÃ¡logo: " + estado.getApiValue()));
            return getCambiarEstadoState();
        }

        _cambiarEstadoState.setValue(UiState.loading());

        pedidoApi.cambiarEstado(id, nuevoEstadoId).enqueue(new Callback<PedidoDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PedidoDetalleDto> call,
                                   @NonNull Response<PedidoDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _cambiarEstadoState.setValue(UiState.success(response.body()));
                } else {
                    _cambiarEstadoState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente mÃ¡s tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PedidoDetalleDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CambiarEstado network failure", t);
                _cambiarEstadoState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getCambiarEstadoState();
    }

    @Override
    public LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState() {
        return _cambiarEstadoState;
    }

    @Override
    public void resetCambiarEstadoState() {
        _cambiarEstadoState.setValue(null);
    }

    // ------------------------------------------------------------------
    // asignarRepartidor
    // ------------------------------------------------------------------

    @Override
    public LiveData<UiState<PedidoDetalleDto>> asignarRepartidor(int id, int repartidorId) {
        _asignarRepartidorState.setValue(UiState.loading());

        final AsignarRepartidorRequest body = new AsignarRepartidorRequest(repartidorId);

        pedidoApi.asignarRepartidor(id, body).enqueue(new Callback<PedidoDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PedidoDetalleDto> call,
                                   @NonNull Response<PedidoDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _asignarRepartidorState.setValue(UiState.success(response.body()));
                } else {
                    _asignarRepartidorState.setValue(UiState.error(
                            parseMensaje(response, "Error del servidor, intente mÃ¡s tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<PedidoDetalleDto> call, @NonNull Throwable t) {
                Log.e(TAG, "AsignarRepartidor network failure", t);
                _asignarRepartidorState.setValue(UiState.error("No hay conexiÃ³n a internet"));
            }
        });

        return getAsignarRepartidorState();
    }

    @Override
    public LiveData<UiState<PedidoDetalleDto>> getAsignarRepartidorState() {
        return _asignarRepartidorState;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Parses the server's {@code {"mensaje":"..."}} envelope from a Retrofit
     * error body, falling back to {@code fallback} when the body is missing
     * or unparseable. Mirrors the ProductoRepositoryImpl pattern.
     */
    private static String parseMensaje(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                if (errorResponse != null && errorResponse.getMensaje() != null) {
                    return errorResponse.getMensaje();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error body", e);
        }
        return fallback;
    }

    private static ParsedError parseCrearError(Response<?> response) {
        final String fallback = "Error del servidor, intente mÃ¡s tarde";
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                ErrorResponse errorResponse = new Gson().fromJson(errorBody, ErrorResponse.class);
                if (errorResponse != null) {
                    String code = errorResponse.getCodigo();
                    if ("NO_OPEN_REGISTER".equalsIgnoreCase(code)) {
                        return new ParsedError(code, "AbrÃ­ una caja antes de crear un pedido");
                    }
                    if (errorResponse.getMensaje() != null) {
                        return new ParsedError(code, errorResponse.getMensaje());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing create-order error body", e);
        }
        return new ParsedError(null, fallback);
    }

    private static final class ParsedError {
        private final String code;
        private final String message;

        private ParsedError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}


package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;

import java.util.List;

/**
 * Repository contract for the pedidos REST data layer.
 *
 * <p>Each method emits LOADING through a single dedicated
 * {@link LiveData} instance (exposed via a paired {@code getXxxState()}
 * getter) before the network call and posts SUCCESS or ERROR on the
 * Retrofit callback. The instances are never reallocated so observers
 * registered in a {@code ViewModel} constructor keep receiving
 * emissions across retries without leaking.</p>
 */
public interface PedidoRepository {

    /**
     * Fetches the full list of pedidos. Each call resets the dedicated
     * {@link #getPedidosState()} instance to LOADING and posts SUCCESS
     * (with the list) or ERROR.
     */
    LiveData<UiState<List<PedidoResumenDto>>> getPedidos();

    /**
     * Fetches the list of pedidos assigned to a specific driver.
     * Reuses getPedidosState() so observers don't need changing.
     */
    LiveData<UiState<List<PedidoResumenDto>>> getPedidosPorRepartidor(int repartidorId);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getPedidos()} calls.
     */
    LiveData<UiState<List<PedidoResumenDto>>> getPedidosState();

    /**
     * Fetches a single pedido by id. Each call resets the dedicated
     * {@link #getPedidoState()} instance to LOADING and posts SUCCESS
     * (with the dto) or ERROR.
     */
    LiveData<UiState<PedidoDetalleDto>> getPedido(int id);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getPedido(int)} calls.
     */
    LiveData<UiState<PedidoDetalleDto>> getPedidoState();

    /**
     * Fetches the pedidos filtered by estado. The enum is converted to
     * its API value via {@link EstadoPedidoEnum#getApiValue()} before
     * being sent as a path variable. Each call resets the dedicated
     * {@link #getByEstadoState()} instance to LOADING and posts SUCCESS
     * (with the list) or ERROR.
     */
    LiveData<UiState<List<PedidoResumenDto>>> getByEstado(EstadoPedidoEnum estado);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #getByEstado(EstadoPedidoEnum)} calls.
     */
    LiveData<UiState<List<PedidoResumenDto>>> getByEstadoState();

    /**
     * Creates a new pedido. Performs client-side validation
     * (non-empty detalles, delivery coords when metodoVentaId == 1)
     * BEFORE any network call: on validation failure the dedicated
     * {@link #getCrearState()} instance is set to ERROR and the API is
     * never called. On validation success the state is reset to LOADING
     * and SUCCESS (with the created dto) or ERROR is posted.
     */
    LiveData<UiState<PedidoDetalleDto>> crearPedido(CrearPedidoRequest request);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #crearPedido(CrearPedidoRequest)} calls.
     */
    LiveData<UiState<PedidoDetalleDto>> getCrearState();

    /**
     * Resets the crearState LiveData so that observers don't re-trigger
     * SUCCESS events when re-attaching.
     */
    void resetCrearState();

    /**
     * PATCH the estado of an existing pedido. The enum is resolved to
     * its catalog ID via {@code CatalogoRepository.resolveEstadoId()}
     * <b>before</b> the network call — the v2 endpoint accepts a raw
     * {@code int} body (no wrapper object). Each call resets the
     * dedicated {@link #getCambiarEstadoState()} instance to LOADING
     * and posts SUCCESS (with the updated dto) or ERROR. If the
     * catalog is not yet loaded, the call short-circuits with an
     * ERROR and never reaches the API.
     */
    LiveData<UiState<PedidoDetalleDto>> cambiarEstado(int id, EstadoPedidoEnum estado);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #cambiarEstado(int, EstadoPedidoEnum)} calls.
     */
    LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState();

    /**
     * Resets the cambiarEstadoState LiveData so that observers don't re-trigger
     * SUCCESS events when re-attaching.
     */
    void resetCambiarEstadoState();

    /**
     * PATCH the repartidor of an existing pedido. Each call resets the
     * dedicated {@link #getAsignarRepartidorState()} instance to LOADING
     * and posts SUCCESS (with the updated dto) or ERROR.
     */
    LiveData<UiState<PedidoDetalleDto>> asignarRepartidor(int id, int repartidorId);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #asignarRepartidor(int, int)} calls.
     */
    LiveData<UiState<PedidoDetalleDto>> getAsignarRepartidorState();
}

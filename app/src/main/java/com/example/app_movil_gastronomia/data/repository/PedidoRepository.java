package com.example.app_movil_gastronomia.data.repository;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.PedidoResumenDto;

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
     * PATCH the estado of an existing pedido. The enum is converted to
     * its API value via {@link EstadoPedidoEnum#getApiValue()} and
     * wrapped in a {@code CambiarEstadoRequest} body. Each call resets
     * the dedicated {@link #getCambiarEstadoState()} instance to LOADING
     * and posts SUCCESS (with the updated dto) or ERROR.
     */
    LiveData<UiState<PedidoDetalleDto>> cambiarEstado(int id, EstadoPedidoEnum estado);

    /**
     * Returns the single {@link LiveData} instance that holds the current
     * state of {@link #cambiarEstado(int, EstadoPedidoEnum)} calls.
     */
    LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState();

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

package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.data.dto.catalogo.CatalogoItemDto;

import java.util.List;

/**
 * In-memory cache of the three v2 backend catalogs (estados-pedido,
 * metodos-pago, metodos-venta).
 *
 * <p>Spec CAT-REP-001: the implementation loads all three catalogs
 * eagerly on construction. Consumers resolve a human-readable name
 * into the integer ID the v2 backend expects via {@code resolve*Id}
 * — that lookup is O(1) against the in-memory map. {@link #isReady()}
 * gates callers (e.g. estado PATCH in {@code PedidoRepositoryImpl})
 * so they can fail fast if the cache is incomplete.</p>
 *
 * <p>Failure semantics: if a catalog load fails, that catalog's
 * LiveData is posted as an empty list and {@code isReady()} stays
 * false. The other catalogs can still load successfully and be
 * resolved normally.</p>
 */
public interface CatalogoRepository {

    /**
     * Returns the single {@link LiveData} instance that holds the
     * estado-pedido catalog. The instance is never reallocated.
     */
    LiveData<List<CatalogoItemDto>> getEstadosPedido();

    /**
     * Returns the single {@link LiveData} instance that holds the
     * metodos-pago catalog. The instance is never reallocated.
     */
    LiveData<List<CatalogoItemDto>> getMetodosPago();

    /**
     * Returns the single {@link LiveData} instance that holds the
     * metodos-venta catalog. The instance is never reallocated.
     */
    LiveData<List<CatalogoItemDto>> getMetodosVenta();

    /**
     * Resolves an estado display name into its catalog ID.
     *
     * @param nombre human-readable name (e.g. "EnPreparacion")
     * @return the cached ID, or -1 if the name is not in the cache
     * @throws IllegalStateException if the estado catalog is not
     *         fully loaded yet ({@link #isReady()} is false)
     */
    int resolveEstadoId(String nombre);

    /**
     * Resolves a metodo-pago display name into its catalog ID.
     *
     * @see #resolveEstadoId(String) for failure semantics
     */
    int resolveMetodoPagoId(String nombre);

    /**
     * Resolves a metodo-venta display name into its catalog ID.
     *
     * @see #resolveEstadoId(String) for failure semantics
     */
    int resolveMetodoVentaId(String nombre);

    /**
     * @return true only when all three catalogs have been loaded
     *         successfully. If any load failed this stays false.
     */
    boolean isReady();
}

package com.example.app_movil_gastronomia.ui.pedido;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearDetalleRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * VM for the new-pedido form. Bridges two repository sources into
 * VM-owned LiveData:
 *
 * <ul>
 *   <li>{@link ProductoRepository#getProductListState()} — the catalog
 *       that backs the "Agregar Producto" picker dialog. Loaded once
 *       on construction; the fragment can re-invoke
 *       {@link #loadProductos()} to retry on error.</li>
 *   <li>{@link PedidoRepository#getCrearState()} — the result of the
 *       POST /api/pedidos call. Emits LOADING / SUCCESS / ERROR.</li>
 * </ul>
 *
 * <p>Validation lives here so the fragment stays a thin view layer.
 * {@link #validate(CrearPedidoRequest)} enforces the same rules the
 * server enforces (PED-VAL-001 / PED-VAL-002) plus the client-only
 * "customer name required" check. On validation failure the
 * {@link #formError} LiveData is updated and the API is never called.</p>
 */
@HiltViewModel
public class CrearPedidoViewModel extends ViewModel {

    /**
     * Server identifier for the Delivery sales method. The form sends
     * lat/lng only when this id is selected; matching
     * {@code PedidoRepositoryImpl.DELIVERY_ID}.
     */
    private static final int DELIVERY_ID = 1;

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    private final MutableLiveData<UiState<List<ProductoDto>>> productListState =
            new MutableLiveData<>();
    private final MutableLiveData<UiState<PedidoDetalleDto>> crearState =
            new MutableLiveData<>();
    private final MutableLiveData<String> formError = new MutableLiveData<>();

    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    private Observer<UiState<List<ProductoDto>>> productListObserver;
    private LiveData<UiState<List<ProductoDto>>> productListSource;

    private Observer<UiState<PedidoDetalleDto>> crearObserver;
    private LiveData<UiState<PedidoDetalleDto>> crearSource;

    @Inject
    public CrearPedidoViewModel(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;

        wireProductListObserver();
        wireCrearObserver();
    }

    // ------------------------------------------------------------------
    // Public state
    // ------------------------------------------------------------------

    public LiveData<UiState<List<ProductoDto>>> getProductListState() {
        return productListState;
    }

    public LiveData<UiState<PedidoDetalleDto>> getCrearState() {
        return crearState;
    }

    /** Returns whether the create-order error should offer the caja shortcut. */
    @VisibleForTesting
    static boolean shouldOfferOpenRegister(UiState<PedidoDetalleDto> state) {
        return state != null
                && state.getStatus() == UiState.Status.ERROR
                && "NO_OPEN_REGISTER".equalsIgnoreCase(state.getErrorCode());
    }

    /**
     * One-shot validation error message for the form. The fragment
     * consumes it on emission (e.g. via a Snackbar) and can call
     * {@link #acknowledgeFormError()} to clear it.
     */
    public LiveData<String> getFormError() {
        return formError;
    }

    public void acknowledgeFormError() {
        formError.setValue(null);
    }

    // ------------------------------------------------------------------
    // Intents
    // ------------------------------------------------------------------

    /** Reloads the product catalog. Used both for the initial load and retry. */
    public void loadProductos() {
        productoRepository.getProductos();
    }

    /**
     * Validates {@code request} and, on success, calls
     * {@link PedidoRepository#crearPedido(CrearPedidoRequest)} which
     * posts the result on {@link #getCrearState()}. On validation
     * failure the form error LiveData is updated and the API is never
     * called.
     */
    public void crearPedido(CrearPedidoRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            formError.setValue(validationError);
            return;
        }
        pedidoRepository.crearPedido(request);
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Returns {@code null} when the request is valid, otherwise a
     * user-facing error message describing the first failing rule.
     *
     * <p>Order of checks (matters for UX — most fundamental first):</p>
     * <ol>
     *   <li>Customer name not blank.</li>
     *   <li>At least one detalle line.</li>
     *   <li>When metodoVentaId == Delivery, lat / lng both provided.</li>
     * </ol>
     */
    @VisibleForTesting
    String validate(CrearPedidoRequest request) {
        if (request == null) {
            return "El pedido es inválido";
        }
        if (request.getClienteNombre() == null
                || request.getClienteNombre().trim().isEmpty()) {
            return "El nombre del cliente es requerido";
        }
        List<CrearDetalleRequest> detalles = request.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            return "Agregá al menos un producto";
        }
        if (request.getMetodoVentaId() == DELIVERY_ID) {
            if (request.getClienteDireccion() == null
                    || request.getClienteDireccion().trim().isEmpty()) {
                return "Dirección y coordenadas requeridas para Delivery";
            }
            if (request.getLatitudDestino() == null
                    || request.getLongitudDestino() == null) {
                return "Dirección y coordenadas requeridas para Delivery";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Wiring
    // ------------------------------------------------------------------

    private void wireProductListObserver() {
        productListObserver = productListState::setValue;
        productListSource = productoRepository.getProductListState();
        productListSource.observeForever(productListObserver);
        observerRegistrationCount.incrementAndGet();

        // Kick off the initial load.
        productoRepository.getProductos();
    }

    private void wireCrearObserver() {
        crearObserver = crearState::setValue;
        crearSource = pedidoRepository.getCrearState();
        crearSource.observeForever(crearObserver);
        observerRegistrationCount.incrementAndGet();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (productListSource != null && productListObserver != null) {
            productListSource.removeObserver(productListObserver);
        }
        if (crearSource != null && crearObserver != null) {
            crearSource.removeObserver(crearObserver);
        }
    }

    // ------------------------------------------------------------------
    // DetalleLine → CrearDetalleRequest mapping
    // ------------------------------------------------------------------

    /**
     * Maps the UI-layer {@link DetalleLine} list to a list of
     * {@link CrearDetalleRequest} wire DTOs, preserving order.
     *
     * <p>Spec PED-CRUD-001 / pedido-creacion "DetalleLine maps to
     * CrearDetalleRequest": every field of {@code DetalleLine} is
     * copied 1:1 to the matching DTO field. The mapping is one-way
     * (UI → wire) and happens exactly once, at submit time, in the
     * ViewModel so the {@code CrearPedidoFragment} and
     * {@code DetalleAdapter} never need to import the DTO.</p>
     *
     * <p>A {@code null} or empty input returns an empty list — never
     * {@code null} — so the caller can chain
     * {@code request.setDetalles(...)} unconditionally.</p>
     *
     * @param lines UI-layer detail lines; may be {@code null} or empty
     * @return a non-null, order-preserving list of DTOs (possibly empty)
     */
    public static List<CrearDetalleRequest> mapDetalles(List<DetalleLine> lines) {
        List<CrearDetalleRequest> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (DetalleLine line : lines) {
            out.add(new CrearDetalleRequest(
                    line.getProductoId(),
                    line.getNombre(),
                    line.getPrecio(),
                    line.getCantidad()
            ));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // Request building
    // ------------------------------------------------------------------

    /**
     * Builds the {@link CrearPedidoRequest} from primitive form values
     * plus the UI-layer {@link DetalleLine} list. The mapping from
     * {@code DetalleLine} to {@code CrearDetalleRequest} happens here
     * (via {@link #mapDetalles(List)}) so the fragment never needs to
     * import the wire DTO.
     *
     * <p>The {@code cajaId} and {@code demoraAprox} fields are left
     * {@code null} (server auto-assigns the caja, client UI does not
     * collect a demora yet).</p>
     *
     * <p>Spec PED-CRUD-001 / pedido-creacion "DetalleLine maps to
     * CrearDetalleRequest" — the resulting {@code detalles} list is
     * set on the request here, completing the UI → wire boundary.</p>
     */
    public CrearPedidoRequest buildRequest(
            String clienteNombre,
            int metodoVentaId,
            int metodoPagoId,
            String clienteDireccion,
            Double latitudDestino,
            Double longitudDestino,
            List<DetalleLine> detalles
    ) {
        CrearPedidoRequest request = new CrearPedidoRequest();
        request.setCajaId(null);
        request.setClienteNombre(clienteNombre);
        request.setMetodoVentaId(metodoVentaId);
        request.setMetodoPagoId(metodoPagoId);
        request.setClienteDireccion(clienteDireccion);
        request.setLatitudDestino(latitudDestino);
        request.setLongitudDestino(longitudDestino);
        request.setDemoraAprox(null);

        List<CrearDetalleRequest> detalleDtos = mapDetalles(detalles);
        double total = 0d;
        for (CrearDetalleRequest d : detalleDtos) {
            total += d.getPrecio() * d.getCantidad();
        }
        request.setTotalEstimado(total);
        request.setDetalles(detalleDtos);
        return request;
    }

    // ------------------------------------------------------------------
    // Test diagnostics
    // ------------------------------------------------------------------

    /** Test-only: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

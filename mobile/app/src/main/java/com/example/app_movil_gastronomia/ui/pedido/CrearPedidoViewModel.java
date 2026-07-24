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

@HiltViewModel
public class CrearPedidoViewModel extends ViewModel {

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

    public LiveData<UiState<List<ProductoDto>>> getProductListState() {
        return productListState;
    }

    public LiveData<UiState<PedidoDetalleDto>> getCrearState() {
        return crearState;
    }

    @VisibleForTesting
    static boolean shouldOfferOpenRegister(UiState<PedidoDetalleDto> state) {
        return state != null
                && state.getStatus() == UiState.Status.ERROR
                && "NO_OPEN_REGISTER".equalsIgnoreCase(state.getErrorCode());
    }

    public LiveData<String> getFormError() {
        return formError;
    }

    public void acknowledgeFormError() {
        formError.setValue(null);
    }

    public void loadProductos() {
        productoRepository.getProductos();
    }

    public void crearPedido(CrearPedidoRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            formError.setValue(validationError);
            return;
        }
        pedidoRepository.crearPedido(request);
    }

    public void resetCrearState() {
        pedidoRepository.resetCrearState();
    }

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

    private void wireProductListObserver() {
        productListObserver = productListState::setValue;
        productListSource = productoRepository.getProductListState();
        productListSource.observeForever(productListObserver);
        observerRegistrationCount.incrementAndGet();

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

        List<CrearDetalleRequest> detalleDtos = mapDetalles(detalles);
        double total = 0d;
        for (CrearDetalleRequest d : detalleDtos) {
            total += d.getPrecio() * d.getCantidad();
        }
        request.setTotalEstimado(total);
        request.setDetalles(detalleDtos);
        return request;
    }

    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

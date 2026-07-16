package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Bridges the {@link ProductoRepository} single-instance product list state
 * into a VM-owned LiveData. Registers an {@code observeForever} observer
 * exactly once in the constructor and removes it in {@link #onCleared()}.
 */
@HiltViewModel
public class ProductListViewModel extends ViewModel {

    private final ProductoRepository productoRepository;
    private final MutableLiveData<UiState<List<ProductoDto>>> productState = new MutableLiveData<>();
    private final MutableLiveData<UiState<String>> actionState = new MutableLiveData<>();
    private final Observer<UiState<List<ProductoDto>>> repositoryObserver;
    private final Observer<UiState<ProductoDto>> createObserver;
    private final Observer<UiState<ProductoDto>> updateObserver;
    private final Observer<UiState<Void>> deleteObserver;
    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    private Action pendingAction;

    private enum Action {
        CREATE,
        UPDATE,
        DELETE
    }

    @Inject
    public ProductListViewModel(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
        this.repositoryObserver = productState::setValue;
        this.createObserver = state -> handleProductAction(state, "Producto creado exitosamente");
        this.updateObserver = state -> handleProductAction(state, "Producto actualizado exitosamente");
        this.deleteObserver = state -> handleDeleteAction(state);
        // Register ONCE for the lifetime of this ViewModel.
        productoRepository.getProductListState().observeForever(repositoryObserver);
        productoRepository.getCrearState().observeForever(createObserver);
        productoRepository.getActualizarState().observeForever(updateObserver);
        productoRepository.getEliminarState().observeForever(deleteObserver);
        observerRegistrationCount.incrementAndGet();
        // Trigger the initial load.
        loadProductos();
    }

    public LiveData<UiState<List<ProductoDto>>> getProductState() {
        return productState;
    }

    public LiveData<UiState<String>> getActionState() {
        return actionState;
    }

    public void loadProductos() {
        productoRepository.getProductos();
    }

    public void retry() {
        loadProductos();
    }

    public void createProduct(String nombre, String precio, String demora) {
        ValidationResult validation = validateProductInput(nombre, precio, demora);
        if (!validation.isValid()) {
            actionState.setValue(UiState.error(validation.getError()));
            return;
        }

        pendingAction = Action.CREATE;
        productoRepository.crearProducto(new CrearProductoRequest(
                nombre.trim(), Double.parseDouble(precio.trim()), Integer.parseInt(demora.trim())));
    }

    public void updateProduct(int id, String nombre, String precio, String demora) {
        ValidationResult validation = validateProductInput(nombre, precio, demora);
        if (!validation.isValid()) {
            actionState.setValue(UiState.error(validation.getError()));
            return;
        }

        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setNombre(nombre.trim());
        request.setPrecio(Double.parseDouble(precio.trim()));
        request.setDemora(Integer.parseInt(demora.trim()));
        pendingAction = Action.UPDATE;
        productoRepository.actualizarProducto(id, request);
    }

    public void deleteProduct(int id) {
        pendingAction = Action.DELETE;
        productoRepository.eliminarProducto(id);
    }

    public static ValidationResult validateProductInput(String nombre, String precio, String demora) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return ValidationResult.invalid("El nombre es requerido");
        }

        try {
            double parsedPrice = Double.parseDouble(precio == null ? "" : precio.trim());
            if (!Double.isFinite(parsedPrice) || parsedPrice < 0) {
                return ValidationResult.invalid("Ingrese un precio válido");
            }
        } catch (NumberFormatException exception) {
            return ValidationResult.invalid("Ingrese un precio válido");
        }

        try {
            int parsedDelay = Integer.parseInt(demora == null ? "" : demora.trim());
            if (parsedDelay < 0) {
                return ValidationResult.invalid("Ingrese una demora válida");
            }
        } catch (NumberFormatException exception) {
            return ValidationResult.invalid("Ingrese una demora válida");
        }

        return ValidationResult.valid();
    }

    private void handleProductAction(UiState<ProductoDto> state, String successMessage) {
        if (state == null || pendingAction == null) return;
        switch (state.getStatus()) {
            case LOADING:
                actionState.setValue(UiState.loading());
                break;
            case SUCCESS:
                actionState.setValue(UiState.success(successMessage));
                pendingAction = null;
                loadProductos();
                break;
            case ERROR:
                actionState.setValue(UiState.error(state.getError()));
                break;
        }
    }

    private void handleDeleteAction(UiState<Void> state) {
        if (state == null || pendingAction == null) return;
        switch (state.getStatus()) {
            case LOADING:
                actionState.setValue(UiState.loading());
                break;
            case SUCCESS:
                actionState.setValue(UiState.success("Producto eliminado exitosamente"));
                pendingAction = null;
                loadProductos();
                break;
            case ERROR:
                actionState.setValue(UiState.error(state.getError()));
                break;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        productoRepository.getProductListState().removeObserver(repositoryObserver);
        productoRepository.getCrearState().removeObserver(createObserver);
        productoRepository.getActualizarState().removeObserver(updateObserver);
        productoRepository.getEliminarState().removeObserver(deleteObserver);
    }

    /** Test-only diagnostic: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String error;

        private ValidationResult(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        static ValidationResult invalid(String error) {
            return new ValidationResult(false, error);
        }

        public boolean isValid() {
            return valid;
        }

        public String getError() {
            return error;
        }
    }
}

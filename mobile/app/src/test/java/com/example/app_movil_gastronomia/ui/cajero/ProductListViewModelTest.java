package com.example.app_movil_gastronomia.ui.cajero;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ProductListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void validationRejectsEmptyNameAndInvalidNumbers() {
        ProductListViewModel.ValidationResult emptyName =
                ProductListViewModel.validateProductInput("  ", "100", "10");
        ProductListViewModel.ValidationResult invalidPrice =
                ProductListViewModel.validateProductInput("Pizza", "not-a-number", "10");
        ProductListViewModel.ValidationResult invalidDelay =
                ProductListViewModel.validateProductInput("Pizza", "100", "1.5");

        assertFalse(emptyName.isValid());
        assertEquals("El nombre es requerido", emptyName.getError());
        assertFalse(invalidPrice.isValid());
        assertEquals("Ingrese un precio válido", invalidPrice.getError());
        assertFalse(invalidDelay.isValid());
        assertEquals("Ingrese una demora válida", invalidDelay.getError());
    }

    @Test
    public void validationAcceptsDifferentValidProductValues() {
        ProductListViewModel.ValidationResult first =
                ProductListViewModel.validateProductInput("Pizza", "100.50", "10");
        ProductListViewModel.ValidationResult second =
                ProductListViewModel.validateProductInput("Empanada", "0", "0");

        assertTrue(first.isValid());
        assertTrue(second.isValid());
    }

    @Test
    public void invalidCreateEmitsErrorWithoutCallingRepository() {
        FakeProductoRepository repository = new FakeProductoRepository();
        ProductListViewModel viewModel = new ProductListViewModel(repository);

        viewModel.createProduct("", "100", "10");

        assertEquals(0, repository.createCalls);
        assertNotNull(viewModel.getActionState().getValue());
        assertEquals(UiState.Status.ERROR, viewModel.getActionState().getValue().getStatus());
        assertEquals("El nombre es requerido", viewModel.getActionState().getValue().getError());
    }

    @Test
    public void validCreateCallsRepositoryAndRefreshesProducts() {
        FakeProductoRepository repository = new FakeProductoRepository();
        ProductListViewModel viewModel = new ProductListViewModel(repository);

        viewModel.createProduct("Pizza", "1250.50", "15");

        assertEquals(1, repository.createCalls);
        assertEquals("Pizza", repository.createdRequest.getNombre());
        assertEquals(1250.50, repository.createdRequest.getPrecio(), 0.001);
        assertEquals(15, repository.createdRequest.getDemora());
        assertEquals(UiState.Status.SUCCESS, viewModel.getActionState().getValue().getStatus());
        assertEquals(2, repository.listCalls);
    }

    @Test
    public void validUpdateCallsRepositoryWithParsedValues() {
        FakeProductoRepository repository = new FakeProductoRepository();
        ProductListViewModel viewModel = new ProductListViewModel(repository);

        viewModel.updateProduct(7, "Empanada", "800", "12");

        assertEquals(1, repository.updateCalls);
        assertEquals(7, repository.updatedId);
        assertEquals("Empanada", repository.updatedRequest.getNombre());
        assertEquals(Double.valueOf(800), repository.updatedRequest.getPrecio());
        assertEquals(Integer.valueOf(12), repository.updatedRequest.getDemora());
        assertEquals(UiState.Status.SUCCESS, viewModel.getActionState().getValue().getStatus());
    }

    @Test
    public void deleteCallsRepositoryAndRefreshesProducts() {
        FakeProductoRepository repository = new FakeProductoRepository();
        ProductListViewModel viewModel = new ProductListViewModel(repository);

        viewModel.deleteProduct(11);

        assertEquals(1, repository.deleteCalls);
        assertEquals(11, repository.deletedId);
        assertEquals(UiState.Status.SUCCESS, viewModel.getActionState().getValue().getStatus());
        assertEquals(2, repository.listCalls);
    }

    @Test
    public void backendCreateFailureEmitsErrorAndDoesNotRefreshProducts() {
        FakeProductoRepository repository = new FakeProductoRepository();
        repository.createResult = UiState.error("No hay conexión a internet");
        ProductListViewModel viewModel = new ProductListViewModel(repository);

        viewModel.createProduct("Pizza", "100", "10");

        assertEquals(1, repository.createCalls);
        assertEquals(UiState.Status.ERROR, viewModel.getActionState().getValue().getStatus());
        assertEquals("No hay conexión a internet", viewModel.getActionState().getValue().getError());
        assertEquals(1, repository.listCalls);
    }

    private static final class FakeProductoRepository implements ProductoRepository {
        private final MutableLiveData<UiState<List<ProductoDto>>> listState =
                new MutableLiveData<>();
        private final MutableLiveData<UiState<ProductoDto>> getState = new MutableLiveData<>();
        private final MutableLiveData<UiState<ProductoDto>> createState = new MutableLiveData<>();
        private final MutableLiveData<UiState<ProductoDto>> updateState = new MutableLiveData<>();
        private final MutableLiveData<UiState<Void>> deleteState = new MutableLiveData<>();

        private int listCalls;
        private int createCalls;
        private int updateCalls;
        private int deleteCalls;
        private int updatedId;
        private int deletedId;
        private CrearProductoRequest createdRequest;
        private ActualizarProductoRequest updatedRequest;
        private UiState<ProductoDto> createResult = UiState.success(new ProductoDto());
        private UiState<ProductoDto> updateResult = UiState.success(new ProductoDto());
        private UiState<Void> deleteResult = UiState.success(null);

        @Override
        public LiveData<UiState<List<ProductoDto>>> getProductos() {
            listCalls++;
            listState.setValue(UiState.success(Collections.emptyList()));
            return listState;
        }

        @Override
        public LiveData<UiState<List<ProductoDto>>> getProductListState() {
            return listState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> getProducto(int id) {
            return getState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> getProductoState() {
            return getState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> crearProducto(CrearProductoRequest request) {
            createCalls++;
            createdRequest = request;
            createState.setValue(UiState.loading());
            createState.setValue(createResult);
            return createState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> getCrearState() {
            return createState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> actualizarProducto(
                int id, ActualizarProductoRequest request) {
            updateCalls++;
            updatedId = id;
            updatedRequest = request;
            updateState.setValue(UiState.loading());
            updateState.setValue(updateResult);
            return updateState;
        }

        @Override
        public LiveData<UiState<ProductoDto>> getActualizarState() {
            return updateState;
        }

        @Override
        public LiveData<UiState<Void>> eliminarProducto(int id) {
            deleteCalls++;
            deletedId = id;
            deleteState.setValue(UiState.loading());
            deleteState.setValue(deleteResult);
            return deleteState;
        }

        @Override
        public LiveData<UiState<Void>> getEliminarState() {
            return deleteState;
        }
    }
}

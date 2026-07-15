package com.example.app_movil_gastronomia.ui.cajero;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
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
    private final Observer<UiState<List<ProductoDto>>> repositoryObserver;
    private final AtomicInteger observerRegistrationCount = new AtomicInteger(0);

    @Inject
    public ProductListViewModel(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
        this.repositoryObserver = productState::setValue;
        // Register ONCE for the lifetime of this ViewModel.
        productoRepository.getProductListState().observeForever(repositoryObserver);
        observerRegistrationCount.incrementAndGet();
        // Trigger the initial load.
        loadProductos();
    }

    public LiveData<UiState<List<ProductoDto>>> getProductState() {
        return productState;
    }

    public void loadProductos() {
        productoRepository.getProductos();
    }

    public void retry() {
        loadProductos();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        productoRepository.getProductListState().removeObserver(repositoryObserver);
    }

    /** Test-only diagnostic: how many times the VM registered an observer. */
    @VisibleForTesting
    int getObserverRegistrationCount() {
        return observerRegistrationCount.get();
    }
}

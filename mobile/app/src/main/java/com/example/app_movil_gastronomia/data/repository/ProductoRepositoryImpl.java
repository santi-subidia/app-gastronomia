package com.example.app_movil_gastronomia.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.ProductoApi;
import com.example.app_movil_gastronomia.data.dto.producto.ActualizarProductoRequest;
import com.example.app_movil_gastronomia.data.dto.producto.CrearProductoRequest;
import com.example.app_movil_gastronomia.data.dto.ErrorResponse;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owns a single {@link MutableLiveData} instance per method — one for the
 * product list and one for each CRUD verb. Every instance is reset to
 * LOADING on its method call and then posted SUCCESS or ERROR. Instances
 * are never reallocated, so observers registered in the ViewModel
 * constructor (via {@code observeForever}) keep receiving emissions across
 * retries without leaking.
 */
@Singleton
public class ProductoRepositoryImpl implements ProductoRepository {

    private static final String TAG = "ProductoRepositoryImpl";

    private final ProductoApi productoApi;
    private final MutableLiveData<UiState<List<ProductoDto>>> _productListState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ProductoDto>> _productoState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ProductoDto>> _crearState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ProductoDto>> _actualizarState = new MutableLiveData<>();
    private final MutableLiveData<UiState<Void>> _eliminarState = new MutableLiveData<>();

    @Inject
    public ProductoRepositoryImpl(ProductoApi productoApi) {
        this.productoApi = productoApi;
    }

    @Override
    public LiveData<UiState<List<ProductoDto>>> getProductos() {
        // Reset the single shared instance to LOADING before the network call.
        _productListState.setValue(UiState.loading());

        productoApi.getProductos().enqueue(new Callback<List<ProductoDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ProductoDto>> call,
                                   @NonNull Response<List<ProductoDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _productListState.setValue(UiState.success(response.body()));
                } else {
                    _productListState.setValue(UiState.error("Error del servidor, intente más tarde"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ProductoDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "GetProductos network failure", t);
                _productListState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getProductListState();
    }

    @Override
    public LiveData<UiState<List<ProductoDto>>> getProductListState() {
        return _productListState;
    }

    @Override
    public LiveData<UiState<ProductoDto>> getProducto(int id) {
        _productoState.setValue(UiState.loading());

        productoApi.getProducto(id).enqueue(new Callback<ProductoDto>() {
            @Override
            public void onResponse(@NonNull Call<ProductoDto> call,
                                   @NonNull Response<ProductoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _productoState.setValue(UiState.success(response.body()));
                } else {
                    _productoState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductoDto> call, @NonNull Throwable t) {
                Log.e(TAG, "GetProducto network failure", t);
                _productoState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getProductoState();
    }

    @Override
    public LiveData<UiState<ProductoDto>> getProductoState() {
        return _productoState;
    }

    @Override
    public LiveData<UiState<ProductoDto>> crearProducto(CrearProductoRequest request) {
        _crearState.setValue(UiState.loading());

        productoApi.crearProducto(request).enqueue(new Callback<ProductoDto>() {
            @Override
            public void onResponse(@NonNull Call<ProductoDto> call,
                                   @NonNull Response<ProductoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _crearState.setValue(UiState.success(response.body()));
                } else {
                    _crearState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductoDto> call, @NonNull Throwable t) {
                Log.e(TAG, "CrearProducto network failure", t);
                _crearState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getCrearState();
    }

    @Override
    public LiveData<UiState<ProductoDto>> getCrearState() {
        return _crearState;
    }

    @Override
    public LiveData<UiState<ProductoDto>> actualizarProducto(int id, ActualizarProductoRequest request) {
        _actualizarState.setValue(UiState.loading());

        productoApi.actualizarProducto(id, request).enqueue(new Callback<ProductoDto>() {
            @Override
            public void onResponse(@NonNull Call<ProductoDto> call,
                                   @NonNull Response<ProductoDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    _actualizarState.setValue(UiState.success(response.body()));
                } else {
                    _actualizarState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProductoDto> call, @NonNull Throwable t) {
                Log.e(TAG, "ActualizarProducto network failure", t);
                _actualizarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getActualizarState();
    }

    @Override
    public LiveData<UiState<ProductoDto>> getActualizarState() {
        return _actualizarState;
    }

    @Override
    public LiveData<UiState<Void>> eliminarProducto(int id) {
        _eliminarState.setValue(UiState.loading());

        productoApi.eliminarProducto(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call,
                                   @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    _eliminarState.setValue(UiState.success(null));
                } else {
                    _eliminarState.setValue(UiState.error(parseMensaje(response, "Error del servidor, intente más tarde")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "EliminarProducto network failure", t);
                _eliminarState.setValue(UiState.error("No hay conexión a internet"));
            }
        });

        return getEliminarState();
    }

    @Override
    public LiveData<UiState<Void>> getEliminarState() {
        return _eliminarState;
    }

    /**
     * Parses the server's {@code {"mensaje":"..."}} envelope from a Retrofit
     * error body, falling back to {@code fallback} when the body is missing
     * or unparseable. Mirrors the AuthRepositoryImpl.login() pattern.
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
        } catch (IOException e) {
            Log.e(TAG, "Error parsing error body", e);
        }
        return fallback;
    }
}

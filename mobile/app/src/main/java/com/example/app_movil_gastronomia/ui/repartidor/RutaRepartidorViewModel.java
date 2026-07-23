package com.example.app_movil_gastronomia.ui.repartidor;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.OsrmApi;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.routing.OsrmRouteResponse;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;

import java.util.Locale;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import dagger.hilt.android.lifecycle.HiltViewModel;

/** Coordinates order destination state and OSRM route requests for the map screen. */
@HiltViewModel
public class RutaRepartidorViewModel extends ViewModel {

    private final OsrmApi osrmApi;
    @Nullable
    private final PedidoRepository pedidoRepository;

    private final MutableLiveData<UiState<PedidoDetalleDto>> destinationState = new MutableLiveData<>();
    private final MutableLiveData<UiState<OsrmRouteResponse>> routeState = new MutableLiveData<>();
    private final MutableLiveData<double[]> driverLocation = new MutableLiveData<>();

    private final Observer<UiState<PedidoDetalleDto>> pedidoObserver;
    @Nullable
    private PedidoDetalleDto destination;
    private boolean routeRequested;

    @Inject
    public RutaRepartidorViewModel(OsrmApi osrmApi, @Nullable PedidoRepository pedidoRepository) {
        this.osrmApi = osrmApi;
        this.pedidoRepository = pedidoRepository;
        this.pedidoObserver = state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                applyDestination(state.getData());
            } else if (state.getStatus() == UiState.Status.ERROR) {
                destinationState.setValue(UiState.error(state.getError()));
            }
        };
        if (pedidoRepository != null) {
            pedidoRepository.getPedidoState().observeForever(pedidoObserver);
        }
    }

    public LiveData<UiState<PedidoDetalleDto>> getDestinationState() {
        return destinationState;
    }

    public LiveData<UiState<OsrmRouteResponse>> getRouteState() {
        return routeState;
    }

    public LiveData<double[]> getDriverLocation() {
        return driverLocation;
    }

    public void loadPedido(int pedidoId) {
        if (pedidoRepository != null && pedidoId > 0) {
            pedidoRepository.getPedido(pedidoId);
        }
    }

    public void onDriverLocationChanged(double latitude, double longitude) {
        driverLocation.setValue(new double[]{latitude, longitude});
        if (destination != null && !routeRequested) {
            requestRoute(latitude, longitude, destination);
        }
    }

    /** Package-private for deterministic unit tests and repository callbacks. */
    void applyDestination(PedidoDetalleDto pedido) {
        routeRequested = false;
        if (pedido == null || pedido.getLatitudDestino() == null || pedido.getLongitudDestino() == null) {
            destination = null;
            destinationState.setValue(UiState.error("El pedido no tiene coordenadas de destino"));
            return;
        }
        destination = pedido;
        destinationState.setValue(UiState.success(pedido));
        double[] current = driverLocation.getValue();
        if (current != null) {
            requestRoute(current[0], current[1], pedido);
        }
    }

    private void requestRoute(double latitude, double longitude, PedidoDetalleDto pedido) {
        if (osrmApi == null) return;

        routeRequested = true;
        routeState.setValue(UiState.loading());
        String coordinates = buildCoordinates(
                longitude, latitude,
                pedido.getLongitudDestino(), pedido.getLatitudDestino());

        osrmApi.getRoute(coordinates, "full", "geojson").enqueue(new Callback<OsrmRouteResponse>() {
            @Override
            public void onResponse(Call<OsrmRouteResponse> call, Response<OsrmRouteResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && "Ok".equalsIgnoreCase(response.body().getCode())
                        && response.body().getRouteCoordinates().size() >= 2) {
                    routeRequested = false;
                    routeState.setValue(UiState.success(response.body()));
                } else {
                    routeRequested = false;
                    routeState.setValue(UiState.error("No se pudo calcular la ruta"));
                }
            }

            @Override
            public void onFailure(Call<OsrmRouteResponse> call, Throwable throwable) {
                routeRequested = false;
                routeState.setValue(UiState.error("No hay conexión para calcular la ruta"));
            }
        });
    }

    static String buildCoordinates(double originLongitude, double originLatitude,
                                   double destinationLongitude, double destinationLatitude) {
        return String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f",
                originLongitude, originLatitude, destinationLongitude, destinationLatitude);
    }

    /** Package-private for unit tests that validate response-to-state transitions. */
    void applyRouteResponse(Response<OsrmRouteResponse> response) {
        if (response.isSuccessful() && response.body() != null
                && "Ok".equalsIgnoreCase(response.body().getCode())
                && response.body().getRouteCoordinates().size() >= 2) {
            routeRequested = false;
            routeState.setValue(UiState.success(response.body()));
        } else {
            routeRequested = false;
            routeState.setValue(UiState.error("No se pudo calcular la ruta"));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (pedidoRepository != null) {
            pedidoRepository.getPedidoState().removeObserver(pedidoObserver);
        }
    }
}

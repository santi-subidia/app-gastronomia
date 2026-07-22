package com.example.app_movil_gastronomia.ui.repartidor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.OsrmApi;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.routing.OsrmRouteResponse;

import com.google.gson.Gson;

import org.junit.Rule;
import org.junit.Test;

import retrofit2.Response;

/** Covers destination validation and route state transitions without Android UI. */
public class RutaRepartidorViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void validDestinationThenRouteResponseProducesSuccessStates() {
        RutaRepartidorViewModel viewModel = new RutaRepartidorViewModel((OsrmApi) null, null);
        PedidoDetalleDto pedido = new PedidoDetalleDto();
        pedido.setLatitudDestino(-34.6);
        pedido.setLongitudDestino(-58.4);

        viewModel.applyDestination(pedido);
        assertEquals(UiState.Status.SUCCESS, viewModel.getDestinationState().getValue().getStatus());

        OsrmRouteResponse route = new Gson().fromJson(
                "{\"code\":\"Ok\",\"routes\":[{\"geometry\":"
                        + "{\"type\":\"LineString\",\"coordinates\":[[-58.5,-34.7],[-58.4,-34.6]]}}]}",
                OsrmRouteResponse.class);
        viewModel.applyRouteResponse(Response.success(route));

        assertNotNull(viewModel.getRouteState().getValue());
        assertEquals(UiState.Status.SUCCESS, viewModel.getRouteState().getValue().getStatus());
    }

    @Test
    public void missingDestinationCoordinatesProducesError() {
        RutaRepartidorViewModel viewModel = new RutaRepartidorViewModel((OsrmApi) null, null);
        viewModel.applyDestination(new PedidoDetalleDto());

        assertEquals(UiState.Status.ERROR, viewModel.getDestinationState().getValue().getStatus());
    }

    @Test
    public void coordinatesUseOsrmLongitudeLatitudeOrder() {
        assertEquals("-58.400000,-34.600000;-58.300000,-34.500000",
                RutaRepartidorViewModel.buildCoordinates(-58.4, -34.6, -58.3, -34.5));
    }
}

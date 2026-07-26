package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;

import java.util.List;

public interface PedidoRepository {

    LiveData<UiState<List<PedidoResumenDto>>> getPedidos();

    LiveData<UiState<List<PedidoResumenDto>>> getPedidosPorRepartidor(int repartidorId);

    LiveData<UiState<List<PedidoResumenDto>>> getPedidosState();

    LiveData<UiState<PedidoDetalleDto>> getPedido(int id);

    LiveData<UiState<PedidoDetalleDto>> getPedidoState();

    LiveData<UiState<List<PedidoResumenDto>>> getByEstado(EstadoPedidoEnum estado);

    LiveData<UiState<List<PedidoResumenDto>>> getByEstadoState();

    LiveData<UiState<PedidoDetalleDto>> crearPedido(CrearPedidoRequest request);

    LiveData<UiState<PedidoDetalleDto>> getCrearState();

    void resetCrearState();

    LiveData<UiState<PedidoDetalleDto>> cambiarEstado(int id, EstadoPedidoEnum estado);

    LiveData<UiState<PedidoDetalleDto>> getCambiarEstadoState();

    void resetCambiarEstadoState();

    LiveData<UiState<PedidoDetalleDto>> asignarRepartidor(int id, int repartidorId);

    LiveData<UiState<PedidoDetalleDto>> getAsignarRepartidorState();

    void resetAsignarRepartidorState();

    LiveData<UiState<PedidoDetalleDto>> reintentarEnCocina(int id);

    LiveData<UiState<PedidoDetalleDto>> getReintentarEnCocinaState();

    void resetReintentarEnCocinaState();
}

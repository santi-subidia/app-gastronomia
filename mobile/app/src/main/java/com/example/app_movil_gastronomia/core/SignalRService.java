package com.example.app_movil_gastronomia.core;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.EstadoCambiadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.EstimacionPedidoActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.NuevoPedidoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.PosicionGPSActualizadaMessage;
import com.example.app_movil_gastronomia.data.dto.signalr.RepartidorAsignadoMessage;


public interface SignalRService {

    void connect(String token);

    void disconnect();

    void unirseACocina();

    void unirseAPedido(int pedidoId);

    void salirDePedido(int pedidoId);

    void enviarPosicion(int repartidorId, double lat, double lng);
    LiveData<NuevoPedidoMessage> getNuevoPedido();
    LiveData<EstadoCambiadoMessage> getEstadoCambiado();
    LiveData<EstimacionPedidoActualizadaMessage> getEstimacionPedidoActualizada();
    LiveData<RepartidorAsignadoMessage> getRepartidorAsignado();
    LiveData<DemoraRegistradaMessage> getDemoraRegistrada();
    LiveData<PosicionGPSActualizadaMessage> getPosicionGPSActualizada();
    LiveData<PedidoFinalizadoMessage> getPedidoFinalizado();
    LiveData<Boolean> getConnected();
    LiveData<String> getError();
}

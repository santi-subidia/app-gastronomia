package com.example.app_movil_gastronomia.ui.pedido;

import androidx.annotation.Nullable;

import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;

/** Defines the active order states that can be force-canceled by the UI. */
final class PedidoCancellationPolicy {

    private PedidoCancellationPolicy() {
    }

    static boolean isCancelable(@Nullable EstadoPedidoEnum estado) {
        if (estado == null) return false;
        switch (estado) {
            case PENDIENTE:
            case EN_PREPARACION:
            case LISTO_PARA_RETIRAR:
            case EN_CAMINO:
                return true;
            default:
                return false;
        }
    }
}

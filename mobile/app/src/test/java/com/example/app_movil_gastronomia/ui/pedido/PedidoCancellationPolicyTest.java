package com.example.app_movil_gastronomia.ui.pedido;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;

import org.junit.Test;

/** Unit coverage for which active orders may be force-canceled. */
public class PedidoCancellationPolicyTest {

    @Test
    public void activeStatesCanBeCanceled() {
        assertTrue(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.PENDIENTE));
        assertTrue(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.EN_PREPARACION));
        assertTrue(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.LISTO_PARA_RETIRAR));
        assertTrue(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.EN_CAMINO));
    }

    @Test
    public void terminalStatesCannotBeCanceledAgain() {
        assertFalse(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.ENTREGADO));
        assertFalse(PedidoCancellationPolicy.isCancelable(EstadoPedidoEnum.CANCELADO));
        assertFalse(PedidoCancellationPolicy.isCancelable(null));
    }
}

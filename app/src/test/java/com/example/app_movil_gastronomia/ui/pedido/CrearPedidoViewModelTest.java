package com.example.app_movil_gastronomia.ui.pedido;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearDetalleRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link CrearPedidoViewModel#mapDetalles(List)}.
 *
 * <p>Spec PED-CRUD-001 / pedido-creacion "DetalleLine maps to
 * CrearDetalleRequest" — each DetalleLine produced by the
 * {@link DetalleAdapter} / {@code CrearPedidoFragment} must be mapped
 * to a {@link CrearDetalleRequest} with identical field values at
 * submit time. The ViewModel owns this mapping so the DTO stays a
 * pure wire contract and the UI layer never imports the DTO.</p>
 */
public class CrearPedidoViewModelTest {

    @Test
    public void shouldOfferOpenRegisterOnlyForMissingRegisterError() {
        UiState<PedidoDetalleDto> missingRegister = UiState.error(
                "Abrí una caja antes de crear un pedido", "NO_OPEN_REGISTER");
        UiState<PedidoDetalleDto> otherError = UiState.error(
                "No hay conexión a internet", null);

        assertTrue(CrearPedidoViewModel.shouldOfferOpenRegister(missingRegister));
        assertTrue(!CrearPedidoViewModel.shouldOfferOpenRegister(otherError));
    }

    /**
     * Single-line happy path: a DetalleLine with non-trivial values
     * maps to a CrearDetalleRequest with all four fields matching.
     */
    @Test
    public void mapDetalles_singleLine_copiesAllFourFields() {
        DetalleLine line = new DetalleLine(42, "Pizza Muzza", 1500.0, 3);

        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(
                Collections.singletonList(line));

        assertNotNull(out);
        assertEquals(1, out.size());
        CrearDetalleRequest r = out.get(0);
        assertEquals(42, r.getProductoId());
        assertEquals("Pizza Muzza", r.getNombre());
        assertEquals(1500.0, r.getPrecio(), 0.0);
        assertEquals(3, r.getCantidad());
    }

    /**
     * Multi-line list maps 1:1, preserving order. The submit
     * pipeline uses positional ordering to compute {@code totalEstimado},
     * so the mapping must NOT shuffle the input.
     */
    @Test
    public void mapDetalles_multipleLines_preservesOrderAndValues() {
        DetalleLine a = new DetalleLine(10, "Pizza", 500.0, 2);
        DetalleLine b = new DetalleLine(20, "Coca", 100.0, 4);
        DetalleLine c = new DetalleLine(30, "Flan", 250.5, 1);

        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(
                Arrays.asList(a, b, c));

        assertEquals(3, out.size());

        CrearDetalleRequest r0 = out.get(0);
        assertEquals(10, r0.getProductoId());
        assertEquals("Pizza", r0.getNombre());
        assertEquals(500.0, r0.getPrecio(), 0.0);
        assertEquals(2, r0.getCantidad());

        CrearDetalleRequest r1 = out.get(1);
        assertEquals(20, r1.getProductoId());
        assertEquals("Coca", r1.getNombre());
        assertEquals(100.0, r1.getPrecio(), 0.0);
        assertEquals(4, r1.getCantidad());

        CrearDetalleRequest r2 = out.get(2);
        assertEquals(30, r2.getProductoId());
        assertEquals("Flan", r2.getNombre());
        assertEquals(250.5, r2.getPrecio(), 0.0);
        assertEquals(1, r2.getCantidad());
    }

    /**
     * Empty list maps to empty list (not null). The fragment passes
     * the result to {@code CrearPedidoRequest.setDetalles}, which
     * should always receive a list — never null — to keep the JSON
     * serialization deterministic.
     */
    @Test
    public void mapDetalles_emptyList_returnsEmptyList() {
        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(
                new ArrayList<>());
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    /**
     * Null input is treated as empty list. Defensive: the fragment
     * can be in an early state where {@code detalles} hasn't been
     * initialised yet.
     */
    @Test
    public void mapDetalles_nullInput_returnsEmptyList() {
        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(null);
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    /**
     * A null {@code nombre} in the UI model is propagated as-is to
     * the DTO. The DTO field is non-primitive, so a null is a legal
     * value at the type level; the fragment should never produce
     * this state (it always picks a product from the picker), but
     * the mapping must not crash if it does.
     */
    @Test
    public void mapDetalles_nullNombre_isPropagatedToDto() {
        DetalleLine line = new DetalleLine(1, null, 0.0, 1);

        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(
                Collections.singletonList(line));

        assertEquals(1, out.size());
        assertEquals(null, out.get(0).getNombre());
    }
}

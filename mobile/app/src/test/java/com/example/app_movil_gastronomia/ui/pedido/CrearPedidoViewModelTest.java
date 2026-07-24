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
 * Pruebas unitarias de {@link CrearPedidoViewModel#mapDetalles(List)}.
 *
 * <p>Especificación PED-CRUD-001: "DetalleLine se convierte en
 * CrearDetalleRequest". Cada línea producida por la interfaz se convierte
 * en un {@link CrearDetalleRequest} con los mismos valores. El ViewModel
 * realiza esta conversión para mantener separado el DTO de red.</p>
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
     * Una línea válida se convierte conservando sus cuatro campos.
     * y se convierte conservando sus cuatro campos.
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
     * Una lista de varias líneas conserva la correspondencia y el orden,
     * necesario para calcular {@code totalEstimado}.
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
     * Una lista vacía se convierte en otra lista vacía, nunca en null,
     * para mantener estable la serialización JSON.
     */
    @Test
    public void mapDetalles_emptyList_returnsEmptyList() {
        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(
                new ArrayList<>());
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    /**
     * Una entrada null se trata como una lista vacía para cubrir estados
     * iniciales donde {@code detalles} todavía no fue inicializado.
     */
    @Test
    public void mapDetalles_nullInput_returnsEmptyList() {
        List<CrearDetalleRequest> out = CrearPedidoViewModel.mapDetalles(null);
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    /**
     * Un {@code nombre} null del modelo de UI se conserva en el DTO.
     * La conversión no debe fallar aunque la interfaz normalmente lo evite.
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

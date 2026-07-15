package com.example.app_movil_gastronomia.ui.pedido;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * Locks the Android resource and source contracts for caja restriction UX.
 * The tests keep the user-visible cancel action and register shortcut from
 * being removed while the surrounding screens are refactored.
 */
public class CajaRestrictionUiContractTest {

    @Test
    public void orderDetailContainsExplicitCancelAction() throws IOException {
        String layout = read("app/src/main/res/layout/fragment_pedido_detail.xml");
        String source = read("app/src/main/java/com/example/app_movil_gastronomia/ui/pedido/PedidoDetailFragment.java");

        assertTrue(layout.contains("@+id/button_cancelar_pedido"));
        assertTrue(source.contains("EstadoPedidoEnum.CANCELADO"));
    }

    @Test
    public void createOrderCanNavigateToOpenRegister() throws IOException {
        String navigation = read("app/src/main/res/navigation/mobile_navigation.xml");
        String source = read("app/src/main/java/com/example/app_movil_gastronomia/ui/pedido/CrearPedidoFragment.java");

        assertTrue(navigation.contains("action_nav_crear_pedido_to_nav_caja"));
        assertTrue(source.contains("action_nav_crear_pedido_to_nav_caja"));
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("..", relativePath);
        }
        assertTrue("Expected resource to exist: " + path, Files.exists(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

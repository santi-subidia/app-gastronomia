package com.example.app_movil_gastronomia.ui.pedido;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

/** Locks the cashier-only delay history entry point and its UI states. */
public class DelayHistoryUiContractTest {

    @Test
    public void cashierDetailExposesDelayHistoryStates() throws IOException {
        String layout = read("app/src/main/res/layout/fragment_pedido_detail.xml");
        String source = read("app/src/main/java/com/example/app_movil_gastronomia/ui/pedido/PedidoDetailFragment.java");
        String viewModel = read("app/src/main/java/com/example/app_movil_gastronomia/ui/pedido/PedidoDetailViewModel.java");

        assertTrue(layout.contains("@+id/button_ver_demoras"));
        assertTrue(layout.contains("@+id/demoras_progress_bar"));
        assertTrue(layout.contains("@+id/demoras_text_empty"));
        assertTrue(layout.contains("@+id/demoras_text_error"));
        assertTrue(layout.contains("@+id/demoras_container"));
        assertTrue(source.contains("boolean isCajero"));
        assertTrue(source.contains("binding.buttonVerDemoras.setVisibility(View.VISIBLE)"));
        assertTrue(source.contains("showDemorasLoading"));
        assertTrue(source.contains("showDemorasContent"));
        assertTrue(source.contains("showDemorasError"));
        assertTrue(source.contains("getDemoraMinutos()"));
        assertTrue(source.contains("getObservaciones()"));
        assertTrue(viewModel.contains("demoraRepository.getDemoras(pedidoId)"));
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

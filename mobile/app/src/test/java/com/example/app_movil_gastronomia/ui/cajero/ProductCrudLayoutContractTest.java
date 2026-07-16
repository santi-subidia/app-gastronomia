package com.example.app_movil_gastronomia.ui.cajero;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Locks the resource contracts required by the product CRUD screen.
 * These tests intentionally fail until the corresponding layouts are added.
 */
public class ProductCrudLayoutContractTest {

    @Test
    public void productFormContainsAllEditableFields() throws IOException {
        String layout = read("app/src/main/res/layout/dialog_product_form.xml");

        assertTrue(layout.contains("@+id/input_product_name"));
        assertTrue(layout.contains("@+id/input_product_price"));
        assertTrue(layout.contains("@+id/input_product_delay"));
    }

    @Test
    public void productListContainsCreateAction() throws IOException {
        String layout = read("app/src/main/res/layout/fragment_product_list.xml");

        assertTrue(layout.contains("FloatingActionButton"));
        assertTrue(layout.contains("@+id/fab_add_product"));
    }

    @Test
    public void productItemContainsEditAndDeleteActions() throws IOException {
        String layout = read("app/src/main/res/layout/item_product.xml");

        assertTrue(layout.contains("@+id/button_edit_product"));
        assertTrue(layout.contains("@+id/button_delete_product"));
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

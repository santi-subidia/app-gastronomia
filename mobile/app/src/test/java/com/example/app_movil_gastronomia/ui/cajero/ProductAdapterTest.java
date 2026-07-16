package com.example.app_movil_gastronomia.ui.cajero;

import static org.junit.Assert.assertEquals;

import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import org.junit.Test;

public class ProductAdapterTest {

    @Test
    public void formatsPricesForDifferentProducts() {
        new ProductAdapter(product -> { }, product -> { });

        assertEquals("$1250", ProductAdapter.formatPrice(1250.4));
        assertEquals("$0", ProductAdapter.formatPrice(0));
    }
}

package com.example.app_movil_gastronomia.ui.pedido;

import com.example.app_movil_gastronomia.data.repository.AuthRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.CajaRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.CatalogoRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.ConfiguracionRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.DemoraRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.PedidoRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.ProductoRepositoryImpl;
import com.example.app_movil_gastronomia.data.api.PedidoApi;
import com.example.app_movil_gastronomia.data.api.ProductoApi;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;
import com.example.app_movil_gastronomia.data.repository.contract.CatalogoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.ConfiguracionRepository;
import com.example.app_movil_gastronomia.data.repository.contract.DemoraRepository;
import com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository;
import com.example.app_movil_gastronomia.data.repository.contract.ProductoRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

/** Hilt bindings for deterministic caja restriction UI instrumentation tests. */
@Module
@TestInstallIn(
        components = SingletonComponent.class,
        replaces = com.example.app_movil_gastronomia.di.RepositoryModule.class
)
public abstract class CajaRestrictionTestRepositoryModule {

    private static PedidoRepository pedidoRepository;
    private static ProductoRepository productoRepository;

    static void setRepositories(PedidoRepository pedidos, ProductoRepository productos) {
        pedidoRepository = pedidos;
        productoRepository = productos;
    }

    @Provides
    @Singleton
    static PedidoRepository providePedidoRepository(
            PedidoApi pedidoApi, CatalogoRepository catalogoRepository) {
        return pedidoRepository != null
                ? pedidoRepository
                : new PedidoRepositoryImpl(pedidoApi, catalogoRepository);
    }

    @Provides
    @Singleton
    static ProductoRepository provideProductoRepository(ProductoApi productoApi) {
        return productoRepository != null
                ? productoRepository
                : new ProductoRepositoryImpl(productoApi);
    }

    @Binds
    abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    abstract ConfiguracionRepository bindConfiguracionRepository(ConfiguracionRepositoryImpl impl);

    @Binds
    abstract DemoraRepository bindDemoraRepository(DemoraRepositoryImpl impl);

    @Binds
    abstract CajaRepository bindCajaRepository(CajaRepositoryImpl impl);

    @Binds
    abstract CatalogoRepository bindCatalogoRepository(CatalogoRepositoryImpl impl);
}

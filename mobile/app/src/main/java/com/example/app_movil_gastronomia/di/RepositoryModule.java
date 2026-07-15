package com.example.app_movil_gastronomia.di;

import com.example.app_movil_gastronomia.data.repository.contract.*;
import com.example.app_movil_gastronomia.data.repository.*;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    public abstract ProductoRepository bindProductoRepository(ProductoRepositoryImpl impl);

    @Binds
    public abstract ConfiguracionRepository bindConfiguracionRepository(ConfiguracionRepositoryImpl impl);

    @Binds
    public abstract DemoraRepository bindDemoraRepository(DemoraRepositoryImpl impl);

    @Binds
    public abstract PedidoRepository bindPedidoRepository(PedidoRepositoryImpl impl);

    @Binds
    public abstract CajaRepository bindCajaRepository(CajaRepositoryImpl impl);

    @Binds
    public abstract CatalogoRepository bindCatalogoRepository(CatalogoRepositoryImpl impl);
}

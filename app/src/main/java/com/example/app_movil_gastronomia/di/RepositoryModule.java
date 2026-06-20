package com.example.app_movil_gastronomia.di;

import com.example.app_movil_gastronomia.data.repository.AuthRepository;
import com.example.app_movil_gastronomia.data.repository.AuthRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.ConfiguracionRepository;
import com.example.app_movil_gastronomia.data.repository.ConfiguracionRepositoryImpl;
import com.example.app_movil_gastronomia.data.repository.ProductoRepository;
import com.example.app_movil_gastronomia.data.repository.ProductoRepositoryImpl;

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
}

package com.example.app_movil_gastronomia.nav;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.di.StorageModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

/**
 * Módulo de pruebas Hilt que reemplaza el enlace de producción de
 * {@link StorageModule#provideTokenManager(android.content.Context)}
 * por un {@link FakeTokenManager} compartido para las pruebas Hilt.
 *
 * <p>El proveedor usa el mismo alcance {@code @Singleton} que producción para
 * que la prueba y {@code MainActivity.tokenManager} reciban la misma instancia.
 *
 * <p>El estado inicial es "sin sesión" para representar una instalación nueva.
 */
@Module
@TestInstallIn(
        components = SingletonComponent.class,
        replaces = StorageModule.class
)
public class TestStorageModule {

    @Provides
    @Singleton
    public TokenManager provideFakeTokenManager() {
        return new FakeTokenManager();
    }
}

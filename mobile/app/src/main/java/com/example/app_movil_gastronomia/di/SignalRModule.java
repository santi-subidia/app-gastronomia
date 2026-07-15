package com.example.app_movil_gastronomia.di;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.SignalRServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module that exposes the {@link SignalRService} singleton. Kept
 * in its own module so the SignalR-specific wiring (no Retrofit, no
 * OkHttp — it manages its own WebSocket lifecycle) does not pollute
 * {@link NetworkModule}.
 */
@Module
@InstallIn(SingletonComponent.class)
public class SignalRModule {

    @Provides
    @Singleton
    public SignalRService provideSignalRService() {
        return new SignalRServiceImpl();
    }
}

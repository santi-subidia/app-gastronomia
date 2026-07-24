package com.example.app_movil_gastronomia.di;

import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.SignalRServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class SignalRModule {

    @Provides
    @Singleton
    public SignalRService provideSignalRService() {
        return new SignalRServiceImpl();
    }
}

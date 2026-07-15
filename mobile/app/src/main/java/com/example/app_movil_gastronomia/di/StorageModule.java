package com.example.app_movil_gastronomia.di;

import android.content.Context;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.TokenManagerImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class StorageModule {

    @Provides
    @Singleton
    public TokenManager provideTokenManager(@ApplicationContext Context context) {
        return new TokenManagerImpl(context);
    }
}
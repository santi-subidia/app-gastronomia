package com.example.app_movil_gastronomia;

import android.app.Application;

import org.maplibre.android.MapLibre;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class GastronomiaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MapLibre.getInstance(this);
    }
}

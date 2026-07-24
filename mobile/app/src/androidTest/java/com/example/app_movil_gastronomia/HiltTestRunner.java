package com.example.app_movil_gastronomia;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltTestApplication;

/** Runner que reemplaza la aplicación de producción por la aplicación de pruebas Hilt. */
public class HiltTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, HiltTestApplication.class.getName(), context);
    }
}

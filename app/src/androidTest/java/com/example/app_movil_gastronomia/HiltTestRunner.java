package com.example.app_movil_gastronomia;

import android.app.Application;
import android.content.Context;

import androidx.test.runner.AndroidJUnitRunner;

import dagger.hilt.android.testing.HiltTestApplication;

/**
 * Custom test runner that swaps the production {@link GastronomiaApp} for
 * {@link HiltTestApplication} so Hilt-generated components can be
 * substituted per-test with {@code @BindValue} and {@code @TestInstallIn}.
 */
public class HiltTestRunner extends AndroidJUnitRunner {

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return super.newApplication(cl, HiltTestApplication.class.getName(), context);
    }
}

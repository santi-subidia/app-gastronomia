package com.example.app_movil_gastronomia.nav;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.di.StorageModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

/**
 * Hilt test module that replaces the production
 * {@link StorageModule#provideTokenManager(android.content.Context)}
 * binding with a shared {@link FakeTokenManager} for all
 * {@code @HiltAndroidTest} classes in the {@code nav} package (and any
 * other test class that installs it).
 *
 * <p>The provider IS {@code @Singleton}-scoped (matching the production
 * scope) so the test class and the injected {@code MainActivity.tokenManager}
 * receive the SAME {@code FakeTokenManager} instance. Without this, each
 * {@code @Inject} point would get a fresh fake and the test's
 * {@code setRole(...)} call would never reach the activity — the activity
 * would see the default "no session" state and route to the login screen.
 *
 * <p>Default state is "no session" so tests that do not configure a role
 * behave the same as a fresh app install.
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

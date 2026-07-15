package com.example.app_movil_gastronomia.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.app_movil_gastronomia.MainActivity;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

/**
 * Instrumented test for the role-conditional bottom-nav menu configured
 * by {@code MainActivity.configureBottomNav(String)} on cold start.
 *
 * <p>For each role the test pre-configures a {@link FakeTokenManager} with
 * a non-expired session and a known role, launches
 * {@link MainActivity}, and asserts both the menu item count and the
 * specific destination IDs that {@code configureBottomNav} added.
 *
 * <p>The {@link TestStorageModule} (installed via
 * {@code @TestInstallIn}) replaces the production
 * {@code StorageModule.provideTokenManager} binding with the fake for the
 * duration of each test.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class BottomNavIntegrationTest {

    @org.junit.Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    public TokenManager tokenManager;

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    @Test
    public void cajero_seesFourBottomNavItems() {
        ((FakeTokenManager) tokenManager).setRole("cajero");

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav_view);
            assertNotNull("BottomNavigationView should be present in the default layout",
                    bottomNav);
            assertEquals("Cajero should see exactly 4 bottom-nav tabs",
                    4, bottomNav.getMenu().size());
            assertNotNull("Home tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_cajero_home));
            assertNotNull("Pedidos tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_pedido_list));
            assertNotNull("Productos tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_cajero_productos));
            assertNotNull("Caja tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_caja));
        });

        scenario.close();
    }

    @Test
    public void cocina_seesTwoBottomNavItems() {
        ((FakeTokenManager) tokenManager).setRole("cocina");

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav_view);
            assertNotNull("BottomNavigationView should be present in the default layout",
                    bottomNav);
            assertEquals("Cocina should see exactly 2 bottom-nav tabs",
                    2, bottomNav.getMenu().size());
            assertNotNull("Home tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_cocina_home));
            assertNotNull("Pedidos tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_pedido_list));
        });

        scenario.close();
    }

    @Test
    public void repartidor_seesThreeBottomNavItems() {
        ((FakeTokenManager) tokenManager).setRole("repartidor");

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_nav_view);
            assertNotNull("BottomNavigationView should be present in the default layout",
                    bottomNav);
            assertEquals("Repartidor should see exactly 3 bottom-nav tabs",
                    3, bottomNav.getMenu().size());
            assertNotNull("Home tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_repartidor_home));
            assertNotNull("Pedidos tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_pedido_list));
            assertNotNull("Mapa tab missing",
                    bottomNav.getMenu().findItem(R.id.nav_mapa));
        });

        scenario.close();
    }
}

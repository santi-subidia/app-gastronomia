package com.example.app_movil_gastronomia.nav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.app_movil_gastronomia.MainActivity;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.TokenManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

/**
 * Prueba instrumentada del flujo completo de cierre de sesión.
 *
 * <p>Escenarios verificados:
 * <ol>
 *   <li>El usuario comienza en el inicio de su rol.</li>
 *   <li>Se invoca {@code MainActivity.performLogout()}.</li>
 *   <li>El token queda en null y el destino actual es {@code R.id.nav_login}.</li>
 *   <li>La pila queda vacía y volver desde login cierra la aplicación.</li>
 * </ol>
 *
 * <p>{@link TestStorageModule} reemplaza el proveedor de tokens de producción
 * durante cada prueba.
 *
 * <p><b>Nota:</b> se invoca el mismo punto de entrada mediante reflexión para
 * evitar agregar una dependencia solo para interactuar con el menú.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class LogoutIntegrationTest {

    @org.junit.Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    public TokenManager tokenManager;

    @Before
    public void setUp() {
        hiltRule.inject();
        ((FakeTokenManager) tokenManager).setRole("cajero");
    }

    @Test
    public void performLogout_clearsToken_andNavigatesToLogin_andClearsBackStack() throws Exception {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            NavController controller = currentNavController(activity);
            assertEquals("Pre-logout: should be on cajero home",
                    R.id.nav_cajero_home, controller.getCurrentDestination().getId());
            assertEquals("Pre-logout: token should be present",
                    "fake.jwt.token", tokenManager.getToken());
        });

        scenario.onActivity(activity -> {
            try {
                Method performLogout = MainActivity.class.getDeclaredMethod("performLogout");
                performLogout.setAccessible(true);
                performLogout.invoke(activity);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Failed to invoke performLogout()", e);
            }
        });

        assertNull("Post-logout: token should be null", tokenManager.getToken());

        scenario.onActivity(activity -> {
            NavController controller = currentNavController(activity);
            NavDestination current = controller.getCurrentDestination();
            assertNotNull("Current destination should not be null", current);
            assertEquals("Post-logout: should be on nav_login",
                    R.id.nav_login, current.getId());

            assertNull("Post-logout: there should be no previous back stack entry",
                    controller.getPreviousBackStackEntry());

            assertFalse("Post-logout: popBackStack should be false (login is start destination)",
                    controller.popBackStack());
        });

        scenario.close();
    }

    /**
     * Obtiene el {@link NavController} alojado por el fragmento de navegación.
     */
    private static NavController currentNavController(MainActivity activity) {
        NavHostFragment navHost = (NavHostFragment) activity
                .getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        assertNotNull("NavHostFragment should be present", navHost);
        return navHost.getNavController();
    }
}

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
 * Instrumented test for the full logout flow.
 *
 * <p>Scenarios verified:
 * <ol>
 *   <li>The user starts on the role-home destination (sanity check).</li>
 *   <li>{@code MainActivity.performLogout()} is invoked.</li>
 *   <li>{@code TokenManager.getToken()} returns null after the call
 *       (proves {@code clearToken} was called).</li>
 *   <li>The current {@code NavController} destination is
 *       {@code R.id.nav_login}.</li>
 *   <li>The back stack contains no previous entry — pressing back from
 *       {@code nav_login} exits the app instead of returning to a home
 *       screen.</li>
 * </ol>
 *
 * <p>The {@link TestStorageModule} (installed via
 * {@code @TestInstallIn}) replaces the production
 * {@code StorageModule.provideTokenManager} binding with the fake for the
 * duration of each test.
 *
 * <p><b>Note on triggering logout:</b> {@code Espresso.openActionBarOverflowMenu()}
 * was removed in Espresso 3.6.0 and {@code espresso-contrib} (which provides
 * {@code ToolbarActions}) is not on the classpath. Rather than add a
 * dependency just to drive a menu, the test invokes the same single
 * entry point used by both the toolbar overflow and the drawer —
 * {@code MainActivity.performLogout()} — via reflection. This still
 * exercises the production logout code path (the same method the menu
 * item and the drawer item both delegate to).
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
        // Log the user in as "cajero" so the activity routes to the
        // cajero home on cold start. The role itself is not significant
        // for the logout assertions — any logged-in role exercises the
        // same performLogout() path.
        ((FakeTokenManager) tokenManager).setRole("cajero");
    }

    @Test
    public void performLogout_clearsToken_andNavigatesToLogin_andClearsBackStack() throws Exception {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        // Sanity: the activity landed on the cajero home destination.
        scenario.onActivity(activity -> {
            NavController controller = currentNavController(activity);
            assertEquals("Pre-logout: should be on cajero home",
                    R.id.nav_cajero_home, controller.getCurrentDestination().getId());
            assertEquals("Pre-logout: token should be present",
                    "fake.jwt.token", tokenManager.getToken());
        });

        // Invoke performLogout() — the single entry point used by both
        // the toolbar overflow and the navigation drawer. This is the
        // production code path, reached via reflection only because
        // the method is private.
        scenario.onActivity(activity -> {
            try {
                Method performLogout = MainActivity.class.getDeclaredMethod("performLogout");
                performLogout.setAccessible(true);
                performLogout.invoke(activity);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Failed to invoke performLogout()", e);
            }
        });

        // Post-logout: the token MUST be cleared.
        assertNull("Post-logout: token should be null", tokenManager.getToken());

        // Post-logout: nav destination is nav_login, and the back stack
        // has no previous entry — popBackStack() returns false because
        // nav_login is the graph's start destination (no parent), so
        // pressing back from login exits the app instead of returning
        // to a home screen.
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
     * Resolves the {@link NavController} hosted by the activity's
     * nav-host fragment. Pulled out so each assertion site stays focused
     * on the intent (read the controller) rather than the lookup boilerplate.
     */
    private static NavController currentNavController(MainActivity activity) {
        NavHostFragment navHost = (NavHostFragment) activity
                .getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        assertNotNull("NavHostFragment should be present", navHost);
        return navHost.getNavController();
    }
}

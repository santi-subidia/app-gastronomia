package com.example.app_movil_gastronomia.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.app_movil_gastronomia.MainActivity;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.SessionManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

/**
 * Verifies that when {@link SessionManager#expireSession()} is fired while
 * {@link MainActivity} is in the foreground, the activity navigates to
 * {@code R.id.nav_login} and consumes the event.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @org.junit.Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Test
    public void sessionExpired_navigatesToLogin() {
        hiltRule.inject();

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            SessionManager sessionManager = activity.sessionManager;
            assertNotNull(sessionManager);

            sessionManager.expireSession();

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        });

        scenario.onActivity(activity -> {
            NavHostFragment navHost = (NavHostFragment) activity
                    .getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            assertNotNull(navHost);
            NavController controller = navHost.getNavController();
            assertEquals(R.id.nav_login, controller.getCurrentDestination().getId());
            assertEquals(Boolean.FALSE, activity.sessionManager.getSessionExpired().getValue());
        });

        scenario.close();
    }

    @Test
    public void sessionExpired_isIdempotentWhenAlreadyOnLogin() {
        hiltRule.inject();

        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.moveToState(Lifecycle.State.RESUMED);

        scenario.onActivity(activity -> {
            SessionManager sessionManager = activity.sessionManager;
            sessionManager.expireSession();
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        });

        scenario.onActivity(activity -> {
            NavHostFragment navHost = (NavHostFragment) activity
                    .getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
            NavController controller = navHost.getNavController();
            assertEquals(R.id.nav_login, controller.getCurrentDestination().getId());
            assertEquals(Boolean.FALSE, activity.sessionManager.getSessionExpired().getValue());
        });

        scenario.close();
    }
}

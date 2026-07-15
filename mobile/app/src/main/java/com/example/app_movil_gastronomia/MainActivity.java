package com.example.app_movil_gastronomia;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.app_movil_gastronomia.core.SessionManager;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private ActivityMainBinding binding;

    @VisibleForTesting
    @Inject
    public SessionManager sessionManager;

    @VisibleForTesting
    @Inject
    public TokenManager tokenManager;

    /**
     * True while {@link #onCreate(Bundle)} is performing the splash-gated
     * auto-login. Held as a field so a future phase can probe it (e.g.
     * instrumentation tests) without re-running the check.
     */
    private boolean isCheckingSession = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();

        // Top-level destinations are the three role-home screens; the
        // drawerLayout is wired as the openable container so the
        // hamburger/up-arrow switch works correctly.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_cajero_home, R.id.nav_cocina_home, R.id.nav_repartidor_home)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Drawer toggle: connects the hamburger icon to the drawer open/close
        // gesture. Required for the icon to appear on the toolbar.
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.appBarMain.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Drawer item selection: handle logout + configuracion, then close
        // the drawer. We keep the listener attached for the whole activity
        // lifetime because the drawer's contents do not change at runtime.
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cerrar_sesion) {
                performLogout();
            } else if (id == R.id.nav_configuracion) {
                navController.navigate(R.id.nav_configuracion);
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Observe session-expiration: when fired, navigate to the login screen
        // and re-arm the flag. Preserved from the previous implementation —
        // OkHttp's AuthInterceptor posts here on 401, and the host Activity
        // is the only place that can safely navigate.
        sessionManager.getSessionExpired().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean expired) {
                if (Boolean.TRUE.equals(expired) && navController != null) {
                    NavDestination current = navController.getCurrentDestination();
                    // Guard: don't re-navigate if we are already on login.
                    if (current != null && current.getId() == R.id.nav_login) {
                        sessionManager.consume();
                        return;
                    }
                    navController.navigate(R.id.nav_login);
                    sessionManager.consume();
                }
            }
        });

        // Splash-gated auto-login. The splash stays on top of every other
        // view while we validate the stored session so the login form
        // never flickers on cold start.
        runAutoLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(controller, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Splash-gated session validation executed on cold start. Decides whether
     * to keep the user in their role-home screen or hand them off to the
     * login destination, then dismisses the splash overlay.
     */
    private void runAutoLogin() {
        showSplash();

        if (!tokenManager.hasToken()) {
            // No token: login is the nav graph's startDestination, so we
            // just need to make sure the splash is dismissed and bail.
            isCheckingSession = false;
            hideSplash();
            return;
        }

        long expSeconds = tokenManager.decodeTokenExp();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (expSeconds < 0 || expSeconds <= nowSeconds) {
            // Malformed (-1) or expired. Treat as invalid: clear and let
            // the startDestination (login) handle the rest.
            tokenManager.clearToken();
            isCheckingSession = false;
            hideSplash();
            return;
        }

        String role = tokenManager.getRole();
        Integer homeDestination = resolveHomeDestination(role);
        if (homeDestination == null) {
            // Unknown role: log a warning and bail to the start destination
            // (login) so the user can re-authenticate.
            Log.w(TAG, "Unknown role '" + role + "' — falling back to login");
            isCheckingSession = false;
            hideSplash();
            return;
        }

        // Valid session: jump to the role-home, then wire the role-specific
        // bottom-nav tabs and the drawer header. popUpTo(mobile_navigation,
        // inclusive=true) clears the back stack so back-from-home exits the
        // app instead of re-entering login.
        NavOptions popUpToGraph = new NavOptions.Builder()
                .setPopUpTo(R.id.mobile_navigation, /* inclusive= */ true)
                .build();
        navController.navigate(homeDestination, null, popUpToGraph);
        configureBottomNav(role);
        bindDrawerHeader();

        isCheckingSession = false;
        hideSplash();
    }

    /**
     * Replaces the bottom navigation menu with the items appropriate for the
     * given role. Items are added programmatically so the IDs always match
     * the nav-graph destinations and {@link NavigationUI} can wire the
     * tab-selection -> navigate behavior without an extra XML file per role.
     */
    private void configureBottomNav(@Nullable String role) {
        BottomNavigationView bottomNav = binding.appBarMain.contentMain.bottomNavView;
        if (bottomNav == null) {
            // The view is only present in the default layout (not w600dp /
            // w1240dp). Tablets skip the bottom-nav entirely.
            return;
        }
        bottomNav.getMenu().clear();
        if (role == null) {
            return;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "cajero":
                bottomNav.getMenu()
                        .add(0, R.id.nav_cajero_home, 0, R.string.cajero_title)
                        .setIcon(R.drawable.ic_home_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_pedido_list, 1, R.string.all_orders)
                        .setIcon(R.drawable.ic_pedidos_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_cajero_productos, 2, R.string.no_products_available)
                        .setIcon(R.drawable.ic_productos_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_caja, 3, R.string.caja_title)
                        .setIcon(R.drawable.ic_caja_24dp);
                break;
            case "cocina":
                bottomNav.getMenu()
                        .add(0, R.id.nav_cocina_home, 0, R.string.cocina_title)
                        .setIcon(R.drawable.ic_home_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_pedido_list, 1, R.string.all_orders)
                        .setIcon(R.drawable.ic_pedidos_24dp);
                break;
            case "repartidor":
                bottomNav.getMenu()
                        .add(0, R.id.nav_repartidor_home, 0, R.string.repartidor_title)
                        .setIcon(R.drawable.ic_home_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_pedido_list, 1, R.string.all_orders)
                        .setIcon(R.drawable.ic_pedidos_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_mapa, 2, R.string.mapa_title)
                        .setIcon(R.drawable.ic_mapa_24dp);
                break;
            default:
                return;
        }
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    /**
     * Binds the drawer's header {@code header_name} and {@code header_role}
     * text views from the persisted session. If the session has no name or
     * role, the localized fallback string is shown so the header is never
     * blank.
     */
    private void bindDrawerHeader() {
        NavigationView navView = binding.navView;
        if (navView.getHeaderCount() == 0) {
            return;
        }
        View header = navView.getHeaderView(0);
        if (header == null) {
            return;
        }
        TextView nameView = header.findViewById(R.id.header_name);
        TextView roleView = header.findViewById(R.id.header_role);

        String name = tokenManager.getNombreUsuario();
        if (nameView != null) {
            nameView.setText(name != null && !name.isEmpty()
                    ? name
                    : getString(R.string.header_fallback));
        }
        if (roleView != null) {
            String role = tokenManager.getRole();
            roleView.setText(role != null && !role.isEmpty()
                    ? role
                    : getString(R.string.header_fallback));
        }
    }

    /**
     * Single logout entry point used by both the toolbar overflow and the
     * drawer. Clears the persisted session, resets the SessionManager flag
     * (so a stale {@code expireSession()} doesn't immediately re-trigger),
     * and navigates to {@code nav_login} with the back stack cleared.
     */
    private void performLogout() {
        tokenManager.clearToken();
        sessionManager.consume();
        if (navController != null) {
            NavOptions popUpToGraph = new NavOptions.Builder()
                    .setPopUpTo(R.id.mobile_navigation, /* inclusive= */ true)
                    .build();
            navController.navigate(R.id.nav_login, null, popUpToGraph);
        }
        if (binding != null && binding.drawerLayout != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * Makes the splash overlay visible. The view is config-dependent
     * ({@code layout-w600dp} does not include it), so we tolerate null.
     */
    private void showSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides the splash overlay. Safe to call when the view is absent.
     */
    private void hideSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Maps a persisted role name to its home destination ID, or {@code null}
     * when the role is unrecognized so the caller can fall back to login.
     *
     * <p>Comparison is case-insensitive and trim-tolerant so the app keeps
     * working when the backend returns a different casing ("cajero" vs
     * "Cajero") or a value padded with whitespace.
     *
     * <p>Extracted from {@link #onCreate(Bundle)} as a package-private
     * static method so it can be unit-tested without instantiating the
     * Activity or running Robolectric.
     */
    @Nullable
    @VisibleForTesting
    static Integer resolveHomeDestination(@Nullable String role) {
        if (role == null) {
            return null;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "cajero":
                return R.id.nav_cajero_home;
            case "cocina":
                return R.id.nav_cocina_home;
            case "repartidor":
                return R.id.nav_repartidor_home;
            default:
                return null;
        }
    }
}

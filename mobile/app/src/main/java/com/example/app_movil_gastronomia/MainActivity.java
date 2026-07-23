package com.example.app_movil_gastronomia;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
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
import com.example.app_movil_gastronomia.data.dto.signalr.DemoraRegistradaMessage;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;
import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

    @Inject
    public AuthRepository authRepository;

    @Inject
    public com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository usuarioRepository;

    @Nullable
    @Inject
    public SignalRService signalRService;

    /**
     * True while {@link #onCreate(Bundle)} is performing the splash-gated
     * auto-login. Held as a field so a future phase can probe it (e.g.
     * instrumentation tests) without re-running the check.
     */
    private boolean isCheckingSession = true;

    private static final String DELAY_NOTIFICATION_CHANNEL_ID = "demoras_channel";
    private static final int DELAY_NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private final Set<Integer> notifiedDemoraIds = new HashSet<>();
    private Observer<DemoraRegistradaMessage> demoraRegistradaObserver;

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

        // Drawer item selection: handle logout + configuracion, then close
        // the drawer. We keep the listener attached for the whole activity
        // lifetime because the drawer's contents do not change at runtime.
        binding.navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cerrar_sesion) {
                performLogout();
            } else if (id == R.id.nav_reportar_contingencia) {
                showReportarContingenciaDialog();
            } else if (id == R.id.nav_configuracion) {
                navController.navigate(R.id.nav_configuracion);
            } else if (id == R.id.nav_repartidores_mapa) {
                navController.navigate(R.id.nav_repartidores_mapa);
            } else if (id == R.id.nav_switch_disponible) {
                // If they tap the menu item background instead of the switch directly, toggle the switch
                if (item.getActionView() != null) {
                    com.google.android.material.switchmaterial.SwitchMaterial switchView = 
                            item.getActionView().findViewById(R.id.drawer_switch_disponible);
                    if (switchView != null && switchView.isEnabled()) {
                        switchView.setChecked(!switchView.isChecked());
                        // Fire the manual toggle logic explicitly
                        int userId = tokenManager.getUserId();
                        if (userId > 0) {
                            usuarioRepository.updateDisponibilidad(userId, switchView.isChecked());
                        }
                    }
                }
                return true; // Don't close the drawer
            }
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
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
                    NavOptions popUpToGraph = new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_login, /* inclusive= */ true)
                            .build();
                    navController.navigate(R.id.nav_login, null, popUpToGraph);
                    sessionManager.consume();
                }
            }
        });

        usuarioRepository.getContingenciaState().observe(this, state -> {
            if (state != null) {
                switch (state.getStatus()) {
                    case LOADING:
                        // Podríamos mostrar un progress dialog
                        break;
                    case SUCCESS:
                        android.widget.Toast.makeText(this, "Contingencia reportada. Estás Fuera de Servicio.", android.widget.Toast.LENGTH_LONG).show();
                        // Refrescar el estado del switch
                        int userId = tokenManager.getUserId();
                        if (userId > 0) {
                            usuarioRepository.fetchUsuario(userId);
                        }
                        break;
                    case ERROR:
                        android.widget.Toast.makeText(this, "Error al reportar: " + state.getError(), android.widget.Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean isLogin = destination.getId() == R.id.nav_login;
            boolean isCocina = tokenManager != null && "Cocina".equalsIgnoreCase(tokenManager.getRole());

            if (binding.appBarMain.toolbar != null) {
                binding.appBarMain.toolbar.setVisibility(isLogin ? View.GONE : View.VISIBLE);
            }
            if (binding.appBarMain.contentMain.bottomNavView != null) {
                binding.appBarMain.contentMain.bottomNavView.setVisibility((isLogin || isCocina) ? View.GONE : View.VISIBLE);
            }
            if (binding.drawerLayout != null) {
                binding.drawerLayout.setDrawerLockMode(
                        isLogin ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED
                );
            }

            // Hide keyboard when navigating to avoid it sticking around on screens without inputs
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        });

        // Splash-gated auto-login. The splash stays on top of every other
        // view while we validate the stored session so the login form
        // never flickers on cold start.
        runAutoLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Navigates to the pedido detail if the activity was launched from a
     * delay notification. Safe to call repeatedly; no-op if the intent has no
     * pedido id.
     */
    private void handleNotificationIntent(@Nullable Intent intent) {
        if (intent == null || navController == null) return;
        int pedidoId = intent.getIntExtra("pedidoId", -1);
        if (pedidoId <= 0) return;
        intent.removeExtra("pedidoId");
        navigateToPedidoDetail(pedidoId);
    }

    private void navigateToPedidoDetail(int pedidoId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == R.id.nav_pedido_detail) {
            // Already there; nothing to do.
            return;
        }
        NavOptions popUp = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_pedido_detail, true)
                .build();
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedidoId);
        navController.navigate(R.id.nav_pedido_detail, args, popUp);
    }

    /**
     * Wires the cashier delay notification observer when the role is Cajero.
     * Called after a successful login or auto-login.
     */
    private void bindDelayNotifications() {
        if (signalRService == null) return;
        if (demoraRegistradaObserver == null) {
            demoraRegistradaObserver = msg -> {
                if (msg == null || !isCajeroRole()) return;
                if (notifiedDemoraIds.contains(msg.getDemoraId())) return;
                notifiedDemoraIds.add(msg.getDemoraId());
                showDelayNotification(msg);
            };
        }
        signalRService.getDemoraRegistrada().observe(this, demoraRegistradaObserver);
    }

    private boolean isCajeroRole() {
        String role = tokenManager != null ? tokenManager.getRole() : null;
        return "Cajero".equalsIgnoreCase(role);
    }

    private void showDelayNotification(DemoraRegistradaMessage msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        DELAY_NOTIFICATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        createDelayNotificationChannel();

        String content = getString(R.string.delay_notification_content,
                msg.getDemoraMinutos(), msg.getSector(), msg.getPedidoId());
        String bigText = content;
        if (msg.getObservaciones() != null && !msg.getObservaciones().isEmpty()) {
            bigText += "\n" + getString(R.string.delay_notification_observations, msg.getObservaciones());
        }

        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.putExtra("pedidoId", msg.getPedidoId());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                msg.getDemoraId(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DELAY_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning_24dp)
                .setContentTitle(getString(R.string.delay_notification_title))
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify(msg.getDemoraId(), builder.build());
    }

    private void createDelayNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        CharSequence name = getString(R.string.delay_notification_channel_name);
        String description = getString(R.string.delay_notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(DELAY_NOTIFICATION_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DELAY_NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(binding.getRoot(), R.string.delay_notification_permission_granted,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(binding.getRoot(), R.string.delay_notification_permission_denied,
                        Snackbar.LENGTH_LONG).show();
            }
        }
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
        if (authRepository != null) {
            authRepository.resetLoginState();
        }
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
        // bottom-nav tabs and the drawer header. popUpTo(nav_login, inclusive=true) clears the back stack so back-from-home exits the
        // app instead of re-entering login.
        NavOptions popUpToGraph = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_login, /* inclusive= */ true)
                .build();
        navController.navigate(homeDestination, null, popUpToGraph);
        configureBottomNav(role);
        configureDrawerMenu(role);
        bindDrawerHeader();

        if (signalRService != null) {
            signalRService.connect(tokenManager.getToken());
        }
        if ("Repartidor".equalsIgnoreCase(role)) {
            startLocationService();
        }
        if ("Cajero".equalsIgnoreCase(role)) {
            bindDelayNotifications();
        }

        isCheckingSession = false;
        hideSplash();
    }

    /**
     * Called by LoginFragment after a successful login to update the UI
     * elements that depend on the authenticated user's role.
     */
    public void onLoginSuccess() {
        if (tokenManager.hasToken()) {
            String role = tokenManager.getRole();
            configureBottomNav(role);
            configureDrawerMenu(role);
            bindDrawerHeader();
            if (signalRService != null) {
                signalRService.connect(tokenManager.getToken());
            }
            if ("Repartidor".equalsIgnoreCase(role)) {
                startLocationService();
            }
            if ("Cajero".equalsIgnoreCase(role)) {
                bindDelayNotifications();
            }
        }
    }

    private void startLocationService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, com.example.app_movil_gastronomia.core.LocationForegroundService.class);
        androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopLocationService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, com.example.app_movil_gastronomia.core.LocationForegroundService.class);
        stopService(serviceIntent);
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
                        .add(0, R.id.nav_cajero_productos, 2, R.string.go_to_products)
                        .setIcon(R.drawable.ic_productos_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_caja, 3, R.string.caja_title)
                        .setIcon(R.drawable.ic_caja_24dp);
                break;
            case "cocina":
                // Cocina solo necesita una pantalla, la BottomNav se oculta entera
                break;
            case "repartidor":
                bottomNav.getMenu()
                        .add(0, R.id.nav_repartidor_home, 0, R.string.repartidor_title)
                        .setIcon(R.drawable.ic_home_24dp);
                bottomNav.getMenu()
                        .add(0, R.id.nav_mapa, 1, R.string.mapa_title)
                        .setIcon(R.drawable.ic_mapa_24dp);
                break;
            default:
                return;
        }
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    private void showReportarContingenciaDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Ej. Se pinchó la rueda, accidente, etc.");
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Reportar Problema")
                .setMessage("¿Por qué no podés continuar? Se te marcará como Fuera de Servicio y tus pedidos pasarán a Contingencia.")
                .setView(input)
                .setPositiveButton("Reportar", (dialog, which) -> {
                    String motivo = input.getText().toString().trim();
                    if (motivo.isEmpty()) {
                        android.widget.Toast.makeText(this, "Debe ingresar un motivo", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int userId = tokenManager.getUserId();
                    if (userId > 0) {
                        usuarioRepository.reportarContingencia(userId, motivo);
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Shows or hides items in the side navigation drawer based on the user's role.
     * For example, the 'cocina' role should not see the configuration menu.
     */
    private void configureDrawerMenu(@Nullable String role) {
        if (binding.navView == null || role == null) return;
        
        Menu drawerMenu = binding.navView.getMenu();
        MenuItem configItem = drawerMenu.findItem(R.id.nav_configuracion);
        MenuItem driversMapItem = drawerMenu.findItem(R.id.nav_repartidores_mapa);
        MenuItem switchItem = drawerMenu.findItem(R.id.nav_switch_disponible);
        MenuItem contingenciaItem = drawerMenu.findItem(R.id.nav_reportar_contingencia);
        
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        
        if (configItem != null) {
            // Only 'cajero' should see the configuration menu
            configItem.setVisible("cajero".equals(normalized));
        }
        if (driversMapItem != null) {
            driversMapItem.setVisible("cajero".equals(normalized));
        }
        if (contingenciaItem != null) {
            contingenciaItem.setVisible("repartidor".equals(normalized));
        }
        if (switchItem != null) {
            boolean isRepartidor = "repartidor".equals(normalized);
            switchItem.setVisible(isRepartidor);
            if (isRepartidor && switchItem.getActionView() != null) {
                com.google.android.material.switchmaterial.SwitchMaterial switchView = 
                        switchItem.getActionView().findViewById(R.id.drawer_switch_disponible);
                if (switchView != null) {
                    // Temporarily disable while fetching state
                    switchView.setEnabled(false);
                    usuarioRepository.getUsuarioState().observe(this, state -> {
                        if (state != null && state.getStatus() == com.example.app_movil_gastronomia.core.UiState.Status.SUCCESS && state.getData() != null) {
                            switchView.setChecked(state.getData().isDisponible());
                            switchView.setEnabled(true);
                        }
                    });
                    usuarioRepository.getUpdateState().observe(this, state -> {
                        if (state != null) {
                            switch (state.getStatus()) {
                                case LOADING:
                                    switchView.setEnabled(false);
                                    break;
                                case SUCCESS:
                                    switchView.setEnabled(true);
                                    android.widget.Toast.makeText(this, "Estado actualizado", android.widget.Toast.LENGTH_SHORT).show();
                                    break;
                                case ERROR:
                                    switchView.setEnabled(true);
                                    switchView.setChecked(!switchView.isChecked());
                                    android.widget.Toast.makeText(this, "Error al actualizar estado: " + state.getError(), android.widget.Toast.LENGTH_LONG).show();
                                    break;
                            }
                        }
                    });
                    
                    switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (buttonView.isPressed()) {
                            int userId = tokenManager.getUserId();
                            if (userId > 0) {
                                usuarioRepository.updateDisponibilidad(userId, isChecked);
                            }
                        }
                    });
                    
                    // Fetch initial state
                    int userId = tokenManager.getUserId();
                    if (userId > 0) {
                        usuarioRepository.fetchUsuario(userId);
                    }
                }
            }
        }
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
        stopLocationService();
        if (signalRService != null) {
            signalRService.disconnect();
        }
        tokenManager.clearToken();
        if (authRepository != null) {
            authRepository.resetLoginState();
        }
        sessionManager.consume();
        if (navController != null) {
            NavOptions popUpToGraph = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_login, /* inclusive= */ true)
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








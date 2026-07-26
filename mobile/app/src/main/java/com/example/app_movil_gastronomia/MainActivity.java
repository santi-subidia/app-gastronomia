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

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_cajero_home, R.id.nav_cocina_home, R.id.nav_repartidor_home)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

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
                if (item.getActionView() != null) {
                    com.google.android.material.switchmaterial.SwitchMaterial switchView = 
                            item.getActionView().findViewById(R.id.drawer_switch_disponible);
                    if (switchView != null && switchView.isEnabled()) {
                        switchView.setChecked(!switchView.isChecked());
                        int userId = tokenManager.getUserId();
                        if (userId > 0) {
                            usuarioRepository.updateDisponibilidad(userId, switchView.isChecked());
                        }
                    }
                }
                return true;
            }
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            return true;
        });

        sessionManager.getSessionExpired().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean expired) {
                if (Boolean.TRUE.equals(expired) && navController != null) {
                    NavDestination current = navController.getCurrentDestination();
                    if (current != null && current.getId() == R.id.nav_login) {
                        sessionManager.consume();
                        return;
                    }
                    NavOptions popUpToGraph = new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_login, true)
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
                        break;
                    case SUCCESS:
                        android.widget.Toast.makeText(this, "Contingencia reportada. Estás Fuera de Servicio.", android.widget.Toast.LENGTH_LONG).show();
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

            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
        });

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
     * Abre el detalle del pedido cuando la actividad se inició desde una
     * notificación de demora. Ignora intents sin identificador de pedido.
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
            return;
        }
        NavOptions popUp = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_pedido_detail, true)
                .build();
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedidoId);
        navController.navigate(R.id.nav_pedido_detail, args, popUp);
    }

    /** Registra el observador de demoras para el rol Cajero. */
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

    /** Valida la sesión al iniciar y muestra la pantalla correspondiente al rol. */
    private void runAutoLogin() {
        showSplash();

        if (!tokenManager.hasToken()) {
            hideSplash();
            return;
        }

        long expSeconds = tokenManager.decodeTokenExp();
        long nowSeconds = System.currentTimeMillis() / 1000L;
        if (expSeconds < 0 || expSeconds <= nowSeconds) {
            tokenManager.clearToken();
            if (authRepository != null) {
                authRepository.resetLoginState();
            }
            hideSplash();
            return;
        }

        String role = tokenManager.getRole();
        Integer homeDestination = resolveHomeDestination(role);
        if (homeDestination == null) {
            Log.w(TAG, "Unknown role '" + role + "' — falling back to login");
            hideSplash();
            return;
        }

        NavOptions popUpToGraph = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_login, true)
                .build();
        navController.navigate(homeDestination, null, popUpToGraph);
        configureAuthenticatedSession(role);
        hideSplash();
    }

    /** Actualiza la interfaz después de un login exitoso. */
    public void onLoginSuccess() {
        if (tokenManager.hasToken()) {
            String role = tokenManager.getRole();
            configureAuthenticatedSession(role);
        }
    }

    private void configureAuthenticatedSession(@Nullable String role) {
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

    private void startLocationService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, com.example.app_movil_gastronomia.core.LocationForegroundService.class);
        androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopLocationService() {
        android.content.Intent serviceIntent = new android.content.Intent(this, com.example.app_movil_gastronomia.core.LocationForegroundService.class);
        stopService(serviceIntent);
    }

    /** Configura la navegación inferior según el rol autenticado. */
    private void configureBottomNav(@Nullable String role) {
        BottomNavigationView bottomNav = binding.appBarMain.contentMain.bottomNavView;
        if (bottomNav == null) {
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
                break;
            case "repartidor":
                bottomNav.getMenu()
                        .add(0, R.id.nav_repartidor_home, 0, R.string.repartidor_title)
                        .setIcon(R.drawable.ic_home_24dp);
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

    /** Muestra en el menú lateral únicamente las opciones permitidas por el rol. */
    private void configureDrawerMenu(@Nullable String role) {
        if (binding.navView == null || role == null) return;
        
        Menu drawerMenu = binding.navView.getMenu();
        MenuItem configItem = drawerMenu.findItem(R.id.nav_configuracion);
        MenuItem driversMapItem = drawerMenu.findItem(R.id.nav_repartidores_mapa);
        MenuItem switchItem = drawerMenu.findItem(R.id.nav_switch_disponible);
        MenuItem contingenciaItem = drawerMenu.findItem(R.id.nav_reportar_contingencia);
        
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        
        if (configItem != null) {
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
                    
                    int userId = tokenManager.getUserId();
                    if (userId > 0) {
                        usuarioRepository.fetchUsuario(userId);
                    }
                }
            }
        }
    }

    /** Completa el encabezado del menú lateral con los datos de la sesión. */
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

    /** Cierra la sesión, limpia la navegación y vuelve al login. */
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
                    .setPopUpTo(R.id.nav_login, true)
                    .build();
            navController.navigate(R.id.nav_login, null, popUpToGraph);
        }
        if (binding != null && binding.drawerLayout != null) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /** Muestra la capa de carga cuando está disponible en el layout. */
    private void showSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.VISIBLE);
        }
    }

    /** Oculta la capa de carga cuando está disponible en el layout. */
    private void hideSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.GONE);
        }
    }

    /** Resuelve el destino inicial a partir del rol persistido. */
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

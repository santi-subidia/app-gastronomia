package com.example.app_movil_gastronomia;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.example.app_movil_gastronomia.core.SignalRService;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

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
    public UsuarioRepository usuarioRepository;

    @Nullable
    @Inject
    public SignalRService signalRService;

    private RoleNavigationManager roleNavigationManager;
    private DelayNotificationManager delayNotificationManager;

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

        roleNavigationManager = new RoleNavigationManager(
                this,
                binding,
                navController,
                tokenManager,
                usuarioRepository,
                this,
                new RoleNavigationManager.Actions() {
                    @Override
                    public void logout() {
                        performLogout();
                    }

                    @Override
                    public void showContingencyDialog() {
                        showReportarContingenciaDialog();
                    }

                    @Override
                    public void navigate(int destinationId) {
                        navController.navigate(destinationId);
                    }
                });
        roleNavigationManager.bindDrawerActions();
        if (signalRService != null) {
            delayNotificationManager = new DelayNotificationManager(
                    this, this, signalRService, tokenManager);
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DelayNotificationManager.PERMISSION_REQUEST_CODE) {
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

    public void onLoginSuccess() {
        if (tokenManager.hasToken()) {
            String role = tokenManager.getRole();
            configureAuthenticatedSession(role);
        }
    }

    private void configureAuthenticatedSession(@Nullable String role) {
        roleNavigationManager.configure(role);

        if (signalRService != null) {
            signalRService.connect(tokenManager.getToken());
        }
        if ("Repartidor".equalsIgnoreCase(role)) {
            startLocationService();
        }
        if ("Cajero".equalsIgnoreCase(role) && delayNotificationManager != null) {
            delayNotificationManager.bind();
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
    private void showSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.VISIBLE);
        }
    }

    private void hideSplash() {
        if (binding.splashLayout != null) {
            binding.splashLayout.setVisibility(View.GONE);
        }
    }

    @Nullable
    @VisibleForTesting
    static Integer resolveHomeDestination(@Nullable String role) {
        return RoleNavigationManager.resolveHomeDestination(role);
    }
}

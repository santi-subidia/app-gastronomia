package com.example.app_movil_gastronomia;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.repository.contract.UsuarioRepository;
import com.example.app_movil_gastronomia.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

public final class RoleNavigationManager {
    public interface Actions {
        void logout();
        void showContingencyDialog();
        void navigate(int destinationId);
    }

    private final MainActivity activity;
    private final ActivityMainBinding binding;
    private final NavController navController;
    private final TokenManager tokenManager;
    private final UsuarioRepository usuarioRepository;
    private final LifecycleOwner lifecycleOwner;
    private final Actions actions;

    public RoleNavigationManager(
            MainActivity activity,
            ActivityMainBinding binding,
            NavController navController,
            TokenManager tokenManager,
            UsuarioRepository usuarioRepository,
            LifecycleOwner lifecycleOwner,
            Actions actions) {
        this.activity = activity;
        this.binding = binding;
        this.navController = navController;
        this.tokenManager = tokenManager;
        this.usuarioRepository = usuarioRepository;
        this.lifecycleOwner = lifecycleOwner;
        this.actions = actions;
    }

    public void bindDrawerActions() {
        binding.navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);
        usuarioRepository.getContingenciaState().observe(lifecycleOwner, state -> {
            if (state == null) return;
            if (state.getStatus() == UiState.Status.SUCCESS) {
                Toast.makeText(activity,
                        "Contingencia reportada. Estás Fuera de Servicio.",
                        Toast.LENGTH_LONG).show();
                fetchCurrentUser();
            } else if (state.getStatus() == UiState.Status.ERROR) {
                Toast.makeText(activity,
                        "Error al reportar: " + state.getError(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    public void configure(@Nullable String role) {
        configureBottomNavigation(role);
        configureDrawerMenu(role);
        bindDrawerHeader();
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_cerrar_sesion) {
            actions.logout();
        } else if (id == R.id.nav_reportar_contingencia) {
            actions.showContingencyDialog();
        } else if (id == R.id.nav_configuracion || id == R.id.nav_repartidores_mapa) {
            actions.navigate(id);
        } else if (id == R.id.nav_switch_disponible) {
            toggleAvailability(item);
            return true;
        }
        binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
        return true;
    }

    private void toggleAvailability(MenuItem item) {
        if (item.getActionView() == null) return;
        SwitchMaterial switchView = item.getActionView().findViewById(R.id.drawer_switch_disponible);
        if (switchView == null || !switchView.isEnabled()) return;

        switchView.setChecked(!switchView.isChecked());
        int userId = tokenManager.getUserId();
        if (userId > 0) {
            usuarioRepository.updateDisponibilidad(userId, switchView.isChecked());
        }
    }

    private void configureBottomNavigation(@Nullable String role) {
        BottomNavigationView bottomNav = binding.appBarMain.contentMain.bottomNavView;
        if (bottomNav == null) return;

        bottomNav.getMenu().clear();
        if (role == null) return;

        String normalized = normalizeRole(role);
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
            case "repartidor":
                bottomNav.getMenu()
                        .add(0, R.id.nav_repartidor_home, 0, R.string.repartidor_title)
                        .setIcon(R.drawable.ic_home_24dp);
                break;
            case "cocina":
                break;
            default:
                return;
        }
        NavigationUI.setupWithNavController(bottomNav, navController);
    }

    private void configureDrawerMenu(@Nullable String role) {
        if (binding.navView == null || role == null) return;

        Menu drawerMenu = binding.navView.getMenu();
        MenuItem configItem = drawerMenu.findItem(R.id.nav_configuracion);
        MenuItem driversMapItem = drawerMenu.findItem(R.id.nav_repartidores_mapa);
        MenuItem switchItem = drawerMenu.findItem(R.id.nav_switch_disponible);
        MenuItem contingencyItem = drawerMenu.findItem(R.id.nav_reportar_contingencia);
        String normalized = normalizeRole(role);

        if (configItem != null) configItem.setVisible("cajero".equals(normalized));
        if (driversMapItem != null) driversMapItem.setVisible("cajero".equals(normalized));
        if (contingencyItem != null) contingencyItem.setVisible("repartidor".equals(normalized));

        boolean isRepartidor = "repartidor".equals(normalized);
        if (switchItem != null) {
            switchItem.setVisible(isRepartidor);
            if (isRepartidor) bindAvailabilitySwitch(switchItem);
        }
    }

    private void bindAvailabilitySwitch(MenuItem switchItem) {
        if (switchItem.getActionView() == null) return;
        SwitchMaterial switchView = switchItem.getActionView().findViewById(R.id.drawer_switch_disponible);
        if (switchView == null) return;

        switchView.setEnabled(false);
        usuarioRepository.getUsuarioState().observe(lifecycleOwner, state -> {
            if (state != null && state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                switchView.setChecked(state.getData().isDisponible());
                switchView.setEnabled(true);
            }
        });
        usuarioRepository.getUpdateState().observe(lifecycleOwner, state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    switchView.setEnabled(false);
                    break;
                case SUCCESS:
                    switchView.setEnabled(true);
                    Toast.makeText(activity, "Estado actualizado", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    switchView.setEnabled(true);
                    switchView.setChecked(!switchView.isChecked());
                    Toast.makeText(activity,
                            "Error al actualizar estado: " + state.getError(),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        });
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            int userId = tokenManager.getUserId();
            if (userId > 0) {
                usuarioRepository.updateDisponibilidad(userId, isChecked);
            }
        });
        fetchCurrentUser();
    }

    private void fetchCurrentUser() {
        int userId = tokenManager.getUserId();
        if (userId > 0) usuarioRepository.fetchUsuario(userId);
    }

    private void bindDrawerHeader() {
        NavigationView navView = binding.navView;
        if (navView.getHeaderCount() == 0) return;

        View header = navView.getHeaderView(0);
        if (header == null) return;

        TextView nameView = header.findViewById(R.id.header_name);
        TextView roleView = header.findViewById(R.id.header_role);
        String name = tokenManager.getNombreUsuario();
        String role = tokenManager.getRole();

        if (nameView != null) {
            nameView.setText(name != null && !name.isEmpty()
                    ? name : activity.getString(R.string.header_fallback));
        }
        if (roleView != null) {
            roleView.setText(role != null && !role.isEmpty()
                    ? role : activity.getString(R.string.header_fallback));
        }
    }

    private static String normalizeRole(String role) {
        return role.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Integer resolveHomeDestination(@Nullable String role) {
        if (role == null) return null;
        switch (normalizeRole(role)) {
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

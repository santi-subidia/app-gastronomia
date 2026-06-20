package com.example.app_movil_gastronomia;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.app_movil_gastronomia.core.SessionManager;
import com.example.app_movil_gastronomia.databinding.ActivityMainBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;

    @VisibleForTesting
    @Inject
    public SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        navController = navHostFragment.getNavController();

        mAppBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Observe session-expiration: when fired, navigate to the login screen
        // and re-arm the flag.
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
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController controller = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(controller, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}

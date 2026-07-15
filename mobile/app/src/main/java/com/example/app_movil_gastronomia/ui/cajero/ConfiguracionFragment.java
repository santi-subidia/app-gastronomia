package com.example.app_movil_gastronomia.ui.cajero;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.databinding.FragmentConfiguracionBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Singleton business-configuration form. Loads the current
 * configuration on creation and either:
 *
 * <ul>
 *   <li>prefills the fields and shows "Actualizar Configuración"
 *       (update mode) when the server already has a record, or</li>
 *   <li>shows "Guardar Configuración" (create mode) when the server
 *       has none yet — the repository's 404 is normalized to
 *       {@code success(null)} by {@link ConfiguracionViewModel}, so
 *       this fragment never has to handle a "not found" error
 *       itself.</li>
 * </ul>
 *
 * <p>The fragment is intentionally thin: all CRUD dispatch lives in
 * the VM. The fragment's job is to (1) render the form, (2) react to
 * the two VM state streams, and (3) collect the form back into a
 * {@link ConfiguracionDto} on submit. The {@code id} field is never
 * exposed to the user — the VM copies it from the cached config when
 * the request is an update.</p>
 */
@AndroidEntryPoint
public class ConfiguracionFragment extends Fragment {

    private FragmentConfiguracionBinding binding;
    private ConfiguracionViewModel viewModel;

    /**
     * Cached so the submit handler can decide whether the next save
     * is a create (no data) or an update. Mirrors the VM's internal
     * configState — kept here to avoid a second observer hop in the
     * click handler.
     */
    @Nullable
    private ConfiguracionDto lastConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConfiguracionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ConfiguracionViewModel.class);

        viewModel.getConfigState().observe(getViewLifecycleOwner(), this::renderConfigState);
        viewModel.getSaveState().observe(getViewLifecycleOwner(), this::renderSaveState);

        binding.buttonSave.setOnClickListener(v -> submit());
        binding.buttonRetry.setOnClickListener(v -> viewModel.loadConfiguracion());
    }

    // ------------------------------------------------------------------
    // State observers
    // ------------------------------------------------------------------

    /**
     * Renders the current configuration state. LOADING hides the form
     * and shows the spinner. ERROR hides the form and shows the error
     * text + retry button. SUCCESS shows the form; a null payload
     * means "create mode" (empty fields + "Guardar" button) and a
     * non-null payload means "update mode" (prefilled + "Actualizar").
     */
    private void renderConfigState(UiState<ConfiguracionDto> state) {
        if (binding == null || state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.formContainer.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressBar.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                binding.buttonRetry.setVisibility(View.GONE);
                binding.formContainer.setVisibility(View.VISIBLE);

                ConfiguracionDto dto = state.getData();
                lastConfig = dto;
                if (dto == null) {
                    applyCreateMode();
                } else {
                    applyUpdateMode(dto);
                }
                break;
            case ERROR:
                binding.progressBar.setVisibility(View.GONE);
                binding.formContainer.setVisibility(View.GONE);
                binding.textError.setVisibility(View.VISIBLE);
                binding.buttonRetry.setVisibility(View.VISIBLE);
                binding.textError.setText(
                        state.getError() != null ? state.getError() : getString(R.string.error_generic)
                );
                break;
        }
    }

    /**
     * Renders the result of the latest create/update call. LOADING
     * disables the submit button. SUCCESS toasts and pops the back
     * stack so the user lands back on the dashboard. ERROR enables
     * the button again and shows a Snackbar with the parsed server
     * message.
     */
    private void renderSaveState(UiState<ConfiguracionDto> state) {
        if (binding == null || state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                binding.buttonSave.setEnabled(false);
                break;
            case SUCCESS:
                binding.buttonSave.setEnabled(true);
                Toast.makeText(requireContext(), R.string.config_saved, Toast.LENGTH_SHORT).show();
                navigateBack();
                break;
            case ERROR:
                binding.buttonSave.setEnabled(true);
                Snackbar.make(
                        binding.getRoot(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG
                ).show();
                break;
        }
    }

    // ------------------------------------------------------------------
    // Form rendering
    // ------------------------------------------------------------------

    private void applyCreateMode() {
        binding.buttonSave.setText(R.string.save_config);
        binding.inputNombre.setText("");
        binding.inputMetodoPagoId.setText("");
        binding.inputMetodoPagoNombre.setText("");
        binding.inputLatitud.setText("");
        binding.inputLongitud.setText("");
    }

    private void applyUpdateMode(ConfiguracionDto dto) {
        binding.buttonSave.setText(R.string.update_config);
        binding.inputNombre.setText(
                dto.getNombreGastronomico() != null ? dto.getNombreGastronomico() : ""
        );
        binding.inputMetodoPagoId.setText(
                dto.getMetodoPagoDefaultId() != null
                        ? String.format(Locale.getDefault(), "%d", dto.getMetodoPagoDefaultId())
                        : ""
        );
        binding.inputMetodoPagoNombre.setText(
                dto.getMetodoPagoDefaultNombre() != null ? dto.getMetodoPagoDefaultNombre() : ""
        );
        binding.inputLatitud.setText(
                dto.getLatitudPartida() != null
                        ? String.format(Locale.getDefault(), "%s", dto.getLatitudPartida())
                        : ""
        );
        binding.inputLongitud.setText(
                dto.getLongitudPartida() != null
                        ? String.format(Locale.getDefault(), "%s", dto.getLongitudPartida())
                        : ""
        );
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    private void submit() {
        ConfiguracionDto dto = new ConfiguracionDto();
        dto.setNombreGastronomico(textOf(binding.inputNombre));
        dto.setMetodoPagoDefaultId(parseInteger(textOf(binding.inputMetodoPagoId)));
        dto.setMetodoPagoDefaultNombre(textOf(binding.inputMetodoPagoNombre));
        dto.setLatitudPartida(parseDouble(textOf(binding.inputLatitud)));
        dto.setLongitudPartida(parseDouble(textOf(binding.inputLongitud)));
        // id is intentionally not read from the form — the VM copies it
        // from the cached config when this is an update, so the server
        // receives a body that matches the previously saved row.
        viewModel.saveConfiguracion(dto);
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------------------

    private static String textOf(android.widget.EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    @Nullable
    private static Integer parseInteger(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static Double parseDouble(String text) {
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void navigateBack() {
        NavController controller = Navigation.findNavController(requireView());
        controller.popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

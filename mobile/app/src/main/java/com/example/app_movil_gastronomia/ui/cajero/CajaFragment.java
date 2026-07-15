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

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.databinding.FragmentCajaBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Open/close caja screen for the cajero. Two modes are toggled by
 * the value of {@link CajaViewModel#getCajaState()}:
 *
 * <ul>
 *   <li><b>No open caja</b> ({@code SUCCESS} with data == null) — shows
 *       the "Abrir Caja" form: a monto de apertura field plus a
 *       primary action button.</li>
 *   <li><b>Open caja</b> ({@code SUCCESS} with data != null) — shows
 *       a details card (fecha, monto, usuario) plus the "Cerrar Caja"
 *       form: a monto de cierre real field plus an error-tinted
 *       action button.</li>
 * </ul>
 *
 * <p>Open and close results surface on separate VM streams: a
 * transient toast on success (the VM auto-reloads the status so the
 * fragment flips modes), and a Snackbar on error. A LOADING state on
 * either stream disables the matching submit button.</p>
 */
@AndroidEntryPoint
public class CajaFragment extends Fragment {

    private FragmentCajaBinding binding;
    private CajaViewModel viewModel;

    /**
     * Last caja DTO from {@code cajaState}, kept so the close button
     * can hand it to the VM along with the user-entered close amount.
     */
    @Nullable
    private CajaDto lastCaja;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCajaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CajaViewModel.class);

        binding.buttonAbrir.setOnClickListener(v -> submitAbrir());
        binding.buttonCerrar.setOnClickListener(v -> submitCerrar());
        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());

        viewModel.getCajaState().observe(getViewLifecycleOwner(), this::renderCaja);
        viewModel.getAbrirState().observe(getViewLifecycleOwner(), this::handleAbrir);
        viewModel.getCerrarState().observe(getViewLifecycleOwner(), this::handleCerrar);
    }

    // ------------------------------------------------------------------
    // Caja status render
    // ------------------------------------------------------------------

    private void renderCaja(UiState<CajaDto> state) {
        if (binding == null || state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                showLoading();
                break;
            case SUCCESS:
                lastCaja = state.getData();
                if (lastCaja == null) {
                    showNoCaja();
                } else {
                    showCajaAbierta(lastCaja);
                }
                break;
            case ERROR:
                showError(state.getError() != null
                        ? state.getError() : getString(R.string.error_generic));
                break;
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.groupNoCaja.setVisibility(View.GONE);
        binding.groupCajaAbierta.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showNoCaja() {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        binding.groupNoCaja.setVisibility(View.VISIBLE);
        binding.groupCajaAbierta.setVisibility(View.GONE);
        binding.inputMontoApertura.setText("");
        binding.buttonAbrir.setEnabled(true);
    }

    private void showCajaAbierta(CajaDto caja) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        binding.groupNoCaja.setVisibility(View.GONE);
        binding.groupCajaAbierta.setVisibility(View.VISIBLE);

        binding.textCajaFecha.setText(getString(
                R.string.caja_label_fecha_apertura,
                caja.getFechaApertura() != null ? caja.getFechaApertura() : ""));
        binding.textCajaMontoApertura.setText(getString(
                R.string.caja_label_monto_apertura, caja.getMontoApertura()));
        binding.textCajaUsuario.setText(getString(
                R.string.caja_label_usuario_apertura,
                caja.getUsuarioAperturaNombre() != null ? caja.getUsuarioAperturaNombre() : ""));

        binding.inputMontoCierre.setText("");
        binding.buttonCerrar.setEnabled(true);
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.groupNoCaja.setVisibility(View.GONE);
        binding.groupCajaAbierta.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message);
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // Open / close submissions
    // ------------------------------------------------------------------

    private void submitAbrir() {
        Double monto = parseMonto(binding.inputMontoApertura);
        if (monto == null) {
            Snackbar.make(binding.getRoot(),
                    R.string.validation_amount_required, Snackbar.LENGTH_LONG).show();
            return;
        }
        binding.buttonAbrir.setEnabled(false);
        viewModel.abrirCaja(monto);
    }

    private void submitCerrar() {
        if (lastCaja == null) return;
        Double monto = parseMonto(binding.inputMontoCierre);
        if (monto == null) {
            Snackbar.make(binding.getRoot(),
                    R.string.validation_amount_required, Snackbar.LENGTH_LONG).show();
            return;
        }
        binding.buttonCerrar.setEnabled(false);
        viewModel.cerrarCaja(lastCaja, monto);
    }

    /**
     * Parses the user-entered monto from a {@link TextInputEditText}.
     * Returns {@code null} for empty / non-numeric input so the caller
     * can show a validation Snackbar without a separate flag.
     */
    private static @Nullable Double parseMonto(TextInputEditText edit) {
        if (edit.getText() == null) return null;
        String text = edit.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Open / close results
    // ------------------------------------------------------------------

    private void handleAbrir(UiState<CajaDto> state) {
        if (binding == null || state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.buttonAbrir.setEnabled(false);
                break;
            case SUCCESS:
                binding.buttonAbrir.setEnabled(true);
                Toast.makeText(requireContext(),
                        R.string.caja_opened, Toast.LENGTH_SHORT).show();
                break;
            case ERROR:
                binding.buttonAbrir.setEnabled(true);
                Snackbar.make(binding.getRoot(),
                        state.getError() != null
                                ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void handleCerrar(UiState<CajaDto> state) {
        if (binding == null || state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.buttonCerrar.setEnabled(false);
                break;
            case SUCCESS:
                binding.buttonCerrar.setEnabled(true);
                Toast.makeText(requireContext(),
                        R.string.caja_close_success, Toast.LENGTH_SHORT).show();
                break;
            case ERROR:
                binding.buttonCerrar.setEnabled(true);
                Snackbar.make(binding.getRoot(),
                        state.getError() != null
                                ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

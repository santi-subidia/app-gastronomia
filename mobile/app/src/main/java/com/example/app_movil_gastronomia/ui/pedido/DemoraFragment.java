package com.example.app_movil_gastronomia.ui.pedido;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.demora.DemoraDto;
import com.example.app_movil_gastronomia.databinding.FragmentDemoraBinding;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * "Registrar Demora" form. The cajero or cocina staff enters the delay
 * in minutes and optional observations, then submits. The server derives
 * the responsible sector from the authenticated user's role. The form is
 * scoped to a single pedido — its id is read from the {@code pedidoId}
 * navigation argument.
 *
 * <p>The fragment is intentionally thin: it reads the form, runs a
 * tiny client-side validation, and forwards the values to
 * {@link DemoraViewModel}. All network plumbing lives in the VM. On
 * SUCCESS the fragment toasts the user and pops the back stack so
 * the caller (PedidoDetail) refreshes naturally.</p>
 */
@AndroidEntryPoint
public class DemoraFragment extends Fragment {

    private FragmentDemoraBinding binding;
    private DemoraViewModel viewModel;
    private int pedidoId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDemoraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoId = getArguments() != null ? getArguments().getInt("pedidoId", -1) : -1;

        viewModel = new ViewModelProvider(this).get(DemoraViewModel.class);

        binding.textTitle.setText(getString(R.string.demora_for_order, pedidoId));
        binding.buttonRegistrar.setOnClickListener(v -> submit());

        attachClearErrorOnType(binding.inputMinutos, binding.inputMinutosLayout);
        viewModel.getCrearState().observe(getViewLifecycleOwner(), this::handleCrearResult);
    }

    private void handleCrearResult(UiState<DemoraDto> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.buttonRegistrar.setEnabled(false);
                break;
            case SUCCESS:
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonRegistrar.setEnabled(true);
                Toast.makeText(requireContext(),
                        R.string.delay_registered, Toast.LENGTH_SHORT).show();
                navigateBack();
                break;
            case ERROR:
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonRegistrar.setEnabled(true);
                Snackbar.make(
                        binding.getRoot(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG
                ).show();
                break;
        }
    }

    private void submit() {
        String minutosStr = textOf(binding.inputMinutos);
        String observaciones = textOf(binding.inputObservaciones);

        if (TextUtils.isEmpty(minutosStr)) {
            binding.inputMinutosLayout.setError(getString(R.string.demora_minutes_hint));
            binding.inputMinutos.requestFocus();
            return;
        }
        int minutos;
        try {
            minutos = Integer.parseInt(minutosStr.trim());
        } catch (NumberFormatException e) {
            binding.inputMinutosLayout.setError(getString(R.string.demora_minutes_hint));
            binding.inputMinutos.requestFocus();
            return;
        }
        if (minutos <= 0) {
            binding.inputMinutosLayout.setError(getString(R.string.demora_minutes_hint));
            binding.inputMinutos.requestFocus();
            return;
        }
        binding.inputMinutosLayout.setError(null);

        viewModel.registrarDemora(pedidoId, minutos, observaciones);
    }

    private void navigateBack() {
        NavController controller = Navigation.findNavController(requireView());
        controller.popBackStack();
    }

    private static String textOf(EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    /**
     * Wires a one-shot error clearer: the moment the user types into
     * {@code field} we drop the {@code setError(...)} red helper text
     * on its parent {@code TextInputLayout}. Keeps the inline
     * validation feel consistent with Material guidelines without
     * pulling in a full form-attached approach.
     */
    private void attachClearErrorOnType(EditText field,
                                        com.google.android.material.textfield.TextInputLayout parent) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (parent.getError() != null) {
                    parent.setError(null);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.app_movil_gastronomia.ui.cajero;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.databinding.FragmentProductListBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductAdapter adapter;
    private AlertDialog productDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProductListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);

        adapter = new ProductAdapter(
                this::showEditProductDialog,
                this::showDeleteConfirmation);
        binding.recyclerViewProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewProducts.setAdapter(adapter);

        viewModel.getProductState().observe(getViewLifecycleOwner(), this::handleState);
        viewModel.getActionState().observe(getViewLifecycleOwner(), this::handleActionState);

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
        binding.fabAddProduct.setOnClickListener(v -> showCreateProductDialog());
    }

    private void handleState(UiState<List<ProductoDto>> state) {
        if (state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                showLoading();
                break;
            case SUCCESS:
                showContent(state.getData());
                break;
            case ERROR:
                showError(state.getError());
                break;
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerViewProducts.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(List<ProductoDto> products) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        if (products == null || products.isEmpty()) {
            binding.recyclerViewProducts.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerViewProducts.setVisibility(View.VISIBLE);
            adapter.submitList(products);
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewProducts.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    private void handleActionState(UiState<String> state) {
        if (state == null) return;

        switch (state.getStatus()) {
            case LOADING:
                break;
            case SUCCESS:
                Snackbar.make(binding.getRoot(), state.getData(), Snackbar.LENGTH_SHORT).show();
                if (productDialog != null) {
                    productDialog.dismiss();
                    productDialog = null;
                }
                break;
            case ERROR:
                Snackbar.make(binding.getRoot(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    private void showCreateProductDialog() {
        showProductDialog(null);
    }

    private void showEditProductDialog(ProductoDto product) {
        showProductDialog(product);
    }

    private void showProductDialog(@Nullable ProductoDto product) {
        View form = getLayoutInflater().inflate(R.layout.dialog_product_form, null);
        TextInputLayout nameLayout = form.findViewById(R.id.layout_product_name);
        TextInputLayout priceLayout = form.findViewById(R.id.layout_product_price);
        TextInputLayout delayLayout = form.findViewById(R.id.layout_product_delay);
        TextInputEditText nameInput = form.findViewById(R.id.input_product_name);
        TextInputEditText priceInput = form.findViewById(R.id.input_product_price);
        TextInputEditText delayInput = form.findViewById(R.id.input_product_delay);

        if (product != null) {
            nameInput.setText(product.getNombre());
            priceInput.setText(String.valueOf(product.getPrecio()));
            delayInput.setText(String.valueOf(product.getDemora()));
        }

        productDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(product == null ? R.string.create_product_title : R.string.edit_product_title)
                .setView(form)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.save_product, null)
                .create();

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSaveButton(productDialog, nameLayout, priceLayout, delayLayout,
                        nameInput, priceInput, delayInput);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        nameInput.addTextChangedListener(watcher);
        priceInput.addTextChangedListener(watcher);
        delayInput.addTextChangedListener(watcher);

        productDialog.setOnShowListener(dialog -> {
            Button saveButton = productDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                if (product == null) {
                    viewModel.createProduct(textOf(nameInput), textOf(priceInput), textOf(delayInput));
                } else {
                    viewModel.updateProduct(product.getId(), textOf(nameInput), textOf(priceInput),
                            textOf(delayInput));
                }
            });
            updateSaveButton(productDialog, nameLayout, priceLayout, delayLayout,
                    nameInput, priceInput, delayInput);
        });
        productDialog.show();
    }

    private void updateSaveButton(AlertDialog dialog,
                                  TextInputLayout nameLayout,
                                  TextInputLayout priceLayout,
                                  TextInputLayout delayLayout,
                                  TextInputEditText nameInput,
                                  TextInputEditText priceInput,
                                  TextInputEditText delayInput) {
        ProductListViewModel.ValidationResult validation =
                ProductListViewModel.validateProductInput(
                        textOf(nameInput), textOf(priceInput), textOf(delayInput));
        nameLayout.setError(null);
        priceLayout.setError(null);
        delayLayout.setError(null);
        if (!validation.isValid()) {
            if (textOf(nameInput).trim().isEmpty()) {
                nameLayout.setError(validation.getError());
            } else if (!isNumber(textOf(priceInput))) {
                priceLayout.setError(validation.getError());
            } else {
                delayLayout.setError(validation.getError());
            }
        }
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(validation.isValid());
        }
    }

    private boolean isNumber(String value) {
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void showDeleteConfirmation(ProductoDto product) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.confirm_delete_product))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm,
                        (dialog, which) -> viewModel.deleteProduct(product.getId()))
                .show();
    }

    @Override
    public void onDestroyView() {
        if (productDialog != null) {
            productDialog.dismiss();
            productDialog = null;
        }
        super.onDestroyView();
        binding = null;
    }
}

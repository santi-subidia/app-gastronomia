package com.example.app_movil_gastronomia.ui.cajero;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.databinding.FragmentProductListBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProductListFragment extends Fragment {

    private FragmentProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductAdapter adapter;

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

        adapter = new ProductAdapter();
        binding.recyclerViewProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewProducts.setAdapter(adapter);

        viewModel.getProductState().observe(getViewLifecycleOwner(), this::handleState);

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
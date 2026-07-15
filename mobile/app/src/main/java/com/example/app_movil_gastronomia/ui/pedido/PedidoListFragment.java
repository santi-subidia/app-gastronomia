package com.example.app_movil_gastronomia.ui.pedido;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.databinding.FragmentPedidoListBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Scrollable list of pedidos, filterable by estado. Renders a
 * LOADING / SUCCESS / ERROR / empty trio exactly like
 * {@link com.example.app_movil_gastronomia.ui.cajero.ProductListFragment}.
 * Tapping a card navigates to {@link PedidoDetailFragment} with the pedido id
 * passed as a SafeArgs integer.
 */
@AndroidEntryPoint
public class PedidoListFragment extends Fragment {

    private FragmentPedidoListBinding binding;
    private PedidoListViewModel viewModel;
    private PedidoAdapter adapter;

    /** Active filter chip — tracked so we can re-apply its selected style. */
    private TextView activeChip;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPedidoListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PedidoListViewModel.class);

        adapter = new PedidoAdapter();
        binding.recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPedidos.setAdapter(adapter);

        adapter.setOnItemClickListener(this::navigateToDetail);

        wireFilterChips();

        // Default selected chip: "Todos" (null filter).
        setActiveChip(binding.chipTodos);

        viewModel.getPedidoListState().observe(getViewLifecycleOwner(), this::handleState);

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
    }

    private void wireFilterChips() {
        binding.chipTodos.setOnClickListener(v -> {
            setActiveChip(binding.chipTodos);
            viewModel.filterByEstado(null);
        });
        binding.chipPendiente.setOnClickListener(v -> {
            setActiveChip(binding.chipPendiente);
            viewModel.filterByEstado(EstadoPedidoEnum.PENDIENTE);
        });
        binding.chipEnPreparacion.setOnClickListener(v -> {
            setActiveChip(binding.chipEnPreparacion);
            viewModel.filterByEstado(EstadoPedidoEnum.EN_PREPARACION);
        });
        binding.chipListo.setOnClickListener(v -> {
            setActiveChip(binding.chipListo);
            viewModel.filterByEstado(EstadoPedidoEnum.LISTO_PARA_RETIRAR);
        });
        binding.chipEnCamino.setOnClickListener(v -> {
            setActiveChip(binding.chipEnCamino);
            viewModel.filterByEstado(EstadoPedidoEnum.EN_CAMINO);
        });
        binding.chipEntregado.setOnClickListener(v -> {
            setActiveChip(binding.chipEntregado);
            viewModel.filterByEstado(EstadoPedidoEnum.ENTREGADO);
        });
    }

    private void setActiveChip(TextView chip) {
        int selectedBg = ContextCompat.getColor(requireContext(), R.color.chip_selected_bg);
        int selectedFg = ContextCompat.getColor(requireContext(), R.color.chip_selected_fg);
        int unselectedBg = ContextCompat.getColor(requireContext(), R.color.chip_unselected_bg);
        int unselectedFg = ContextCompat.getColor(requireContext(), R.color.chip_unselected_fg);

        if (activeChip != null) {
            activeChip.setBackgroundColor(unselectedBg);
            activeChip.setTextColor(unselectedFg);
        }
        activeChip = chip;
        if (activeChip != null) {
            activeChip.setBackgroundColor(selectedBg);
            activeChip.setTextColor(selectedFg);
        }
    }

    private void navigateToDetail(PedidoResumenDto pedido) {
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedido.getId());
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_pedido_list_to_nav_pedido_detail, args);
    }

    private void handleState(UiState<List<PedidoResumenDto>> state) {
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
        binding.recyclerViewPedidos.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(List<PedidoResumenDto> pedidos) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        if (pedidos == null || pedidos.isEmpty()) {
            binding.recyclerViewPedidos.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.recyclerViewPedidos.setVisibility(View.VISIBLE);
            adapter.submitList(pedidos);
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewPedidos.setVisibility(View.GONE);
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

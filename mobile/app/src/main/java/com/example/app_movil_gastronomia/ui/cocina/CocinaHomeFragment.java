package com.example.app_movil_gastronomia.ui.cocina;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;
import com.example.app_movil_gastronomia.databinding.FragmentCocinaHomeBinding;
import com.example.app_movil_gastronomia.ui.pedido.PedidoAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Cocina dashboard: a live queue of pedidos that still require kitchen
 * action. The full list is fetched via {@link com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository}
 * and then filtered client-side to only the estados the kitchen
 * cares about ({@code Pendiente} and {@code EnPreparacion}, matched
 * case-insensitively). The view is also refreshed automatically when
 * the SignalR hub pushes a {@code NuevoPedidoMessage}.
 *
 * <p>UI states mirror {@link com.example.app_movil_gastronomia.ui.cajero.ProductListFragment}:
 * LOADING shows a centered spinner, ERROR shows the message and a
 * retry button, SUCCESS shows the filtered list (or the empty
 * sub-state when the kitchen queue is fully drained).</p>
 */
@AndroidEntryPoint
public class CocinaHomeFragment extends Fragment {

    private FragmentCocinaHomeBinding binding;
    private CocinaHomeViewModel viewModel;
    private PedidoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCocinaHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CocinaHomeViewModel.class);

        adapter = new PedidoAdapter();
        binding.recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPedidos.setAdapter(adapter);

        adapter.setOnItemClickListener(this::navigateToDetail);

        viewModel.getCocinaState().observe(getViewLifecycleOwner(), this::handleState);

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
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
        binding.textEmptySub.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(List<PedidoResumenDto> pedidos) {
        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);

        List<PedidoResumenDto> filtered = filterForCocina(pedidos);

        if (filtered.isEmpty()) {
            binding.recyclerViewPedidos.setVisibility(View.GONE);
            binding.textEmpty.setVisibility(View.VISIBLE);
            binding.textEmptySub.setVisibility(View.VISIBLE);
        } else {
            binding.textEmpty.setVisibility(View.GONE);
            binding.textEmptySub.setVisibility(View.GONE);
            binding.recyclerViewPedidos.setVisibility(View.VISIBLE);
            adapter.submitList(filtered);
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewPedidos.setVisibility(View.GONE);
        binding.textEmpty.setVisibility(View.GONE);
        binding.textEmptySub.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    /**
     * Keeps only pedidos the kitchen is responsible for. Matches the
     * API value of {@code Pendiente} and {@code EnPreparacion}
     * case-insensitively, and also tolerates the human-friendly
     * "En Preparación" form (with the accent and a space) so a server
     * that ever sends the display label instead of the canonical
     * wire value still works.
     */
    static List<PedidoResumenDto> filterForCocina(List<PedidoResumenDto> pedidos) {
        List<PedidoResumenDto> result = new ArrayList<>();
        if (pedidos == null) {
            return result;
        }
        for (PedidoResumenDto p : pedidos) {
            if (isVisibleInCocina(p.getEstado())) {
                result.add(p);
            }
        }
        return result;
    }

    private static boolean isVisibleInCocina(String estado) {
        if (estado == null) return false;
        String normalized = estado.trim().toLowerCase();
        return "pendiente".equals(normalized)
                || "en preparacion".equals(normalized)
                || "enpreparacion".equals(normalized);
    }

    private void navigateToDetail(PedidoResumenDto pedido) {
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedido.getId());
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.nav_pedido_detail, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

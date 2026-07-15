package com.example.app_movil_gastronomia.ui.repartidor;

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
import com.example.app_movil_gastronomia.data.dto.signalr.PedidoFinalizadoMessage;
import com.example.app_movil_gastronomia.databinding.FragmentRepartidorHomeBinding;
import com.example.app_movil_gastronomia.ui.pedido.PedidoAdapter;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Repartidor dashboard: the live list of deliveries the rider is
 * currently responsible for. The full pedido list is fetched via
 * {@link com.example.app_movil_gastronomia.data.repository.contract.PedidoRepository}
 * and then filtered client-side to only the {@code "En Camino"}
 * estado (case-insensitive). The view is also refreshed automatically
 * when the SignalR hub pushes a {@code RepartidorAsignadoMessage}
 * (new assignment) and shows a transient snackbar when a
 * {@code PedidoFinalizadoMessage} arrives.
 *
 * <p>UI states mirror {@link com.example.app_movil_gastronomia.ui.cocina.CocinaHomeFragment}:
 * LOADING shows a centered spinner, ERROR shows the message and a
 * retry button, SUCCESS shows the filtered list (or the empty
 * sub-state when no deliveries are assigned).</p>
 */
@AndroidEntryPoint
public class RepartidorHomeFragment extends Fragment {

    private FragmentRepartidorHomeBinding binding;
    private RepartidorHomeViewModel viewModel;
    private PedidoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRepartidorHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RepartidorHomeViewModel.class);

        adapter = new PedidoAdapter();
        binding.recyclerViewPedidos.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewPedidos.setAdapter(adapter);

        adapter.setOnItemClickListener(this::navigateToDetail);

        viewModel.getRepartidorState().observe(getViewLifecycleOwner(), this::handleState);
        viewModel.getPedidoFinalizado().observe(getViewLifecycleOwner(), this::handlePedidoFinalizado);

        binding.buttonRetry.setOnClickListener(v -> viewModel.retry());
        binding.fabViewMap.setOnClickListener(v -> navigateToMapa());
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

        List<PedidoResumenDto> filtered = filterEnCamino(pedidos);

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
     * Keeps only pedidos the repartidor should see right now:
     * those in the {@code "En Camino"} estado. Case-insensitive
     * and tolerant of both the canonical API value
     * ({@code "EnCamino"}) and the human-friendly label
     * ({@code "En Camino"}).
     */
    static List<PedidoResumenDto> filterEnCamino(List<PedidoResumenDto> pedidos) {
        List<PedidoResumenDto> result = new ArrayList<>();
        if (pedidos == null) {
            return result;
        }
        for (PedidoResumenDto p : pedidos) {
            if (RepartidorHomeViewModel.isEnCamino(p.getEstado())) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Renders a transient "Entrega completada" snackbar when the hub
     * pushes a {@link PedidoFinalizadoMessage}. We do not auto-refresh
     * the list here: the next {@code RepartidorAsignadoMessage} or
     * explicit {@code retry()} will pull the fresh state and the
     * finalizado row will drop out.
     */
    private void handlePedidoFinalizado(PedidoFinalizadoMessage message) {
        if (message == null || binding == null) return;
        Snackbar.make(binding.getRoot(),
                R.string.delivery_completed,
                Snackbar.LENGTH_SHORT).show();
    }

    private void navigateToDetail(PedidoResumenDto pedido) {
        Bundle args = new Bundle();
        args.putInt("pedidoId", pedido.getId());
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.nav_pedido_detail, args);
    }

    /**
     * Navigates to the repartidor map screen. Wired to the
     * "Ver Mapa" extended FAB.
     */
    private void navigateToMapa() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_repartidor_home_to_nav_mapa);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

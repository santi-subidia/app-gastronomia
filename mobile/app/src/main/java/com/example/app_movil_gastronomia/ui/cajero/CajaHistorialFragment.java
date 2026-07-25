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
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialDetalleDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialResumenDto;
import com.example.app_movil_gastronomia.data.dto.caja.DetallePedidoCajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.PedidoCajaDetalleDto;
import com.example.app_movil_gastronomia.databinding.FragmentCajaHistorialBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CajaHistorialFragment extends Fragment {
    private FragmentCajaHistorialBinding binding;
    private CajaHistorialViewModel viewModel;
    private CajaHistorialAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCajaHistorialBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CajaHistorialViewModel.class);
        adapter = new CajaHistorialAdapter(caja -> viewModel.cargarDetalle(caja.getId()));
        binding.recyclerHistorial.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistorial.setAdapter(adapter);
        binding.buttonRetry.setOnClickListener(v -> viewModel.cargarHistorial());
        viewModel.getHistorialState().observe(getViewLifecycleOwner(), this::renderHistorial);
        viewModel.getDetalleState().observe(getViewLifecycleOwner(), this::renderDetalle);
    }

    private void renderHistorial(UiState<List<CajaHistorialResumenDto>> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.textError.setVisibility(View.GONE);
                break;
            case SUCCESS:
                binding.progressBar.setVisibility(View.GONE);
                binding.textError.setVisibility(View.GONE);
                adapter.submitList(state.getData());
                binding.textEmpty.setVisibility(state.getData() == null || state.getData().isEmpty() ? View.VISIBLE : View.GONE);
                break;
            case ERROR:
                binding.progressBar.setVisibility(View.GONE);
                binding.textEmpty.setVisibility(View.GONE);
                binding.textError.setVisibility(View.VISIBLE);
                binding.textError.setText(state.getError() != null ? state.getError() : getString(R.string.error_generic));
                break;
        }
    }

    private void renderDetalle(UiState<CajaHistorialDetalleDto> state) {
        if (state != null && state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
            showDetalle(state.getData());
        }
    }

    private void showDetalle(CajaHistorialDetalleDto caja) {
        StringBuilder message = new StringBuilder();
        message.append("Apertura: ").append(currency(caja.getMontoApertura())).append('\n')
                .append("Cierre teórico: ").append(currency(caja.getMontoCierreTeorico())).append('\n')
                .append("Cierre real: ").append(currency(caja.getMontoCierreReal())).append('\n')
                .append("Diferencia: ").append(currency(caja.getDiferenciaCierre())).append("\n\n")
                .append("Ingresos\nEfectivo: ").append(currency(caja.getIngresosEfectivo()))
                .append("\nTarjeta: ").append(currency(caja.getIngresosTarjeta()))
                .append("\nTransferencia: ").append(currency(caja.getIngresosTransferencia()))
                .append("\n\nPedidos:\n");
        List<PedidoCajaDetalleDto> pedidos = caja.getPedidos();
        if (pedidos == null || pedidos.isEmpty()) {
            message.append("No hay pedidos asociados.");
        } else {
            for (PedidoCajaDetalleDto pedido : pedidos) {
                message.append("\nPedido #").append(pedido.getId())
                        .append(" | ").append(valueOrDash(pedido.getEstado()))
                        .append("\nCliente: ").append(valueOrDash(pedido.getClienteNombre()))
                        .append("\nPago: ").append(valueOrDash(pedido.getMetodoPago()))
                        .append(" | Total: ").append(currency(pedido.getTotal())).append('\n');
                if (pedido.getDetalles() != null) {
                    for (DetallePedidoCajaDto detalle : pedido.getDetalles()) {
                        message.append("- ").append(detalle.getCantidad()).append(" x ")
                                .append(detalle.getNombre()).append(" (")
                                .append(currency(detalle.getPrecio())).append(")\n");
                    }
                }
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.caja_historial_detail_title, caja.getId()))
                .setMessage(message.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static String currency(double value) { return NumberFormat.getCurrencyInstance(new Locale("es", "AR")).format(value); }
    private static String valueOrDash(String value) { return value == null ? "-" : value; }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}

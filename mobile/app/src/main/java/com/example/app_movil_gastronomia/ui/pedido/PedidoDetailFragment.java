package com.example.app_movil_gastronomia.ui.pedido;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.DetallePedidoDto;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.databinding.FragmentPedidoDetailBinding;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Detail screen for a single pedido. Shows status banner, customer / method
 * / date / total info, items list, and three action buttons for the three
 * P0 actions: change estado, assign repartidor, register demora.
 */
@AndroidEntryPoint
public class PedidoDetailFragment extends Fragment {

    @javax.inject.Inject
    public TokenManager tokenManager;

    private FragmentPedidoDetailBinding binding;
    private PedidoDetailViewModel viewModel;
    private int pedidoId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPedidoDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pedidoId = getArguments() != null ? getArguments().getInt("pedidoId", -1) : -1;

        viewModel = new ViewModelProvider(this).get(PedidoDetailViewModel.class);

        viewModel.getDetailState().observe(getViewLifecycleOwner(), this::handleDetailState);
        viewModel.getCambiarEstadoState().observe(getViewLifecycleOwner(), this::handleCambiarEstadoResult);
        viewModel.getAsignarRepartidorState().observe(getViewLifecycleOwner(), this::handleAsignarRepartidorResult);

        binding.buttonCambiarEstado.setOnClickListener(v -> showCambiarEstadoDialog());
        binding.buttonCancelarPedido.setOnClickListener(v -> confirmCancelOrder());
        binding.buttonAsignarRepartidor.setOnClickListener(v -> showAsignarRepartidorDialog());
        binding.buttonRegistrarDemora.setOnClickListener(v -> {
            // Navigate to the Demora form, passing the current pedidoId
            // as a SafeArgs-equivalent Bundle argument. The Demora
            // fragment is responsible for the actual POST.
            Bundle args = new Bundle();
            args.putInt("pedidoId", pedidoId);
            NavController controller = Navigation.findNavController(v);
            controller.navigate(R.id.action_nav_pedido_detail_to_nav_demora, args);
        });
        binding.buttonRetry.setOnClickListener(v -> viewModel.loadPedido(pedidoId));

        if (pedidoId > 0) {
            viewModel.loadPedido(pedidoId);
        }
    }

    private void handleDetailState(UiState<PedidoDetalleDto> state) {
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

    private void handleCambiarEstadoResult(UiState<PedidoDetalleDto> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                // The detail screen already shows its own loader; suppress a second one.
                break;
            case SUCCESS:
                // Refresh the detail from the server so the banner / fields reflect
                // the new estado.
                Toast.makeText(requireContext(),
                        isCanceled(state.getData()) ? R.string.order_cancelled : R.string.change_status,
                        Toast.LENGTH_SHORT).show();
                viewModel.loadPedido(pedidoId);
                break;
            case ERROR:
                Toast.makeText(requireContext(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void handleAsignarRepartidorResult(UiState<PedidoDetalleDto> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                break;
            case SUCCESS:
                Toast.makeText(requireContext(),
                        R.string.assign_driver,
                        Toast.LENGTH_SHORT).show();
                viewModel.loadPedido(pedidoId);
                break;
            case ERROR:
                Toast.makeText(requireContext(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
    }

    private void showContent(PedidoDetalleDto pedido) {
        if (pedido == null) {
            return;
        }

        binding.progressBar.setVisibility(View.GONE);
        binding.textError.setVisibility(View.GONE);
        binding.buttonRetry.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.VISIBLE);

        // Update the action bar title with the pedido id.
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(
                        getString(R.string.order_detail_title, pedido.getId()));
            }
        }

        // Status banner
        EstadoPedidoEnum estado = EstadoPedidoEnum.fromApiValue(pedido.getEstado());
        int statusColor = PedidoAdapter.colorForEstado(estado);
        binding.statusBanner.setBackgroundColor(statusColor);
        binding.statusBanner.setText(PedidoAdapter.labelForEstado(estado));
        // Always pick a readable foreground: white on dark backgrounds,
        // black on the amber Pendiente chip.
        int fg = (estado == EstadoPedidoEnum.PENDIENTE) ? Color.BLACK : Color.WHITE;
        binding.statusBanner.setTextColor(fg);

        // Info card
        binding.clienteNombre.setText(pedido.getClienteNombre());
        String metodoVenta = pedido.getMetodoVenta() != null ? pedido.getMetodoVenta() : "";
        binding.metodoVenta.setText(metodoVenta);
        binding.fechaIngreso.setText(pedido.getFechaIngreso() != null
                ? pedido.getFechaIngreso() : "");
        binding.total.setText(String.format(Locale.getDefault(), "$%.0f", pedido.getTotalEstimado()));

        // Configuración de botones de acción
        String role = tokenManager.getRole() != null ? tokenManager.getRole().toLowerCase(Locale.ROOT) : "";
        boolean isCajero = "cajero".equals(role);
        
        if (isCajero) {
            // Lógica específica para el Cajero
            binding.buttonRegistrarDemora.setVisibility(View.GONE);
            
            if (estado == EstadoPedidoEnum.LISTO_PARA_RETIRAR) {
                if (metodoVenta.toLowerCase(Locale.ROOT).contains("delivery")) {
                    binding.buttonCambiarEstado.setVisibility(View.GONE);
                    binding.buttonAsignarRepartidor.setVisibility(View.VISIBLE);
                } else {
                    binding.buttonAsignarRepartidor.setVisibility(View.GONE);
                    binding.buttonCambiarEstado.setVisibility(View.VISIBLE);
                    binding.buttonCambiarEstado.setText("Entregar");
                    // Eliminamos el listener por defecto que abre el modal de todos los estados
                    // y hacemos que asigne ENTREGADO directamente.
                    binding.buttonCambiarEstado.setOnClickListener(v -> {
                        viewModel.cambiarEstado(pedidoId, EstadoPedidoEnum.ENTREGADO);
                    });
                }
            } else {
                // Si no está listo, el cajero no puede hacer ninguna acción terminal en esta vista
                binding.buttonCambiarEstado.setVisibility(View.GONE);
                binding.buttonAsignarRepartidor.setVisibility(View.GONE);
            }
        } else {
            // Lógica para Cocina / Repartidor (mantener comportamiento previo)
            binding.buttonCambiarEstado.setVisibility(View.VISIBLE);
            binding.buttonCambiarEstado.setText(R.string.change_status);
            binding.buttonCambiarEstado.setOnClickListener(v -> showCambiarEstadoDialog());
            binding.buttonAsignarRepartidor.setVisibility(View.VISIBLE);
            binding.buttonRegistrarDemora.setVisibility(View.VISIBLE);
        }

        boolean canCancel = PedidoCancellationPolicy.isCancelable(estado);
        binding.buttonCancelarPedido.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        // Items list
        renderItems(pedido.getDetallePedidos());
    }

    private void confirmCancelOrder() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cancel_order)
                .setMessage(R.string.confirm_cancel_order)
                .setPositiveButton(R.string.action_confirm,
                        (dialog, which) -> viewModel.cambiarEstado(
                                pedidoId, EstadoPedidoEnum.CANCELADO))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private static boolean isCanceled(PedidoDetalleDto pedido) {
        return pedido != null
                && EstadoPedidoEnum.fromApiValue(pedido.getEstado()) == EstadoPedidoEnum.CANCELADO;
    }

    private void renderItems(List<DetallePedidoDto> detalles) {
        binding.itemsContainer.removeAllViews();
        if (detalles == null || detalles.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("—");
            empty.setTextColor(Color.parseColor("#9E9E9E"));
            empty.setPadding(0, 8, 0, 0);
            binding.itemsContainer.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (DetallePedidoDto d : detalles) {
            TextView row = new TextView(requireContext());
            String nombre = d.getNombre() != null ? d.getNombre() : "";
            row.setText(String.format(Locale.getDefault(),
                    "%d × %s  —  $%.0f", d.getCantidad(), nombre, d.getPrecio()));
            row.setTextColor(Color.parseColor("#D0E4FF"));
            row.setPadding(0, 6, 0, 6);
            binding.itemsContainer.addView(row);
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.contentScroll.setVisibility(View.GONE);
        binding.textError.setVisibility(View.VISIBLE);
        binding.textError.setText(message != null ? message : getString(R.string.error_generic));
        binding.buttonRetry.setVisibility(View.VISIBLE);
    }

    private void showCambiarEstadoDialog() {
        UiState<PedidoDetalleDto> currentState = viewModel.getDetailState().getValue();
        if (currentState == null || currentState.getData() == null) return;
        
        PedidoDetalleDto pedido = currentState.getData();
        EstadoPedidoEnum estadoActual = EstadoPedidoEnum.fromApiValue(pedido.getEstado());
        
        String role = tokenManager.getRole();
        if (role != null) role = role.toLowerCase(Locale.ROOT);
        
        EstadoPedidoEnum[] opciones;

        if ("cajero".equals(role)) {
            if (estadoActual == EstadoPedidoEnum.LISTO_PARA_RETIRAR) {
                opciones = new EstadoPedidoEnum[]{ EstadoPedidoEnum.ENTREGADO };
            } else {
                Toast.makeText(requireContext(), "El cajero solo puede entregar pedidos que están Listos.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            opciones = new EstadoPedidoEnum[]{
                    EstadoPedidoEnum.PENDIENTE,
                    EstadoPedidoEnum.EN_PREPARACION,
                    EstadoPedidoEnum.LISTO_PARA_RETIRAR,
                    EstadoPedidoEnum.EN_CAMINO,
                    EstadoPedidoEnum.ENTREGADO,
                    EstadoPedidoEnum.CANCELADO
            };
        }

        String[] labels = new String[opciones.length];
        for (int i = 0; i < opciones.length; i++) {
            labels[i] = PedidoAdapter.labelForEstado(opciones[i]);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_status)
                .setItems(labels, (dialog, which) -> {
                    EstadoPedidoEnum elegido = opciones[which];
                    viewModel.cambiarEstado(pedidoId, elegido);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAsignarRepartidorDialog() {
        // Build a programmatic EditText to keep the layout XML lean.
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint(R.string.driver_id_hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        LinearLayout container = new LinearLayout(requireContext());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.assign_driver)
                .setView(container)
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    CharSequence raw = input.getText();
                    String text = raw != null ? raw.toString().trim() : "";
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(requireContext(),
                                R.string.driver_id_hint,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int repartidorId = Integer.parseInt(text);
                        viewModel.asignarRepartidor(pedidoId, repartidorId);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(),
                                R.string.driver_id_hint,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

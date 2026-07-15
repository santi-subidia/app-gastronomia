package com.example.app_movil_gastronomia.ui.pedido;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.pedido.CrearPedidoRequest;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoDetalleDto;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;
import com.example.app_movil_gastronomia.databinding.FragmentCrearPedidoBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * New-pedido form. The cajero enters the customer name, picks a
 * sales method and a payment method, optionally fills the delivery
 * address + lat/lng when the method is Delivery, adds one or more
 * product lines through a picker dialog, and submits the result as
 * a POST /api/pedidos.
 *
 * <p>The fragment is intentionally thin: all validation, total
 * recomputation, and network plumbing live in
 * {@link CrearPedidoViewModel}. The fragment's job is to (1) read
 * the form into a {@link CrearPedidoRequest}, (2) react to the VM's
 * state streams, and (3) drive the two dialogs (product picker +
 * quantity input).</p>
 */
@AndroidEntryPoint
public class CrearPedidoFragment extends Fragment {

    /** Server identifier for the Delivery sales method. */
    private static final int METODO_VENTA_DELIVERY = 1;

    /**
     * Local dropdown labels in display order, paired 1:1 with their
     * server id below. Kept as parallel arrays to avoid building a
     * small DTO just for this.
     */
    private static final String[] METODO_VENTA_LABELS = {
            "Retiro en local", // id 2
            "Delivery"         // id 1
    };
    /** Server metodoVentaId values, in display order. */
    private static final int[] METODO_VENTA_IDS = {2, 1};

    private static final String[] METODO_PAGO_LABELS = {
            "Efectivo",      // id 1
            "Tarjeta",       // id 2
            "Transferencia"  // id 3
    };
    private static final int[] METODO_PAGO_IDS = {1, 2, 3};

    /** Default selected sales method (Para llevar). */
    private static final int DEFAULT_METODO_VENTA_INDEX = 0;
    /** Default selected payment method (Efectivo). */
    private static final int DEFAULT_METODO_PAGO_INDEX = 0;

    private FragmentCrearPedidoBinding binding;
    private CrearPedidoViewModel viewModel;
    private DetalleAdapter detalleAdapter;

    /**
     * Detalle lines currently in the form. Edited in-place as the user
     * adds / removes rows. The list holds UI-layer
     * {@link DetalleLine} objects; the conversion to the wire
     * {@code CrearDetalleRequest} happens in
     * {@link CrearPedidoViewModel#mapDetalles(List)} at submit time.
     */
    private final List<DetalleLine> detalles = new ArrayList<>();

    /** Last product list received from the VM, used to populate the picker. */
    private List<ProductoDto> lastProductos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCrearPedidoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CrearPedidoViewModel.class);

        setupDropdowns();
        setupDetalleList();
        setupDeliveryVisibility();

        binding.buttonAddProduct.setOnClickListener(v -> openProductPicker());
        binding.buttonCrear.setOnClickListener(v -> submit());

        viewModel.getProductListState().observe(getViewLifecycleOwner(), this::handleProductos);
        viewModel.getCrearState().observe(getViewLifecycleOwner(), this::handleCrearResult);
        viewModel.getFormError().observe(getViewLifecycleOwner(), this::handleFormError);
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private void setupDropdowns() {
        ArrayAdapter<String> ventaAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                METODO_VENTA_LABELS
        );
        binding.inputMetodoVenta.setAdapter(ventaAdapter);
        binding.inputMetodoVenta.setText(METODO_VENTA_LABELS[DEFAULT_METODO_VENTA_INDEX], false);
        binding.inputMetodoVenta.setOnClickListener(v ->
                binding.inputMetodoVenta.showDropDown());

        ArrayAdapter<String> pagoAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                METODO_PAGO_LABELS
        );
        binding.inputMetodoPago.setAdapter(pagoAdapter);
        binding.inputMetodoPago.setText(METODO_PAGO_LABELS[DEFAULT_METODO_PAGO_INDEX], false);
        binding.inputMetodoPago.setOnClickListener(v ->
                binding.inputMetodoPago.showDropDown());
    }

    private void setupDetalleList() {
        detalleAdapter = new DetalleAdapter();
        detalleAdapter.setOnDeleteClickListener(this::removeDetalle);
        binding.recyclerViewDetalles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewDetalles.setAdapter(detalleAdapter);
        detalleAdapter.submitList(detalles);
        renderTotal();
        renderEmptyState();
    }

    private void setupDeliveryVisibility() {
        // Show delivery fields iff the default selection is Delivery.
        applyDeliveryVisibility(currentMetodoVentaId());

        binding.inputMetodoVenta.setOnItemClickListener((parent, view, position, id) -> {
            // Re-read the selected label so the AutoCompleteTextView
            // shows it after a tap; AutoCompleteTextView handles the
            // text update itself when an item is clicked, so we just
            // need to react to the position change.
            applyDeliveryVisibility(METODO_VENTA_IDS[position]);
        });
    }

    private void applyDeliveryVisibility(int metodoVentaId) {
        boolean isDelivery = metodoVentaId == METODO_VENTA_DELIVERY;
        binding.groupDelivery.setVisibility(isDelivery ? View.VISIBLE : View.GONE);
        if (!isDelivery) {
            // Clear the delivery-only fields so a stale value never
            // slips into a non-Delivery request.
            binding.inputClienteDireccion.setText("");
            binding.inputLatitud.setText("");
            binding.inputLongitud.setText("");
        }
    }

    // ------------------------------------------------------------------
    // State observers
    // ------------------------------------------------------------------

    private void handleProductos(UiState<List<ProductoDto>> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                // No full-screen spinner — the picker dialog shows its own.
                break;
            case SUCCESS:
                lastProductos = state.getData() != null ? state.getData() : new ArrayList<>();
                break;
            case ERROR:
                lastProductos = new ArrayList<>();
                // Surface a transient toast so the user can retry by tapping
                // "Agregar Producto" again.
                Toast.makeText(requireContext(),
                        state.getError() != null ? state.getError() : getString(R.string.error_generic),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void handleCrearResult(UiState<PedidoDetalleDto> state) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                binding.buttonCrear.setEnabled(false);
                break;
            case SUCCESS:
                binding.buttonCrear.setEnabled(true);
                Toast.makeText(requireContext(), R.string.order_created, Toast.LENGTH_SHORT).show();
                navigateBack();
                break;
            case ERROR:
                binding.buttonCrear.setEnabled(true);
                if (CrearPedidoViewModel.shouldOfferOpenRegister(state)) {
                    showOpenRegisterDialog(state.getError());
                } else {
                    Snackbar.make(
                            binding.getRoot(),
                            state.getError() != null ? state.getError() : getString(R.string.error_generic),
                            Snackbar.LENGTH_LONG
                    ).show();
                }
                break;
        }
    }

    private void showOpenRegisterDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_open_caja)
                .setMessage(message != null ? message : getString(R.string.error_generic))
                .setPositiveButton(R.string.open_caja_button, (dialog, which) -> navigateToCaja())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void navigateToCaja() {
        NavController controller = Navigation.findNavController(requireView());
        controller.navigate(R.id.action_nav_crear_pedido_to_nav_caja);
    }

    private void handleFormError(@Nullable String message) {
        if (message == null) return;
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        viewModel.acknowledgeFormError();
    }

    // ------------------------------------------------------------------
    // Detalle editing
    // ------------------------------------------------------------------

    private void openProductPicker() {
        if (lastProductos == null || lastProductos.isEmpty()) {
            Snackbar.make(binding.getRoot(),
                    R.string.no_products_available, Snackbar.LENGTH_SHORT).show();
            // Trigger a retry in case the catalog hadn't loaded yet.
            viewModel.loadProductos();
            return;
        }

        String[] labels = new String[lastProductos.size()];
        for (int i = 0; i < lastProductos.size(); i++) {
            ProductoDto p = lastProductos.get(i);
            labels[i] = String.format(Locale.getDefault(),
                    "%s — $%.0f", p.getNombre(), p.getPrecio());
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_product)
                .setItems(labels, (dialog, which) -> {
                    ProductoDto selected = lastProductos.get(which);
                    promptCantidad(selected);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void promptCantidad(ProductoDto producto) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.quantity_label);
        input.setText("1");
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(requireContext())
                .setTitle(producto.getNombre())
                .setView(input)
                .setPositiveButton(R.string.action_confirm, (dialog, which) -> {
                    int cantidad = parseCantidad(input.getText().toString());
                    if (cantidad <= 0) {
                        Snackbar.make(binding.getRoot(),
                                R.string.validation_details_required, Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    addDetalle(producto, cantidad);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private int parseCantidad(String text) {
        if (TextUtils.isEmpty(text)) return 0;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void addDetalle(ProductoDto producto, int cantidad) {
        // If the same product is already in the list, merge by summing quantity.
        for (int i = 0; i < detalles.size(); i++) {
            DetalleLine existing = detalles.get(i);
            if (existing.getProductoId() == producto.getId()) {
                DetalleLine merged = new DetalleLine(
                        existing.getProductoId(),
                        existing.getNombre(),
                        existing.getPrecio(),
                        existing.getCantidad() + cantidad
                );
                detalles.set(i, merged);
                refreshDetalleUi();
                return;
            }
        }
        detalles.add(new DetalleLine(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                cantidad
        ));
        refreshDetalleUi();
    }

    private void removeDetalle(int position) {
        if (position < 0 || position >= detalles.size()) return;
        detalles.remove(position);
        refreshDetalleUi();
    }

    private void refreshDetalleUi() {
        detalleAdapter.submitList(new ArrayList<>(detalles));
        renderTotal();
        renderEmptyState();
    }

    private void renderTotal() {
        double total = 0d;
        for (DetalleLine d : detalles) {
            total += d.getPrecio() * d.getCantidad();
        }
        binding.textTotal.setText(String.format(Locale.getDefault(), "Total: $%.0f", total));
    }

    private void renderEmptyState() {
        boolean empty = detalles.isEmpty();
        binding.textDetallesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerViewDetalles.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------

    private void submit() {
        CrearPedidoRequest request = buildRequestFromForm();
        viewModel.crearPedido(request);
    }

    private CrearPedidoRequest buildRequestFromForm() {
        String clienteNombre = textOf(binding.inputClienteNombre);
        int metodoVentaId = currentMetodoVentaId();
        int metodoPagoId = currentMetodoPagoId();
        String clienteDireccion = null;
        Double latitud = null;
        Double longitud = null;
        if (metodoVentaId == METODO_VENTA_DELIVERY) {
            clienteDireccion = textOf(binding.inputClienteDireccion);
            latitud = parseDouble(textOf(binding.inputLatitud));
            longitud = parseDouble(textOf(binding.inputLongitud));
        }

        // Delegate request building to the ViewModel so the fragment
        // never imports the wire DTO (CrearDetalleRequest). The VM
        // maps DetalleLine → CrearDetalleRequest internally and
        // computes totalEstimado from the mapped DTOs.
        return viewModel.buildRequest(
                clienteNombre,
                metodoVentaId,
                metodoPagoId,
                clienteDireccion,
                latitud,
                longitud,
                detalles
        );
    }

    private int currentMetodoVentaId() {
        String label = binding.inputMetodoVenta.getText() != null
                ? binding.inputMetodoVenta.getText().toString() : "";
        for (int i = 0; i < METODO_VENTA_LABELS.length; i++) {
            if (METODO_VENTA_LABELS[i].equals(label)) {
                return METODO_VENTA_IDS[i];
            }
        }
        return METODO_VENTA_IDS[DEFAULT_METODO_VENTA_INDEX];
    }

    private int currentMetodoPagoId() {
        String label = binding.inputMetodoPago.getText() != null
                ? binding.inputMetodoPago.getText().toString() : "";
        for (int i = 0; i < METODO_PAGO_LABELS.length; i++) {
            if (METODO_PAGO_LABELS[i].equals(label)) {
                return METODO_PAGO_IDS[i];
            }
        }
        return METODO_PAGO_IDS[DEFAULT_METODO_PAGO_INDEX];
    }

    private static String textOf(EditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

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

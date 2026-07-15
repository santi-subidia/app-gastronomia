package com.example.app_movil_gastronomia.ui.pedido;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_movil_gastronomia.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the in-progress pedido's detalle lines.
 *
 * <p>Each row is a {@link DetalleLine} snapshot: producto name,
 * quantity, line subtotal, and a delete button. The host
 * ({@link CrearPedidoFragment}) rebuilds the list on every add /
 * remove so this adapter is intentionally a "dumb" rendering
 * surface — no diffing, no animations.</p>
 *
 * <p>Spec PED-CRUD-001 / pedido-creacion "DetalleAdapter renders
 * DetalleLine": the adapter does not know about the wire DTO
 * {@code CrearDetalleRequest} — that mapping happens in
 * {@link CrearPedidoViewModel#mapDetalles(List)} at submit time.</p>
 */
public class DetalleAdapter extends RecyclerView.Adapter<DetalleAdapter.DetalleViewHolder> {

    /** Click callback for the trailing delete button on each row. */
    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    private final List<DetalleLine> items = new ArrayList<>();
    private OnDeleteClickListener deleteListener;

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    public void submitList(List<DetalleLine> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public DetalleLine getItem(int position) {
        return items.get(position);
    }

    public int getItemCountSafe() {
        return items.size();
    }

    @NonNull
    @Override
    public DetalleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_detalle, parent, false);
        return new DetalleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetalleViewHolder holder, int position) {
        DetalleLine detalle = items.get(position);

        holder.nombre.setText(detalle.getNombre() != null ? detalle.getNombre() : "");
        holder.cantidad.setText(String.format(Locale.getDefault(), "x%d", detalle.getCantidad()));
        double subtotal = detalle.getPrecio() * detalle.getCantidad();
        holder.subtotal.setText(String.format(Locale.getDefault(), "$%.0f", subtotal));

        holder.delete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DetalleViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView cantidad;
        final TextView subtotal;
        final ImageButton delete;

        DetalleViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.detalle_nombre);
            cantidad = itemView.findViewById(R.id.detalle_cantidad);
            subtotal = itemView.findViewById(R.id.detalle_subtotal);
            delete = itemView.findViewById(R.id.detalle_delete);

            // Tint the delete button red for clarity in the dark theme.
            int red = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light);
            delete.setColorFilter(red);
        }
    }
}

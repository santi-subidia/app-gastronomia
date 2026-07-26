package com.example.app_movil_gastronomia.ui.pedido;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.data.dto.pedido.EstadoPedidoEnum;
import com.example.app_movil_gastronomia.data.dto.pedido.PedidoResumenDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder> {

    private final List<PedidoResumenDto> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(PedidoResumenDto pedido);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<PedidoResumenDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        PedidoResumenDto pedido = items.get(position);

        holder.pedidoIdText.setText(String.format(Locale.getDefault(), "#%d", pedido.getId()));

        EstadoPedidoEnum estado = pedido.getEstado();
        int statusColor = colorForEstado(estado);
        int textColor = textColorForEstado(estado);

        holder.statusChip.setText(labelForEstado(estado));
        holder.statusChip.setBackgroundColor(statusColor);
        holder.statusChip.setTextColor(textColor);

        holder.accentBorder.setBackgroundColor(statusColor);

        holder.clienteNombreText.setText(pedido.getClienteNombre());

        holder.metodoVentaText.setText(pedido.getMetodoVenta() != null
                ? pedido.getMetodoVenta() : "");

        holder.totalText.setText(String.format(Locale.getDefault(), "$%.0f", pedido.getTotalEstimado()));

        holder.fechaIngresoText.setText(pedido.getFechaIngreso() != null
                ? pedido.getFechaIngreso() : "");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pedido);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static int colorForEstado(EstadoPedidoEnum estado) {
        if (estado == null) return Color.parseColor("#9E9E9E");
        switch (estado) {
            case PENDIENTE:
                return Color.parseColor("#FFC107");
            case EN_PREPARACION:
                return Color.parseColor("#2196F3");
            case LISTO_PARA_RETIRAR:
                return Color.parseColor("#4CAF50");
            case EN_CAMINO:
                return Color.parseColor("#9C27B0");
            case CONTINGENCIA:
                return Color.parseColor("#FF9800");
            case ENTREGADO:
                return Color.parseColor("#9E9E9E");
            case RETIRADO:
                return Color.parseColor("#9E9E9E");
            case CANCELADO:
                return Color.parseColor("#F44336");
            case DEVUELTO:
                return Color.parseColor("#F44336");
            default:
                return Color.parseColor("#9E9E9E");
        }
    }

    static int textColorForEstado(EstadoPedidoEnum estado) {
        if (estado == null) return Color.WHITE;
        switch (estado) {
            case PENDIENTE:
                return Color.BLACK;
            default:
                return Color.WHITE;
        }
    }

    static String labelForEstado(EstadoPedidoEnum estado) {
        if (estado == null) return "—";
        switch (estado) {
            case PENDIENTE:
                return "Pendiente";
            case EN_PREPARACION:
                return "En Preparación";
            case LISTO_PARA_RETIRAR:
                return "Listo";
            case EN_CAMINO:
                return "En Camino";
            case ENTREGADO:
                return "Entregado";
            case CONTINGENCIA:
                return "Contingencia";
            case RETIRADO:
                return "Retirado";
            case CANCELADO:
                return "Cancelado";
            case DEVUELTO:
                return "Devuelto";
            default:
                return estado.name();
        }
    }

    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        final View accentBorder;
        final TextView pedidoIdText;
        final TextView statusChip;
        final TextView clienteNombreText;
        final TextView metodoVentaText;
        final TextView totalText;
        final TextView fechaIngresoText;

        PedidoViewHolder(View itemView) {
            super(itemView);
            accentBorder = itemView.findViewById(R.id.pedido_accent_border);
            pedidoIdText = itemView.findViewById(R.id.pedido_id);
            statusChip = itemView.findViewById(R.id.pedido_status_chip);
            clienteNombreText = itemView.findViewById(R.id.pedido_cliente_nombre);
            metodoVentaText = itemView.findViewById(R.id.pedido_metodo_venta);
            totalText = itemView.findViewById(R.id.pedido_total);
            fechaIngresoText = itemView.findViewById(R.id.pedido_fecha_ingreso);
        }
    }
}

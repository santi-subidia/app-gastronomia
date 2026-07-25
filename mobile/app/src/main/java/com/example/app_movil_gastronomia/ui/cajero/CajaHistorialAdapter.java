package com.example.app_movil_gastronomia.ui.cajero;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialResumenDto;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CajaHistorialAdapter extends RecyclerView.Adapter<CajaHistorialAdapter.ViewHolder> {
    public interface OnCajaClickListener { void onCajaClick(CajaHistorialResumenDto caja); }
    private final List<CajaHistorialResumenDto> items = new ArrayList<>();
    private final OnCajaClickListener listener;

    public CajaHistorialAdapter(OnCajaClickListener listener) { this.listener = listener; }
    public void submitList(List<CajaHistorialResumenDto> cajas) {
        items.clear();
        if (cajas != null) items.addAll(cajas);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_caja_historial, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CajaHistorialResumenDto caja = items.get(position);
        holder.title.setText(String.format(Locale.getDefault(), "Caja #%d", caja.getId()));
        holder.date.setText(String.format(Locale.getDefault(), "%s\nCerrada por: %s",
                caja.getFechaCierre(), valueOrDash(caja.getUsuarioCierreNombre())));
        holder.amount.setText(String.format(Locale.getDefault(), "Cierre real: %s", currency(caja.getMontoCierreReal())));
        holder.summary.setText(String.format(Locale.getDefault(), "%d pedidos | Diferencia: %s",
                caja.getCantidadPedidos(), currency(caja.getDiferenciaCierre())));
        holder.itemView.setOnClickListener(v -> listener.onCajaClick(caja));
    }

    @Override public int getItemCount() { return items.size(); }
    private static String currency(double value) { return NumberFormat.getCurrencyInstance(new Locale("es", "AR")).format(value); }
    private static String valueOrDash(String value) { return value == null ? "-" : value; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, date, amount, summary;
        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.caja_historial_title);
            date = view.findViewById(R.id.caja_historial_date);
            amount = view.findViewById(R.id.caja_historial_amount);
            summary = view.findViewById(R.id.caja_historial_summary);
        }
    }
}

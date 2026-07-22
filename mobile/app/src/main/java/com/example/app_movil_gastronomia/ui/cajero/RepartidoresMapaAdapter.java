package com.example.app_movil_gastronomia.ui.cajero;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_movil_gastronomia.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Renders all delivery drivers, including those without a live location. */
public class RepartidoresMapaAdapter
        extends RecyclerView.Adapter<RepartidoresMapaAdapter.RepartidorViewHolder> {

    private final List<RepartidorUiModel> items = new ArrayList<>();

    public void submitList(List<RepartidorUiModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RepartidorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repartidor_mapa, parent, false);
        return new RepartidorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RepartidorViewHolder holder, int position) {
        RepartidorUiModel item = items.get(position);
        holder.name.setText(item.getNombre());
        holder.status.setText(item.getEstado());
        if (item.hasLocation()) {
            holder.location.setText(String.format(Locale.US, "%.6f, %.6f",
                    item.getLatitud(), item.getLongitud()));
        } else {
            holder.location.setText(R.string.driver_location_unavailable);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class RepartidorViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView status;
        final TextView location;

        RepartidorViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_repartidor_name);
            status = itemView.findViewById(R.id.text_repartidor_status);
            location = itemView.findViewById(R.id.text_repartidor_location);
        }
    }
}

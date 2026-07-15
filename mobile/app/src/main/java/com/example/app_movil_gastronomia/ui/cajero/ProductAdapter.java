package com.example.app_movil_gastronomia.ui.cajero;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_movil_gastronomia.R;
import com.example.app_movil_gastronomia.data.dto.producto.ProductoDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<ProductoDto> items = new ArrayList<>();

    public void submitList(List<ProductoDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductoDto product = items.get(position);
        holder.nameText.setText(product.getNombre());
        holder.priceText.setText(String.format(Locale.getDefault(), "$%.0f", product.getPrecio()));
        holder.detailText.setText(String.format(Locale.getDefault(), "%d min", product.getDemora()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView priceText;
        final TextView detailText;

        ProductViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name);
            priceText = itemView.findViewById(R.id.product_price);
            detailText = itemView.findViewById(R.id.product_detail);
        }
    }
}
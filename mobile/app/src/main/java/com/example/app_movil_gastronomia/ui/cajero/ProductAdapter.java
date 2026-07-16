package com.example.app_movil_gastronomia.ui.cajero;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private final OnProductEditListener editListener;
    private final OnProductDeleteListener deleteListener;

    public interface OnProductEditListener {
        void onEdit(ProductoDto product);
    }

    public interface OnProductDeleteListener {
        void onDelete(ProductoDto product);
    }

    public ProductAdapter() {
        this(null, null);
    }

    public ProductAdapter(OnProductEditListener editListener,
                          OnProductDeleteListener deleteListener) {
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<ProductoDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        if (hasObservers()) {
            notifyDataSetChanged();
        }
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
        holder.priceText.setText(formatPrice(product.getPrecio()));
        holder.detailText.setText(String.format(Locale.getDefault(), "%d min", product.getDemora()));
        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(product);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(product);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static String formatPrice(double price) {
        return String.format(Locale.getDefault(), "$%.0f", price);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView priceText;
        final TextView detailText;
        final ImageButton editButton;
        final ImageButton deleteButton;

        ProductViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.product_name);
            priceText = itemView.findViewById(R.id.product_price);
            detailText = itemView.findViewById(R.id.product_detail);
            editButton = itemView.findViewById(R.id.button_edit_product);
            deleteButton = itemView.findViewById(R.id.button_delete_product);
        }
    }
}

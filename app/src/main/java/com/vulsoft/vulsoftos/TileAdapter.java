package com.vulsoft.vulsoftos;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {

    public interface OnTileClickListener {
        void onTileClick(AppItem item);
    }

    private final List<AppItem> items;
    private final OnTileClickListener clickListener;
    private int accentColor = Color.parseColor("#0078D7"); // Windows Phone Blue

    public TileAdapter(List<AppItem> items, OnTileClickListener clickListener) {
        this.items = items;
        this.clickListener = clickListener;
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tile_wp, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        AppItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.label.setText(item.label);
        holder.icon.setImageDrawable(IconPackManager.getIcon(context, item.packageName, item.icon));
        
        holder.container.setCardBackgroundColor(accentColor);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTileClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        CardView container;
        ImageView icon;
        TextView label;

        public TileViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.tileContainer);
            icon = itemView.findViewById(R.id.tileIcon);
            label = itemView.findViewById(R.id.tileLabel);
        }
    }
}

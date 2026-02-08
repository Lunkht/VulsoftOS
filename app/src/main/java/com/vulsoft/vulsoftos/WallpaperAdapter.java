package com.vulsoft.vulsoftos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WallpaperAdapter extends RecyclerView.Adapter<WallpaperAdapter.ViewHolder> {

    private final List<Integer> wallpaperList;
    private int selectedPosition = -1;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public WallpaperAdapter(List<Integer> wallpaperList, OnItemClickListener listener) {
        this.wallpaperList = wallpaperList;
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int previous = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previous);
        notifyItemChanged(selectedPosition);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imgWallpaper.setImageResource(wallpaperList.get(position));

        // Visual feedback for selection
        if (position == selectedPosition) {
            holder.itemView.setScaleX(1.05f);
            holder.itemView.setScaleY(1.05f);
            holder.itemView.setAlpha(1.0f);
            ((androidx.cardview.widget.CardView) holder.itemView).setCardElevation(dpToPx(12, holder.itemView));
            holder.imgCheck.setVisibility(View.VISIBLE);
        } else {
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
            holder.itemView.setAlpha(0.6f);
            ((androidx.cardview.widget.CardView) holder.itemView).setCardElevation(dpToPx(2, holder.itemView));
            holder.imgCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wallpaperList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgWallpaper;
        ImageView imgCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgWallpaper = itemView.findViewById(R.id.imgWallpaper);
            imgCheck = itemView.findViewById(R.id.imgCheck);
        }
    }

    private float dpToPx(float dp, View view) {
        return dp * view.getContext().getResources().getDisplayMetrics().density;
    }
}

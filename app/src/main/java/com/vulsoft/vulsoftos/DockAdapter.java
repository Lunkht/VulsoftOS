package com.vulsoft.vulsoftos;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DockAdapter extends RecyclerView.Adapter<DockAdapter.DockViewHolder> {

    public interface OnDockClickListener {
        void onDockClick(AppItem appItem);
    }

    private final List<AppItem> dockApps;
    private final OnDockClickListener listener;
    private final AppsAdapter.OnAppLongClickListener longClickListener;
    private int iconRadiusPercent = 25; // Default
    private java.util.Map<String, Integer> notificationCounts = new java.util.HashMap<>();
    private boolean isDragEnabled = false;
    private int iconSize = -1;
    private int cellWidth = -1;

    public DockAdapter(List<AppItem> dockApps, OnDockClickListener listener,
            AppsAdapter.OnAppLongClickListener longClickListener) {
        this.dockApps = dockApps;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setDragEnabled(boolean enabled) {
        this.isDragEnabled = enabled;
        notifyDataSetChanged();
    }

    public void updateNotificationCounts(java.util.Map<String, Integer> counts) {
        this.notificationCounts = counts;
        notifyDataSetChanged();
    }

    public void setIconRadiusPercent(int percent) {
        this.iconRadiusPercent = percent;
        notifyDataSetChanged();
    }

    public void setIconSize(int sizePx) {
        this.iconSize = sizePx;
        notifyDataSetChanged();
    }

    public void setCellWidth(int widthPx) {
        this.cellWidth = widthPx;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dock, parent, false);
        return new DockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DockViewHolder holder, int position) {
        final AppItem item = dockApps.get(position);
        android.content.Context context = holder.itemView.getContext();
        String currentTheme = ThemeManager.getSavedTheme(context);

        if (cellWidth > 0) {
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            params.width = cellWidth;
            holder.itemView.setLayoutParams(params);
        }

        holder.imgIcon.setImageDrawable(IconPackManager.getIcon(context, item.packageName, item.icon));

        // Notification Badge
        if (notificationCounts.containsKey(item.packageName)) {
            int count = notificationCounts.get(item.packageName);
            if (count > 0) {
                holder.notificationBadge.setVisibility(View.VISIBLE);
            } else {
                holder.notificationBadge.setVisibility(View.GONE);
            }
        } else {
            holder.notificationBadge.setVisibility(View.GONE);
        }

        // Apply dynamic squircle background
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);

        if (iconSize > 0) {
            ViewGroup.LayoutParams params = holder.iconContainer.getLayoutParams();
            params.width = iconSize;
            params.height = iconSize;
            holder.iconContainer.setLayoutParams(params);
        }

        // Theme Adaptation
        if (ThemeManager.THEME_AMOLED.equals(currentTheme)) {
            // SHMO: Black background, Neon Green text/icons/border
            background.setColor(context.getColor(R.color.shmo_squircle_bg));
            background.setStroke(1, context.getColor(R.color.shmo_neon_lime));
            holder.imgIcon.clearColorFilter(); // Do not tint original app icons
        } else if (ThemeManager.THEME_GLASS.equals(currentTheme)) {
            // Liquid Glass: Glassy background
            background = ThemeManager.getLiquidGlassDrawable();
            holder.imgIcon.clearColorFilter();
        } else if (ThemeManager.THEME_LIGHT.equals(currentTheme)) {
            // Light Mode: White background
            background.setColor(android.graphics.Color.WHITE);
            holder.imgIcon.clearColorFilter();
        } else {
            // Default
            background.setColor(android.graphics.Color.parseColor("#888888"));
            holder.imgIcon.clearColorFilter();
        }

        // 60dp is the size in item_dock.xml
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        float sizePx = (iconSize > 0) ? iconSize : (60 * density);
        float radiusPx = (sizePx / 2f) * (iconRadiusPercent / 100f);
        background.setCornerRadius(radiusPx);

        holder.iconContainer.setBackground(background);

        // Adaptive Icon Handling
        // If icon is NOT adaptive (legacy square), increase padding to make it look
        // like a foreground
        int paddingDp = 5; // Default padding from XML
        
        String currentIconPack = IconPackManager.getSavedIconPack(context);
        if (IconPackManager.PACK_AFRIQUI.equals(currentIconPack)) {
            // Force small padding for Afriqui theme to ensure icons are large enough
            paddingDp = 4;
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!(item.icon instanceof android.graphics.drawable.AdaptiveIconDrawable)) {
                paddingDp = 12; // Increase padding for legacy icons
            }
        } else {
            // Pre-Oreo, assume all icons are legacy/bitmap
            paddingDp = 12;
        }
        int paddingPx = (int) (paddingDp * density);
        holder.iconContainer.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Notification Animation (Zoom In/Out)
        boolean hasNotificationsDock = false;
        if (notificationCounts.containsKey(item.packageName)) {
            Integer count = notificationCounts.get(item.packageName);
            hasNotificationsDock = count != null && count > 0;
        }

        if (hasNotificationsDock) {
            if (holder.animator == null) {
                holder.animator = ObjectAnimator.ofPropertyValuesHolder(
                        holder.iconContainer,
                        PropertyValuesHolder.ofFloat("scaleX", 1f, 0.9f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f, 0.9f)
                );
                holder.animator.setDuration(800);
                holder.animator.setRepeatCount(ValueAnimator.INFINITE);
                holder.animator.setRepeatMode(ValueAnimator.REVERSE);
                holder.animator.start();
            } else if (!holder.animator.isRunning()) {
                holder.animator.start();
            }
        } else {
            if (holder.animator != null) {
                holder.animator.cancel();
                holder.animator = null;
                holder.iconContainer.setScaleX(1f);
                holder.iconContainer.setScaleY(1f);
            } else {
                holder.iconContainer.setScaleX(1f);
                holder.iconContainer.setScaleY(1f);
            }
        }

        if (isDragEnabled) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnTouchListener(null);
            holder.itemView.setOnLongClickListener(v -> {
                android.content.ClipData.Item clipItem = new android.content.ClipData.Item(item.packageName != null ? item.packageName : "dock_item");
                android.content.ClipData dragData = new android.content.ClipData(item.label, new String[]{android.content.ClipDescription.MIMETYPE_TEXT_PLAIN}, clipItem);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(holder.itemView);
                v.startDragAndDrop(dragData, myShadow, item, 0);
                return true;
            });
        } else {
            // Normal Mode: Click to Open, Long Click shows Menu
            holder.itemView.setOnTouchListener(null);
            
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < dockApps.size()) {
                    if (listener != null) {
                        listener.onDockClick(dockApps.get(pos));
                    }
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < dockApps.size()) {
                    if (longClickListener != null) {
                        longClickListener.onAppLongClick(dockApps.get(pos), v);
                    }
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return dockApps.size();
    }

    public static class DockViewHolder extends RecyclerView.ViewHolder {
        android.widget.FrameLayout iconContainer;
        ImageView imgIcon;
        View notificationBadge;
        ObjectAnimator animator;

        public DockViewHolder(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            notificationBadge = itemView.findViewById(R.id.notificationBadge);
        }
    }
}

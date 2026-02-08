package com.vulsoft.vulsoftos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

public class AppsListAdapter extends RecyclerView.Adapter<AppsListAdapter.ViewHolder> {

    private List<AppItem> apps;
    private List<AppItem> allApps;
    private final AppsAdapter.OnAppClickListener clickListener;
    private final AppsAdapter.OnAppLongClickListener longClickListener;
    private String currentCategory = "Tout";
    private int iconSize = -1;
    private float textSize = -1;
    private int iconRadiusPercent = 50;

    public AppsListAdapter(List<AppItem> apps, AppsAdapter.OnAppClickListener clickListener, AppsAdapter.OnAppLongClickListener longClickListener) {
        this.allApps = new ArrayList<>(apps);
        this.apps = new ArrayList<>(apps);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        sortApps();
    }

    public void setIconRadiusPercent(int percent) {
        this.iconRadiusPercent = percent;
        notifyDataSetChanged();
    }

    public void setAppIconSize(int size) {
        this.iconSize = size;
        notifyDataSetChanged();
    }

    public void setAppTextSize(float size) {
        this.textSize = size;
        notifyDataSetChanged();
    }

    public void updateApps(List<AppItem> newApps) {
        this.allApps = new ArrayList<>(newApps);
        filterApps();
    }
    
    public void setCategoryFilter(String category) {
        this.currentCategory = category;
        filterApps();
    }
    
    private void filterApps() {
        if (currentCategory == null || currentCategory.equals("Tout")) {
            apps = new ArrayList<>(allApps);
        } else {
            apps = new ArrayList<>();
            for (AppItem app : allApps) {
                if (currentCategory.equals(app.category)) {
                    apps.add(app);
                }
            }
        }
        sortApps();
        notifyDataSetChanged();
    }

    private void sortApps() {
        Collections.sort(apps, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem o1, AppItem o2) {
                return o1.label.toString().compareToIgnoreCase(o2.label.toString());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use app_item layout for vertical grid look
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem app = apps.get(position);
        android.content.Context context = holder.itemView.getContext();
        String currentTheme = ThemeManager.getSavedTheme(context);

        holder.label.setText(app.label);
        holder.label.setVisibility(View.VISIBLE); // Ensure label is visible
        
        if (textSize > 0) {
            holder.label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        holder.icon.setImageDrawable(IconPackManager.getIcon(context, app.packageName, app.icon));
        
        if (iconSize > 0) {
            ViewGroup.LayoutParams params = holder.icon.getLayoutParams();
            params.width = iconSize;
            params.height = iconSize;
            holder.icon.setLayoutParams(params);
        }

        // Apply dynamic squircle background
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);

        // Theme Adaptation
        if (ThemeManager.THEME_AMOLED.equals(currentTheme)) {
            // SHMO: Black background, Neon Green text/icons/border
            background.setColor(context.getColor(R.color.shmo_squircle_bg));
            background.setStroke(1, context.getColor(R.color.shmo_neon_lime));
            holder.label.setTextColor(context.getColor(R.color.shmo_neon_lime));
            holder.icon.clearColorFilter(); 
        } else if (ThemeManager.THEME_GLASS.equals(currentTheme)) {
            // Liquid Glass: Glassy background, White text
            background = ThemeManager.getLiquidGlassDrawable();
            holder.label.setTextColor(android.graphics.Color.WHITE);
            holder.icon.clearColorFilter();
        } else if (ThemeManager.THEME_LIGHT.equals(currentTheme)) {
            // Light Mode: White background, Black text
            background.setColor(android.graphics.Color.WHITE);
            holder.label.setTextColor(android.graphics.Color.BLACK);
            holder.icon.clearColorFilter();
        } else {
            // Default
            background.setColor(android.graphics.Color.parseColor("#888888"));
            holder.label.setTextColor(android.graphics.Color.WHITE);
            holder.icon.clearColorFilter();
        }

        float density = holder.itemView.getResources().getDisplayMetrics().density;
        // 60dp is standard size in app_item.xml, but we use iconSize if set or fallback
        float sizePx = (iconSize > 0) ? iconSize : (60 * density);
        float radiusPx = (sizePx / 2f) * (iconRadiusPercent / 100f);
        background.setCornerRadius(radiusPx);

        holder.icon.setBackground(background);

        // Adaptive Icon Handling
        int paddingDp = 4; // Default padding
        
        String currentIconPack = IconPackManager.getSavedIconPack(context);
        if (IconPackManager.PACK_AFRIQUI.equals(currentIconPack)) {
            paddingDp = 4;
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!(app.icon instanceof android.graphics.drawable.AdaptiveIconDrawable)) {
                paddingDp = 12; // Increase padding for legacy icons
            }
        } else {
            paddingDp = 12;
        }
        int paddingPx = (int) (paddingDp * density);
        holder.icon.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        holder.itemView.setOnClickListener(v -> clickListener.onAppClick(app));
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onAppLongClick(app, v);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            label = itemView.findViewById(R.id.label);
        }
    }
}

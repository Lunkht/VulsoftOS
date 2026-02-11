package com.vulsoft.vulsoftos;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppItem appItem);
    }

    public interface OnAppLongClickListener {
        void onAppLongClick(AppItem appItem, View view);
    }

    private static final int VIEW_TYPE_APP = 0;
    private static final int VIEW_TYPE_WIDGET = 1;
    private static final int VIEW_TYPE_FOLDER = 2;

    private final List<AppItem> apps;
    private final OnAppClickListener clickListener;
    private final OnAppLongClickListener longClickListener;
    private final int iconRadiusPercent;
    private java.util.Map<String, Integer> notificationCounts = new java.util.HashMap<>();
    private androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper;
    private boolean isDragEnabled = false;
    private int iconSize = -1;
    private float textSize = -1;
    private AppWidgetHost appWidgetHost;
    private boolean showLabels = true;
    private boolean twoLineTitles = false;

    public AppsAdapter(List<AppItem> apps, int iconRadiusPercent, OnAppClickListener clickListener,
            OnAppLongClickListener longClickListener) {
        this.apps = apps;
        this.iconRadiusPercent = iconRadiusPercent;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setShowLabels(boolean show) {
        this.showLabels = show;
        notifyDataSetChanged();
    }

    public void setTwoLineTitles(boolean twoLines) {
        this.twoLineTitles = twoLines;
        notifyDataSetChanged();
    }

    public void setAppWidgetHost(AppWidgetHost host) {
        this.appWidgetHost = host;
    }

    public void setItemTouchHelper(androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper) {
        this.itemTouchHelper = itemTouchHelper;
    }

    public void setDragEnabled(boolean enabled) {
        this.isDragEnabled = enabled;
        notifyDataSetChanged();
    }

    public void setAppIconSize(int sizePx) {
        this.iconSize = sizePx;
        notifyDataSetChanged();
    }

    public void setAppTextSize(float sizePx) {
        this.textSize = sizePx;
        notifyDataSetChanged();
    }

    public void updateData(List<AppItem> newApps) {
        this.apps.clear();
        this.apps.addAll(newApps);
        notifyDataSetChanged();
    }

    public void updateNotificationCounts(java.util.Map<String, Integer> counts) {
        this.notificationCounts = counts;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (apps.get(position).type == AppItem.Type.WIDGET) {
            return VIEW_TYPE_WIDGET;
        } else if (apps.get(position).type == AppItem.Type.FOLDER) {
            return VIEW_TYPE_FOLDER;
        }
        return VIEW_TYPE_APP;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_WIDGET) {
             FrameLayout container = new FrameLayout(parent.getContext());
             container.setLayoutParams(new ViewGroup.LayoutParams(
                     ViewGroup.LayoutParams.MATCH_PARENT, 
                     ViewGroup.LayoutParams.WRAP_CONTENT));
             return new AppViewHolder(container, true, false);
        } else if (viewType == VIEW_TYPE_FOLDER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.folder_item, parent, false);
            return new AppViewHolder(view, false, true);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.app_item, parent, false);
        return new AppViewHolder(view, false, false);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        final AppItem item = apps.get(position);
        android.util.Log.d("RuvoluteDebug", "AppsAdapter: Binding pos=" + position + " Label=" + item.label);
        int type = getItemViewType(position);
        if (type == VIEW_TYPE_WIDGET) {
            bindWidget(holder, item);
        } else if (type == VIEW_TYPE_FOLDER) {
            bindFolder(holder, item);
        } else {
            bindApp(holder, item);
        }
    }

    private void bindFolder(AppViewHolder holder, AppItem item) {
        android.content.Context context = holder.itemView.getContext();
        holder.label.setText(item.label);
        holder.label.setVisibility(showLabels ? View.VISIBLE : View.GONE);
        holder.label.setMaxLines(twoLineTitles ? 2 : 1);
        holder.label.setSingleLine(!twoLineTitles);
        
        if (textSize > 0) {
            holder.label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        // Background for folder container
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        background.setColor(android.graphics.Color.parseColor("#44888888")); // Semi-transparent grey
        
        float density = context.getResources().getDisplayMetrics().density;
        float sizePx = 60 * density;
        float radiusPx = (sizePx / 2f) * (iconRadiusPercent / 100f);
        background.setCornerRadius(radiusPx);
        
        holder.folderContainer.setBackground(background);
        
        // Populate mini icons
        for (int i = 0; i < 4; i++) {
            ImageView miniIcon = holder.miniIcons[i];
            if (miniIcon != null) {
                if (i < item.folderItems.size()) {
                    miniIcon.setImageDrawable(item.folderItems.get(i).icon);
                    miniIcon.setVisibility(View.VISIBLE);
                } else {
                    miniIcon.setImageDrawable(null);
                    miniIcon.setVisibility(View.INVISIBLE);
                }
            }
        }
        
        if (isDragEnabled) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(v -> {
                android.content.ClipData.Item clipItem = new android.content.ClipData.Item(item.packageName != null ? item.packageName : "folder");
                android.content.ClipData dragData = new android.content.ClipData(item.label, new String[]{android.content.ClipDescription.MIMETYPE_TEXT_PLAIN}, clipItem);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(holder.itemView);
                v.startDragAndDrop(dragData, myShadow, item, 0);
                return true;
            });
        } else {
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onAppClick(item);
                }
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onAppLongClick(item, v);
                }
                return true;
            });
        }
    }

    private void bindWidget(AppViewHolder holder, AppItem item) {
        if (appWidgetHost == null) return;
        
        FrameLayout container = (FrameLayout) holder.itemView;
        container.removeAllViews();
        
        AppWidgetHostView hostView = appWidgetHost.createView(container.getContext(), item.widgetId, null);
        hostView.setAppWidget(item.widgetId, null);
        
        float density = container.getResources().getDisplayMetrics().density;
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        container.addView(hostView, params);
        
        if (isDragEnabled) {
             hostView.setOnLongClickListener(v -> {
                android.content.ClipData.Item clipItem = new android.content.ClipData.Item("widget_" + item.widgetId);
                android.content.ClipData dragData = new android.content.ClipData(item.label, new String[]{android.content.ClipDescription.MIMETYPE_TEXT_PLAIN}, clipItem);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(holder.itemView);
                v.startDragAndDrop(dragData, myShadow, item, 0);
                return true;
            });
        } else {
            hostView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onAppLongClick(item, v);
                }
                return true;
            });
        }
    }

    private void bindApp(AppViewHolder holder, AppItem item) {
        android.content.Context context = holder.itemView.getContext();
        String currentTheme = ThemeManager.getSavedTheme(context);

        holder.label.setText(item.label);
        holder.label.setVisibility(showLabels ? View.VISIBLE : View.GONE);
        holder.label.setMaxLines(twoLineTitles ? 2 : 1);
        holder.label.setSingleLine(!twoLineTitles);
        holder.label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        if (textSize > 0) {
            holder.label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSize);
        }

        holder.icon.setImageDrawable(IconPackManager.getIcon(context, item.packageName, item.icon));
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
            holder.icon.clearColorFilter(); // Do not tint original app icons
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

        // 60dp is the size in app_item.xml
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        float sizePx = 60 * density;
        float radiusPx = (sizePx / 2f) * (iconRadiusPercent / 100f);
        background.setCornerRadius(radiusPx);

        holder.icon.setBackground(background);

        // Adaptive Icon Handling
        // If icon is NOT adaptive (legacy square), increase padding to make it look
        // like a foreground
        int paddingDp = 4; // Default padding
        
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
        holder.icon.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Notification Animation (Zoom In/Out)
        boolean hasNotifications = false;
        if (notificationCounts.containsKey(item.packageName)) {
            Integer count = notificationCounts.get(item.packageName);
            hasNotifications = count != null && count > 0;
        }

        if (hasNotifications) {
            if (holder.animator == null) {
                holder.animator = ObjectAnimator.ofPropertyValuesHolder(
                        holder.icon,
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
                holder.icon.setScaleX(1f);
                holder.icon.setScaleY(1f);
            } else {
                holder.icon.setScaleX(1f);
                holder.icon.setScaleY(1f);
            }
        }

        if (isDragEnabled) {
            // Edit Mode: Use native Drag & Drop to allow moving between Grid and Dock
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnTouchListener(null);
            holder.itemView.setOnLongClickListener(v -> {
                android.content.ClipData.Item clipItem = new android.content.ClipData.Item(item.packageName);
                android.content.ClipData dragData = new android.content.ClipData(item.label, new String[]{android.content.ClipDescription.MIMETYPE_TEXT_PLAIN}, clipItem);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(holder.itemView);
                v.startDragAndDrop(dragData, myShadow, item, 0);
                return true;
            });
        } else {
            // Normal Mode: Click to Open, Long Click shows Menu AND enables drag
            final float[] touchStart = new float[2];
            final boolean[] longPressTriggered = { false };
            final boolean[] dragStarted = { false };
            final int touchSlop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();

            holder.itemView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        touchStart[0] = event.getRawX();
                        touchStart[1] = event.getRawY();
                        longPressTriggered[0] = false;
                        dragStarted[0] = false;
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        // If long press was triggered and user moves, start drag
                        if (longPressTriggered[0] && !dragStarted[0]) {
                            float dx = Math.abs(event.getRawX() - touchStart[0]);
                            float dy = Math.abs(event.getRawY() - touchStart[1]);
                            if (dx > touchSlop || dy > touchSlop) {
                                dragStarted[0] = true;
                                if (itemTouchHelper != null) {
                                    itemTouchHelper.startDrag(holder);
                                }
                                return true;
                            }
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        longPressTriggered[0] = false;
                        dragStarted[0] = false;
                        break;
                }
                return false;
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d("RuvoluteDebug", "AppsAdapter: CLICK DETECTED");
                    android.util.Log.d("RuvoluteDebug", " - Label: " + item.label);
                    android.util.Log.d("RuvoluteDebug", " - Package: " + item.packageName);
                    android.util.Log.d("RuvoluteDebug", " - Class: " + item.className);
                    android.util.Log.d("RuvoluteDebug", " - Type: " + item.type);
                    android.util.Log.d("RuvoluteDebug", " - Intent: " + (item.launchIntent != null ? item.launchIntent.toString() : "NULL"));
                    
                    if (clickListener != null) {
                        clickListener.onAppClick(item);
                    }
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longPressTriggered[0] = true;
                    if (longClickListener != null) {
                        longClickListener.onAppLongClick(item, v);
                    }
                    // Return true to consume the event, but allow subsequent drag
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label;
        ObjectAnimator animator;
        
        // Folder specific
        FrameLayout folderContainer;
        ImageView[] miniIcons;

        public AppViewHolder(@NonNull View itemView, boolean isWidget, boolean isFolder) {
            super(itemView);
            if (!isWidget) {
                if (isFolder) {
                    label = itemView.findViewById(R.id.label);
                    folderContainer = itemView.findViewById(R.id.folder_icon_container);
                    miniIcons = new ImageView[4];
                    miniIcons[0] = itemView.findViewById(R.id.mini_icon_1);
                    miniIcons[1] = itemView.findViewById(R.id.mini_icon_2);
                    miniIcons[2] = itemView.findViewById(R.id.mini_icon_3);
                    miniIcons[3] = itemView.findViewById(R.id.mini_icon_4);
                } else {
                    icon = itemView.findViewById(R.id.icon);
                    label = itemView.findViewById(R.id.label);
                }
            }
        }
    }
}

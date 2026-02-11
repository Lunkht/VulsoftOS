package com.vulsoft.vulsoftos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.appwidget.AppWidgetHost;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppsPagerAdapter extends RecyclerView.Adapter<AppsPagerAdapter.PageViewHolder> {

    private final List<AppItem> allApps;
    private int pageSize;
    private int spanCount;
    private final int iconRadiusPercent;
    private final AppsAdapter.OnAppClickListener clickListener;
    private final AppsAdapter.OnAppLongClickListener longClickListener;
    private OnOrderChangedListener orderChangedListener;
    private java.util.Map<String, Integer> notificationCounts = new java.util.HashMap<>();
    private boolean showRecentsPage = true;
    private int iconSize = -1;
    private float textSize = -1;
    private boolean showLabels = true;
    private boolean twoLineTitles = false;
    private int verticalSpacing = 0;
    private AppWidgetHost appWidgetHost;
    private View.OnDragListener dragListener;
    private String currentCategory = "Tout"; // Default category
    
    public interface OnOrderChangedListener {
        void onOrderChanged();
    }
    
    public void setOnOrderChangedListener(OnOrderChangedListener listener) {
        this.orderChangedListener = listener;
    }
    
    public void setOnDragListener(View.OnDragListener listener) {
        this.dragListener = listener;
        notifyDataSetChanged();
    }

    private final List<List<AppItem>> pages = new ArrayList<>();
    
    public AppsPagerAdapter(List<AppItem> allApps, int pageSize, int spanCount, int iconRadiusPercent, AppsAdapter.OnAppClickListener clickListener, AppsAdapter.OnAppLongClickListener longClickListener) {
        this.allApps = allApps;
        this.pageSize = pageSize;
        this.spanCount = spanCount;
        this.iconRadiusPercent = iconRadiusPercent;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        recalculatePages();
    }
    
    public void setAppWidgetHost(AppWidgetHost host) {
        this.appWidgetHost = host;
    }
    
    public void updateGridSize(int newPageSize, int newSpanCount) {
        this.pageSize = newPageSize;
        this.spanCount = newSpanCount;
        recalculatePages();
        notifyDataSetChanged();
    }
    
    public void updateApps() {
        recalculatePages();
        notifyDataSetChanged();
    }
    
    public void setCategoryFilter(String category) {
        this.currentCategory = category;
        recalculatePages();
        notifyDataSetChanged();
    }
    
    private void recalculatePages() {
        pages.clear();
        if (allApps.isEmpty()) return;
        
        List<AppItem> filteredApps = new ArrayList<>();
        if ("Tout".equals(currentCategory) || currentCategory == null) {
            filteredApps.addAll(allApps);
        } else {
            for (AppItem app : allApps) {
                if (currentCategory.equals(app.category)) {
                    filteredApps.add(app);
                }
            }
        }
        
        List<AppItem> currentPage = new ArrayList<>();
        
        for (AppItem item : filteredApps) {
            if (currentPage.size() >= pageSize) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }
            currentPage.add(item);
        }
        
        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
    }

    public void setShowRecentsPage(boolean show) {
        this.showRecentsPage = show;
        notifyDataSetChanged();
    }

    public void updateNotificationCounts(java.util.Map<String, Integer> counts) {
        this.notificationCounts = counts;
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

    public void setShowLabels(boolean show) {
        this.showLabels = show;
        notifyDataSetChanged();
    }

    public void setTwoLineTitles(boolean twoLines) {
        this.twoLineTitles = twoLines;
        notifyDataSetChanged();
    }

    public void setVerticalSpacing(int spacingPx) {
        this.verticalSpacing = spacingPx;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.page_apps;
    }

    private boolean isDragEnabled = false;

    public void setDragEnabled(boolean enabled) {
        this.isDragEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<AppItem> pageApps;
        
        // Normal Apps Page
        if (position >= 0 && position < pages.size()) {
            pageApps = new ArrayList<>(pages.get(position));
        } else {
            pageApps = new ArrayList<>();
        }
        
        // Ensure Recycler is visible for normal pages
        holder.recyclerView.setVisibility(View.VISIBLE);
        if (holder.textEmpty != null) holder.textEmpty.setVisibility(View.GONE);
        if (holder.layoutPermission != null) holder.layoutPermission.setVisibility(View.GONE);

        // Optimization: Reuse LayoutManager and Adapter if already set
        GridLayoutManager layoutManager;
        if (holder.recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            layoutManager = (GridLayoutManager) holder.recyclerView.getLayoutManager();
            if (layoutManager.getSpanCount() != spanCount) {
                layoutManager.setSpanCount(spanCount);
            }
        } else {
            layoutManager = new GridLayoutManager(holder.recyclerView.getContext(), spanCount);
            holder.recyclerView.setLayoutManager(layoutManager);
        }

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < 0 || position >= pageApps.size()) return 1;
                AppItem item = pageApps.get(position);
                if (item.type == AppItem.Type.WIDGET) {
                    return Math.min(item.spanX, spanCount);
                }
                return 1;
            }
        });
        
        AppsAdapter adapter;
        if (holder.recyclerView.getAdapter() instanceof AppsAdapter) {
            adapter = (AppsAdapter) holder.recyclerView.getAdapter();
            adapter.updateData(pageApps);
        } else {
            adapter = new AppsAdapter(pageApps, iconRadiusPercent, clickListener, longClickListener);
            holder.recyclerView.setAdapter(adapter);
        }

        adapter.setAppWidgetHost(appWidgetHost);
        if (iconSize > 0) adapter.setAppIconSize(iconSize);
        if (textSize > 0) adapter.setAppTextSize(textSize);
        adapter.setShowLabels(showLabels);
        adapter.setTwoLineTitles(twoLineTitles);
        adapter.updateNotificationCounts(notificationCounts);
        adapter.setDragEnabled(isDragEnabled);
        
        // Vertical Spacing
        // Remove existing decorations to avoid duplicates
        while (holder.recyclerView.getItemDecorationCount() > 0) {
            holder.recyclerView.removeItemDecorationAt(0);
        }
        if (verticalSpacing > 0) {
            holder.recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    outRect.bottom = verticalSpacing;
                }
            });
        }
        
        if (dragListener != null) {
            holder.recyclerView.setOnDragListener(dragListener);
        }

        // Only enable drag & drop for normal app pages
        final int currentPageIndex = position;
        
        // ItemTouchHelper should only be attached once per RecyclerView
        if (holder.recyclerView.getTag(R.id.tag_touch_helper) == null) {
            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    0
            ) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                }
                
                @Override
                public boolean isLongPressDragEnabled() {
                    return false;
                }
            };
            
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(holder.recyclerView);
            holder.recyclerView.setTag(R.id.tag_touch_helper, itemTouchHelper);
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final RecyclerView recyclerView;
        final android.widget.TextView textEmpty;
        final android.view.View layoutPermission;
        final android.view.View btnGrantPermission;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.recyclerPageApps);
            textEmpty = itemView.findViewById(R.id.textEmpty);
            layoutPermission = itemView.findViewById(R.id.layoutPermission);
            btnGrantPermission = itemView.findViewById(R.id.btnGrantPermission);
        }
    }
}

package com.vulsoft.vulsoftos;

import android.app.Dialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.app.ActivityManager;
import android.widget.ImageView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RecentAppsDialogFragment extends BottomSheetDialogFragment {

    private RecyclerView recyclerRecentApps;
    private EditText searchRecentApps;
    private ImageView btnCloseAll;
    private AppsAdapter adapter;
    private final List<AppItem> recentApps = new ArrayList<>();
    private final List<AppItem> allRecentApps = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_recent_apps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerRecentApps = view.findViewById(R.id.recyclerRecentApps);
        recyclerRecentApps.setLayoutManager(new GridLayoutManager(getContext(), 4));

        btnCloseAll = view.findViewById(R.id.btnCloseAll);
        if (btnCloseAll != null) {
            btnCloseAll.setOnClickListener(v -> closeAllRecentApps());
        }

        searchRecentApps = view.findViewById(R.id.searchRecentApps);
        searchRecentApps.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadRecentApps();
    }

    private void filterApps(String query) {
        recentApps.clear();
        if (query == null || query.isEmpty()) {
            recentApps.addAll(allRecentApps);
        } else {
            String lowerQuery = query.toLowerCase();
            for (AppItem app : allRecentApps) {
                if (app.label != null && app.label.toLowerCase().contains(lowerQuery)) {
                    recentApps.add(app);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // Background transparent pour le dialogue
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                // Effet de flou pour Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.setBlurBehindRadius(60); // Radius de flou
                    window.setAttributes(params);
                }
            }

            // Rendre le container de la bottom sheet transparent pour voir notre background custom
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            }
            
            // Haptic feedback on open
            vibrate();
        }
    }

    private void vibrate() {
        Context context = getContext();
        if (context == null) return;

        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(50);
            }
        }
    }

    private void closeAllRecentApps() {
        Context context = getContext();
        if (context == null) return;

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        // Try to kill background processes
        for (AppItem app : allRecentApps) {
            if (app.packageName != null) {
                try {
                    am.killBackgroundProcesses(app.packageName);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        
        // Save clear timestamp to persist the "cleared" state
        android.content.SharedPreferences prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_recents_clear_time", System.currentTimeMillis()).apply();

        // Clear local lists
        allRecentApps.clear();
        recentApps.clear();
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        Toast.makeText(context, "Applications récentes effacées", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void loadRecentApps() {
        new Thread(() -> {
            Context context = getContext();
            if (context == null) return;

            // Get last clear time
            long lastClearTime = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                    .getLong("last_recents_clear_time", 0);

            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 3600 * 24, time);

            if (stats == null || stats.isEmpty()) {
                return;
            }

            PackageManager pm = context.getPackageManager();
            Map<Long, UsageStats> sortedStats = new TreeMap<>(Collections.reverseOrder());

            for (UsageStats usageStats : stats) {
                // Filter out apps used BEFORE the last clear time
                if (usageStats.getLastTimeUsed() > lastClearTime) {
                    sortedStats.put(usageStats.getLastTimeUsed(), usageStats);
                }
            }

            List<AppItem> loadedApps = new ArrayList<>();
            int count = 0;
            String myPackage = context.getPackageName();

            for (UsageStats usageStats : sortedStats.values()) {
                if (count >= 15) break; // Limit to 15 recent apps

                String packageName = usageStats.getPackageName();
                
                // Skip our own launcher
                if (packageName.equals(myPackage)) continue;

                // Check if it's a launchable app
                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent == null) continue;

                try {
                    AppItem item = new AppItem();
                    item.packageName = packageName;
                    item.launchIntent = launchIntent;
                    item.label = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                    item.icon = pm.getApplicationIcon(packageName);
                    
                    // Avoid duplicates (UsageStats might return multiple entries for same package sometimes if not aggregated correctly, 
                    // though queryUsageStats usually aggregates by interval. But map logic above uses timestamp keys, so same package 
                    // appearing at different times is possible? No, queryUsageStats returns list of aggregates per package usually, 
                    // but let's double check unique packages.)
                    boolean exists = false;
                    for (AppItem existing : loadedApps) {
                        if (existing.packageName.equals(packageName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        loadedApps.add(item);
                        count++;
                    }
                    
                } catch (PackageManager.NameNotFoundException e) {
                    // Skip
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allRecentApps.clear();
                    allRecentApps.addAll(loadedApps);
                    
                    filterApps(searchRecentApps.getText().toString());
                    
                    // Reuse AppsAdapter logic
                    int iconRadius = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                            .getInt("icon_corner_radius", 50);

                    adapter = new AppsAdapter(recentApps, iconRadius, appItem -> {
                        try {
                            startActivity(appItem.launchIntent);
                            dismiss();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Impossible d'ouvrir l'application", Toast.LENGTH_SHORT).show();
                        }
                    }, null); // No long click in recents
                    
                    recyclerRecentApps.setAdapter(adapter);
                });
            }
        }).start();
    }
}

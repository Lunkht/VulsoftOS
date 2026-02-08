package com.vulsoft.vulsoftos;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RecentAppsHelper {

    public static List<AppItem> getRecentApps(Context context, PackageManager pm) {
        List<AppItem> recentApps = new ArrayList<>();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        
        if (usm == null) return recentApps;

        long time = System.currentTimeMillis();
        // Query last 24 hours
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);

        if (appList != null && !appList.isEmpty()) {
            TreeMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }

            // Get apps from the sorted map (descending order of time)
            int count = 0;
            for (Map.Entry<Long, UsageStats> entry : sortedMap.descendingMap().entrySet()) {
                if (count >= 15) break; // Limit to 15 recent apps

                UsageStats stats = entry.getValue();
                String packageName = stats.getPackageName();

                // Filter out system UI, launcher itself, etc if needed
                if (packageName.equals(context.getPackageName())) continue;

                Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    AppItem item = new AppItem();
                    item.label = getAppLabel(pm, packageName);
                    item.packageName = packageName;
                    item.icon = getAppIcon(pm, packageName);
                    item.launchIntent = launchIntent;
                    
                    // Check if already added (UsageStats can have duplicates for different intervals)
                    boolean exists = false;
                    for (AppItem existing : recentApps) {
                        if (existing.packageName.equals(packageName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        recentApps.add(item);
                        count++;
                    }
                }
            }
        }
        return recentApps;
    }

    private static String getAppLabel(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private static android.graphics.drawable.Drawable getAppIcon(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
    
    public static boolean hasUsageStatsPermission(Context context) {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, 
                android.os.Process.myUid(), context.getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }
}

package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class RecentAppsManager {

    private static final String PREFS_NAME = "recent_apps_prefs";
    private static final String KEY_RECENT_APPS = "recent_apps_list";
    private static final int MAX_RECENT_APPS = 15;

    public static void addApp(Context context, String packageName) {
        if (packageName == null || packageName.equals("com.valkunt.os"))
            return;

        List<String> recentApps = getRecentApps(context);

        // Remove if already exists to move it to top
        recentApps.remove(packageName);

        // Add to top
        recentApps.add(0, packageName);

        // Trim size
        if (recentApps.size() > MAX_RECENT_APPS) {
            recentApps = recentApps.subList(0, MAX_RECENT_APPS);
        }

        saveRecentApps(context, recentApps);
    }

    public static List<String> getRecentApps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_RECENT_APPS, "[]");
        List<String> apps = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                apps.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return apps;
    }

    private static void saveRecentApps(Context context, List<String> apps) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray array = new JSONArray();
        for (String app : apps) {
            array.put(app);
        }
        prefs.edit().putString(KEY_RECENT_APPS, array.toString()).apply();
    }
}

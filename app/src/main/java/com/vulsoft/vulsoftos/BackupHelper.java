package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BackupHelper {

    private static JSONObject getPrefsJson(Context context, String prefName) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        JSONObject json = new JSONObject();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Set) {
                json.put(entry.getKey(), new JSONArray((Set<?>) v));
            } else {
                json.put(entry.getKey(), v);
            }
        }
        return json;
    }

    private static void restorePrefsJson(Context context, String prefName, JSONObject json) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Clear existing settings before restore

        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Double) {
                editor.putFloat(key, ((Double) value).floatValue());
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof JSONArray) {
                JSONArray array = (JSONArray) value;
                Set<String> set = new HashSet<>();
                for (int i = 0; i < array.length(); i++) {
                    set.add(array.getString(i));
                }
                editor.putStringSet(key, set);
            }
        }
        editor.apply();
    }

    public static void backupSettings(Context context, Uri uri) throws Exception {
        JSONObject root = new JSONObject();
        root.put("launcher_prefs", getPrefsJson(context, "launcher_prefs"));
        root.put("wallpaper_prefs", getPrefsJson(context, "wallpaper_prefs"));
        root.put("theme_prefs", getPrefsJson(context, "theme_prefs"));
        
        // Backup App Layout (Grid/Dock)
        java.io.File layoutFile = new java.io.File(context.getFilesDir(), "app_layout.json");
        if (layoutFile.exists()) {
             try (BufferedReader reader = new BufferedReader(new java.io.FileReader(layoutFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                if (sb.length() > 0) {
                    root.put("app_layout", new JSONObject(sb.toString()));
                }
             }
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(root.toString().getBytes());
            }
        }
    }

    public static void restoreSettings(Context context, Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        JSONObject root = new JSONObject(sb.toString());

        // Check if it's the new format (contains keys for prefs files)
        if (root.has("launcher_prefs")) {
            restorePrefsJson(context, "launcher_prefs", root.getJSONObject("launcher_prefs"));
            if (root.has("wallpaper_prefs")) {
                restorePrefsJson(context, "wallpaper_prefs", root.getJSONObject("wallpaper_prefs"));
            }
            if (root.has("theme_prefs")) {
                restorePrefsJson(context, "theme_prefs", root.getJSONObject("theme_prefs"));
            }
            
            // Restore App Layout
            if (root.has("app_layout")) {
                JSONObject layoutJson = root.getJSONObject("app_layout");
                java.io.File layoutFile = new java.io.File(context.getFilesDir(), "app_layout.json");
                try (java.io.FileWriter writer = new java.io.FileWriter(layoutFile)) {
                    writer.write(layoutJson.toString());
                }
            }
            
        } else {
            // Fallback for legacy backups (flat launcher_prefs)
            restorePrefsJson(context, "launcher_prefs", root);
        }
    }
}

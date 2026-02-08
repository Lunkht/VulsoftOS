package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WidgetManager {

    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_WIDGET_CONFIG = "widget_config";
    private static final String KEY_ACTIVE_WIDGETS = "active_widgets";

    public enum WidgetType {
        TIME_DATE("time_date", R.layout.layout_widget_at_a_glance),
        SEARCH_BAR("search_bar", R.layout.layout_widget_search_bar);

        private final String id;
        private final int layoutRes;

        WidgetType(String id, int layoutRes) {
            this.id = id;
            this.layoutRes = layoutRes;
        }

        public String getId() {
            return id;
        }

        public int getLayoutRes() {
            return layoutRes;
        }

        public static WidgetType fromId(String id) {
            for (WidgetType type : values()) {
                if (type.id.equals(id))
                    return type;
            }
            return null;
        }
    }

    public static List<WidgetType> getActiveWidgets(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_ACTIVE_WIDGETS, null);
        List<WidgetType> widgets = new ArrayList<>();

        if (saved == null) {
            // Default widgets
            widgets.add(WidgetType.SEARCH_BAR);
            return widgets;
        }

        try {
            org.json.JSONArray array = new org.json.JSONArray(saved);
            for (int i = 0; i < array.length(); i++) {
                WidgetType type = WidgetType.fromId(array.getString(i));
                if (type != null)
                    widgets.add(type);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return widgets;
    }

    public static void saveActiveWidgets(Context context, List<WidgetType> widgets) {
        org.json.JSONArray array = new org.json.JSONArray();
        for (WidgetType type : widgets) {
            array.put(type.getId());
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACTIVE_WIDGETS, array.toString()).apply();
    }

    public static class WidgetConfig {
        public boolean visible = true;
        public float x = 0;
        public float y = 0;
        public int width = -1; // -1 means wrap_content
        public int height = -1;
        public float textSize = 16f;

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("visible", visible);
            json.put("x", x);
            json.put("y", y);
            json.put("width", width);
            json.put("height", height);
            json.put("textSize", textSize);
            return json;
        }

        public static WidgetConfig fromJson(JSONObject json) throws JSONException {
            WidgetConfig config = new WidgetConfig();
            config.visible = json.optBoolean("visible", true);
            config.x = (float) json.optDouble("x", 0);
            config.y = (float) json.optDouble("y", 0);
            config.width = json.optInt("width", -1);
            config.height = json.optInt("height", -1);
            config.textSize = (float) json.optDouble("textSize", 16f);
            return config;
        }
    }

    public static void saveWidgetConfig(Context context, String widgetId, WidgetConfig config) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String allConfigsJson = prefs.getString(KEY_WIDGET_CONFIG, "{}");
            JSONObject allConfigs = new JSONObject(allConfigsJson);
            allConfigs.put(widgetId, config.toJson());
            prefs.edit().putString(KEY_WIDGET_CONFIG, allConfigs.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static WidgetConfig getWidgetConfig(Context context, String widgetId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String allConfigsJson = prefs.getString(KEY_WIDGET_CONFIG, "{}");
            JSONObject allConfigs = new JSONObject(allConfigsJson);
            if (allConfigs.has(widgetId)) {
                return WidgetConfig.fromJson(allConfigs.getJSONObject(widgetId));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new WidgetConfig(); // Return default config
    }

    public static boolean isEditModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("edit_mode", false);
    }

    public static void setEditMode(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("edit_mode", enabled).apply();
    }
}

package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

public class WidgetCustomizationManager {
    private static final String PREFS_NAME = "widget_customization_prefs";
    private static final String KEY_WIDGET_TEXT_COLOR = "widget_text_color";
    private static final String KEY_TIME_WIDGET_X = "time_widget_x";
    private static final String KEY_TIME_WIDGET_Y = "time_widget_y";
    private static final String KEY_DATE_WIDGET_X = "date_widget_x";
    private static final String KEY_DATE_WIDGET_Y = "date_widget_y";
    private static final String KEY_TIME_WIDGET_SIZE = "time_widget_size";
    private static final String KEY_DATE_WIDGET_SIZE = "date_widget_size";

    public static void saveWidgetTextColor(Context context, String color) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WIDGET_TEXT_COLOR, color).apply();
    }

    public static String getWidgetTextColor(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WIDGET_TEXT_COLOR, "#FFFFFF");
    }

    public static void saveWidgetPosition(Context context, String widgetType, float x, float y) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyX = widgetType.equals("time") ? KEY_TIME_WIDGET_X : KEY_DATE_WIDGET_X;
        String keyY = widgetType.equals("time") ? KEY_TIME_WIDGET_Y : KEY_DATE_WIDGET_Y;
        prefs.edit()
                .putFloat(keyX, x)
                .putFloat(keyY, y)
                .apply();
    }

    public static float[] getWidgetPosition(Context context, String widgetType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String keyX = widgetType.equals("time") ? KEY_TIME_WIDGET_X : KEY_DATE_WIDGET_X;
        String keyY = widgetType.equals("time") ? KEY_TIME_WIDGET_Y : KEY_DATE_WIDGET_Y;

        float defaultX = widgetType.equals("time") ? 0.5f : 0.5f; // Center by default
        float defaultY = widgetType.equals("time") ? 0.3f : 0.7f; // Time at 30%, Date at 70%

        float x = prefs.getFloat(keyX, defaultX);
        float y = prefs.getFloat(keyY, defaultY);
        return new float[] { x, y };
    }

    public static void saveWidgetSize(Context context, String widgetType, float size) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = widgetType.equals("time") ? KEY_TIME_WIDGET_SIZE : KEY_DATE_WIDGET_SIZE;
        prefs.edit().putFloat(key, size).apply();
    }

    public static float getWidgetSize(Context context, String widgetType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = widgetType.equals("time") ? KEY_TIME_WIDGET_SIZE : KEY_DATE_WIDGET_SIZE;
        return prefs.getFloat(key, 1.0f); // Default size multiplier
    }

    public static void applyWidgetTextColor(Context context, TextView textView) {
        String colorString = getWidgetTextColor(context);
        int color;

        switch (colorString) {
            case "#FFFFFF":
                color = Color.WHITE;
                break;
            case "#000000":
                color = Color.BLACK;
                break;
            case "red":
                color = context.getResources().getColor(R.color.widget_red, null);
                break;
            case "green":
                color = context.getResources().getColor(R.color.widget_green, null);
                break;
            case "orange":
                color = context.getResources().getColor(R.color.widget_orange, null);
                break;
            case "system":
                int extracted = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                        .getInt("extracted_vibrant", Color.WHITE);
                color = extracted;
                break;
            case "theme":
            default:
                // Use theme color
                color = Color.WHITE;
                break;
        }

        textView.setTextColor(color);
    }

    public static void applyWidgetPosition(Context context, View widget, String widgetType) {
        float[] position = getWidgetPosition(context, widgetType);

        // Convert relative position to actual position when parent is measured
        widget.post(() -> {
            View parent = (View) widget.getParent();
            if (parent != null) {
                float x = position[0] * parent.getWidth() - widget.getWidth() / 2f;
                float y = position[1] * parent.getHeight() - widget.getHeight() / 2f;

                widget.setX(Math.max(0, Math.min(x, parent.getWidth() - widget.getWidth())));
                widget.setY(Math.max(0, Math.min(y, parent.getHeight() - widget.getHeight())));
            }
        });
    }

    public static void applyWidgetSize(Context context, TextView textView, String widgetType) {
        float sizeMultiplier = getWidgetSize(context, widgetType);
        float baseSize = widgetType.equals("time") ? 108f : 18f;
        textView.setTextSize(baseSize * sizeMultiplier);
    }
}
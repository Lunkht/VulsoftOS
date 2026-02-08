package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import com.vulsoft.vulsoftos.MainActivity;

public class ThemeManager {
    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME = "selected_theme";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_AMOLED = "amoled";
    public static final String THEME_SYSTEM = "system";
    public static final String THEME_GLASS = "glass";

    public static void saveTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme).apply();
    }

    public static String getSavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_DARK); // Dark devient le thème par défaut
    }

    public static void applyTheme(Context context) {
        String theme = getSavedTheme(context);
        boolean isLauncher = context instanceof MainActivity;

        switch (theme) {
            case THEME_LIGHT:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_Light : R.style.Theme_Ruvolute_Light);
                break;
            case THEME_DARK:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_Dark : R.style.Theme_Ruvolute_Dark);
                break;
            case THEME_AMOLED:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_SHMO : R.style.Theme_Ruvolute_SHMO);
                break;
            case THEME_SYSTEM:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_DayNight : R.style.Theme_Ruvolute_DayNight);
                break;
            case THEME_GLASS:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_LiquidGlass : R.style.Theme_Ruvolute_LiquidGlass);
                break;
            default:
                context.setTheme(isLauncher ? R.style.Theme_Ruvolute_Launcher_Dark : R.style.Theme_Ruvolute_Dark);
                break;
        }
    }

    public static android.graphics.drawable.GradientDrawable getLiquidGlassDrawable() {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
        drawable.setColors(new int[] {
            android.graphics.Color.argb(80, 255, 255, 255), // Top-Left Highlight
            android.graphics.Color.argb(30, 255, 255, 255)  // Bottom-Right Shadow/Translucent
        });
        drawable.setStroke(2, android.graphics.Color.argb(100, 255, 255, 255)); // Semi-transparent white border
        return drawable;
    }
}

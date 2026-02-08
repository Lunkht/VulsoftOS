package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class IconPackManager {

    private static final String PREFS_NAME = "icon_prefs";
    private static final String KEY_ICON_PACK = "selected_icon_pack";

    public static final String PACK_DEFAULT = "default";
    public static final String PACK_RUVOLUTE = "ruvolute";
    public static final String PACK_AFRIQUI = "afriqui";

    private static final Map<String, Integer> ruvoluteMap = new HashMap<>();
    private static final Map<String, Integer> afriquiMap = new HashMap<>();

    // Target size for icons (e.g. 192px is good for xxxhdpi)
    private static final int TARGET_ICON_SIZE = 192;

    static {
        // Mapping: Package Name -> Drawable Resource ID
        // Note: In a real scenario, this list would be extensive.
        // We use partial package matching or exact matching.
        
        // --- RUVOLUTE THEME ---
        // Settings
        ruvoluteMap.put("com.android.settings", R.drawable.ic_theme_settings);
        ruvoluteMap.put("com.vulsoft.vulsoftos.SettingsActivity", R.drawable.ic_theme_settings); // Self
        
        // Phone / Dialer
        ruvoluteMap.put("com.android.dialer", R.drawable.ic_theme_phone);
        ruvoluteMap.put("com.google.android.dialer", R.drawable.ic_theme_phone);
        ruvoluteMap.put("com.samsung.android.dialer", R.drawable.ic_theme_phone);
        
        // Camera
        ruvoluteMap.put("com.android.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.android.camera2", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.google.android.GoogleCamera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.samsung.android.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.sec.android.app.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.oneplus.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.asus.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.sonyericsson.android.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.motorola.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.huawei.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.oppo.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.coloros.camera", R.drawable.ic_theme_camera);
        ruvoluteMap.put("com.miui.camera", R.drawable.ic_theme_camera);
        
        // Messages
        ruvoluteMap.put("com.google.android.apps.messaging", R.drawable.ic_theme_message);
        ruvoluteMap.put("com.android.mms", R.drawable.ic_theme_message);
        
        // Browser
        ruvoluteMap.put("com.android.chrome", R.drawable.ic_theme_browser);
        ruvoluteMap.put("com.google.android.apps.chrome", R.drawable.ic_theme_browser);

        // --- AFRIQUI THEME ---
        // Settings
        afriquiMap.put("com.android.settings", R.drawable.afro_settings);
        afriquiMap.put("com.vulsoft.vulsoftos.activities.SettingsActivity", R.drawable.afro_settings);

        // Phone / Dialer
        afriquiMap.put("com.android.dialer", R.drawable.afro_phone);
        afriquiMap.put("com.google.android.dialer", R.drawable.afro_phone);
        afriquiMap.put("com.samsung.android.dialer", R.drawable.afro_phone);

        // Camera
        afriquiMap.put("com.android.camera", R.drawable.afro_camera);
        afriquiMap.put("com.android.camera2", R.drawable.afro_camera);
        afriquiMap.put("com.google.android.GoogleCamera", R.drawable.afro_camera);
        afriquiMap.put("com.samsung.android.camera", R.drawable.afro_camera);
        afriquiMap.put("com.sec.android.app.camera", R.drawable.afro_camera);
        afriquiMap.put("com.oneplus.camera", R.drawable.afro_camera);
        afriquiMap.put("com.asus.camera", R.drawable.afro_camera);
        afriquiMap.put("com.sonyericsson.android.camera", R.drawable.afro_camera);
        afriquiMap.put("com.motorola.camera", R.drawable.afro_camera);
        afriquiMap.put("com.huawei.camera", R.drawable.afro_camera);
        afriquiMap.put("com.oppo.camera", R.drawable.afro_camera);
        afriquiMap.put("com.coloros.camera", R.drawable.afro_camera);
        afriquiMap.put("com.miui.camera", R.drawable.afro_camera);

        // Messages
        afriquiMap.put("com.google.android.apps.messaging", R.drawable.afro_messages);
        afriquiMap.put("com.android.mms", R.drawable.afro_messages);

        // Contacts
        afriquiMap.put("com.android.contacts", R.drawable.afro_contact);
        afriquiMap.put("com.google.android.contacts", R.drawable.afro_contact);
        afriquiMap.put("com.samsung.android.contacts", R.drawable.afro_contact);
        afriquiMap.put("com.samsung.android.app.contacts", R.drawable.afro_contact);
        afriquiMap.put("com.oneplus.contacts", R.drawable.afro_contact);
        afriquiMap.put("com.huawei.contacts", R.drawable.afro_contact);

        // Calendar
        afriquiMap.put("com.android.calendar", R.drawable.afro_calendar);
        afriquiMap.put("com.google.android.calendar", R.drawable.afro_calendar);
        afriquiMap.put("com.samsung.android.calendar", R.drawable.afro_calendar);

        // Calculator
        afriquiMap.put("com.android.calculator2", R.drawable.afro_calculator);
        afriquiMap.put("com.google.android.calculator", R.drawable.afro_calculator);
        afriquiMap.put("com.sec.android.app.popupcalculator", R.drawable.afro_calculator);

        // Gallery / Photos
        afriquiMap.put("com.android.gallery3d", R.drawable.afro_gallery);
        afriquiMap.put("com.google.android.apps.photos", R.drawable.afro_gallery);
        afriquiMap.put("com.sec.android.gallery3d", R.drawable.afro_gallery);
        afriquiMap.put("com.miui.gallery", R.drawable.afro_gallery);

        // Documents / Files
        afriquiMap.put("com.android.documentsui", R.drawable.afro_document);
        afriquiMap.put("com.google.android.documentsui", R.drawable.afro_document);
        afriquiMap.put("com.google.android.apps.nbu.files", R.drawable.afro_document);
        afriquiMap.put("com.sec.android.app.myfiles", R.drawable.afro_document);

        // Browsers
        afriquiMap.put("com.android.chrome", R.drawable.afro_g_chrome);
        afriquiMap.put("com.google.android.apps.chrome", R.drawable.afro_g_chrome);
        afriquiMap.put("org.mozilla.firefox", R.drawable.afro_firefox);
        afriquiMap.put("org.mozilla.firefox_beta", R.drawable.afro_firefox);

        // Email
        afriquiMap.put("com.google.android.gm", R.drawable.afro_email);
        afriquiMap.put("com.android.email", R.drawable.afro_email);
        afriquiMap.put("com.samsung.android.email.provider", R.drawable.afro_email);
        afriquiMap.put("com.microsoft.office.outlook", R.drawable.afro_email);
        afriquiMap.put("com.yahoo.mobile.client.android.mail", R.drawable.afro_email);
    }

    public static void saveIconPack(Context context, String packName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ICON_PACK, packName).apply();
    }

    public static String getSavedIconPack(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ICON_PACK, PACK_DEFAULT);
    }

    public static Drawable getIcon(Context context, String packageName, Drawable defaultIcon) {
        String currentPack = getSavedIconPack(context);
        Drawable icon = defaultIcon;

        if (PACK_RUVOLUTE.equals(currentPack)) {
            Integer resId = ruvoluteMap.get(packageName);
            if (resId != null) {
                try {
                    icon = loadSafeDrawable(context, resId);
                } catch (Exception e) {
                    // keep default
                }
            }
        } else if (PACK_AFRIQUI.equals(currentPack)) {
            Integer resId = afriquiMap.get(packageName);
            if (resId != null) {
                try {
                    icon = loadSafeDrawable(context, resId);
                } catch (Exception e) {
                    // keep default
                }
            }
        }

        // Apply Shape
        String shape = IconShapeHelper.getSavedShape(context);
        return IconShapeHelper.applyShape(context, icon, shape);
    }

    public static Drawable loadSafeDrawable(Context context, int resId) {
        try {
            int targetSize = getTargetIconSize(context);

            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(context.getResources(), resId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
            
            if (bitmap != null) {
                return new BitmapDrawable(context.getResources(), bitmap);
            } else {
                // Fallback if decoding fails (e.g. vector drawable)
                return ContextCompat.getDrawable(context, resId);
            }
        } catch (Exception e) {
            // Fallback for any error
            return ContextCompat.getDrawable(context, resId);
        }
    }

    private static int getTargetIconSize(Context context) {
        // Calculate target size based on density (e.g., 48dp * density)
        // Standard launcher icon sizes: mdpi=48, hdpi=72, xhdpi=96, xxhdpi=144, xxxhdpi=192
        // We aim for the largest needed for the device.
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (48 * density * 1.2f); // 1.2f factor for slightly larger source to ensure quality
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

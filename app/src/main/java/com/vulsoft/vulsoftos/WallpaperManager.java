package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.InputStream;

public class WallpaperManager {
    private static final String PREFS_NAME = "wallpaper_prefs";
    private static final String KEY_WALLPAPER = "selected_wallpaper";
    private static final String KEY_IS_CUSTOM = "is_custom_wallpaper";
    private static final String KEY_CUSTOM_URI = "custom_wallpaper_uri";
    private static final String KEY_SCALE_TYPE = "wallpaper_scale_type";

    public static void saveWallpaper(Context context, int wallpaperResId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_WALLPAPER, wallpaperResId)
                .putBoolean(KEY_IS_CUSTOM, false)
                .apply();

        try {
            android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance(context);
            wallpaperManager.setResource(wallpaperResId,
                    android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveCustomWallpaper(Context context, Uri uri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CUSTOM_URI, uri.toString())
                .putBoolean(KEY_IS_CUSTOM, true)
                .apply();

        try {
            android.app.WallpaperManager wallpaperManager = android.app.WallpaperManager.getInstance(context);
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                wallpaperManager.setStream(inputStream, null, true,
                        android.app.WallpaperManager.FLAG_SYSTEM | android.app.WallpaperManager.FLAG_LOCK);
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getSavedWallpaper(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_WALLPAPER, -1);
    }

    public static boolean isCustomWallpaper(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_IS_CUSTOM, false);
    }

    public static WallpaperDrawable.ScaleType getScaleType(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String scaleTypeName = prefs.getString(KEY_SCALE_TYPE, "CENTER_CROP");
        try {
            return WallpaperDrawable.ScaleType.valueOf(scaleTypeName);
        } catch (IllegalArgumentException e) {
            return WallpaperDrawable.ScaleType.CENTER_CROP;
        }
    }

    public static void setScaleType(Context context, WallpaperDrawable.ScaleType scaleType) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SCALE_TYPE, scaleType.name()).apply();
    }

    public static void setRandomWallpaper(Context context) {
        int[] wallpapers = {
            R.drawable.wallpaper_black,
            R.drawable.wallpaper_blue,
            R.drawable.wallpaper_dark,
            R.drawable.wallpaper_green,
            R.drawable.wallpaper_orange,
            R.drawable.wallpaper_purple
        };

        int currentResId = getSavedWallpaper(context);
        java.util.Random random = new java.util.Random();
        int nextResId;
        
        // Try to pick a different wallpaper
        do {
            nextResId = wallpapers[random.nextInt(wallpapers.length)];
        } while (wallpapers.length > 1 && nextResId == currentResId);

        saveWallpaper(context, nextResId);
    }

    public static Drawable getCurrentWallpaper(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences launcherPrefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        boolean isCustom = prefs.getBoolean(KEY_IS_CUSTOM, false);
        WallpaperDrawable.ScaleType scaleType = getScaleType(context);
        int blurRadius = launcherPrefs.getInt("wallpaper_blur_radius", 0);

        Bitmap bitmap = null;

        if (isCustom) {
            String uriString = prefs.getString(KEY_CUSTOM_URI, null);
            if (uriString != null) {
                try {
                    InputStream inputStream = context.getContentResolver()
                            .openInputStream(Uri.parse(uriString));
                    if (inputStream != null) {
                        // Optimiser le chargement pour éviter les OutOfMemoryError
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(inputStream, null, options);
                        inputStream.close();

                        // Calculer la taille d'échantillonnage
                        options.inSampleSize = calculateInSampleSize(options, 1080, 1920);
                        options.inJustDecodeBounds = false;
                        // Enable mutable bitmap for blur
                        options.inMutable = true;

                        inputStream = context.getContentResolver()
                                .openInputStream(Uri.parse(uriString));
                        bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                        inputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Charger depuis les ressources
            int resId = prefs.getInt(KEY_WALLPAPER, -1);
            if (resId == -1) {
                return null;
            }
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeResource(context.getResources(), resId, options);

                options.inSampleSize = calculateInSampleSize(options, 1080, 1920);
                options.inJustDecodeBounds = false;
                // Enable mutable bitmap for blur
                options.inMutable = true;

                bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitmap != null) {
            if (blurRadius > 0) {
                // Apply advanced blur logic: downscale for higher intensity (>25 effective radius)
                float scale = 1.0f;
                float rsRadius = blurRadius;
                
                // If blur radius is high, downscale to achieve stronger blur effect
                // RenderScript limit is 25.0f. 
                // We map 0-100 preference to stronger effects.
                // If radius <= 25, we use it directly (scale 1.0).
                // If radius > 25, we downscale.
                // Example: radius 50 -> scale 0.5, RS radius 25 -> effective 50.
                // Example: radius 100 -> scale 0.25, RS radius 25 -> effective 100.
                
                if (blurRadius > 25) {
                    scale = 25.0f / (float) blurRadius;
                    rsRadius = 25.0f;
                }
                
                // Limit scale to avoid too small bitmaps
                scale = Math.max(0.1f, scale);

                try {
                    Bitmap inputForBlur = bitmap;
                    if (scale < 1.0f) {
                        int scaledWidth = Math.max(1, (int) (bitmap.getWidth() * scale));
                        int scaledHeight = Math.max(1, (int) (bitmap.getHeight() * scale));
                        inputForBlur = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
                    }
                    
                    bitmap = blur(context, inputForBlur, rsRadius);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new WallpaperDrawable(bitmap, scaleType);
        } else {
            // Fallback vers le drawable par défaut
            int resId = prefs.getInt(KEY_WALLPAPER, -1);
            if (resId == -1) {
                return null;
            }
            return androidx.core.content.ContextCompat.getDrawable(context, resId);
        }
    }

    // RenderScript Blur implementation
    public static Bitmap blur(Context context, Bitmap image, float radius) {
        if (radius <= 0) return image;
        if (radius > 25) radius = 25.0f;

        Bitmap inputBitmap = image;
        // Check if config is supported or null, RenderScript needs a valid config
        if (image.getConfig() == null || image.getConfig() == Bitmap.Config.HARDWARE) {
             inputBitmap = image.copy(Bitmap.Config.ARGB_8888, true);
        }

        try {
            // Create a copy for output
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap.getWidth(), inputBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            
            android.renderscript.RenderScript rs = android.renderscript.RenderScript.create(context);
            android.renderscript.ScriptIntrinsicBlur theIntrinsic = android.renderscript.ScriptIntrinsicBlur.create(rs, android.renderscript.Element.U8_4(rs));
            
            android.renderscript.Allocation tmpIn = android.renderscript.Allocation.createFromBitmap(rs, inputBitmap);
            android.renderscript.Allocation tmpOut = android.renderscript.Allocation.createFromBitmap(rs, outputBitmap);
            
            theIntrinsic.setRadius(radius);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            
            tmpOut.copyTo(outputBitmap);
            rs.destroy();
            
            return outputBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return image; // Fallback to original if blur fails
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Drawable getCurrentWallpaperWithScaleType(Context context, WallpaperDrawable.ScaleType scaleType) {
        Drawable drawable = getCurrentWallpaper(context);
        if (drawable instanceof WallpaperDrawable) {
            ((WallpaperDrawable) drawable).setScaleType(scaleType);
        }
        return drawable;
    }
}

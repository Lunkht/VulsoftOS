package com.vulsoft.vulsoftos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

public class WallpaperUtils {
    
    /**
     * Applique le fond d'écran à une vue avec les bonnes proportions
     */
    public static void applyWallpaperToView(Context context, View view) {
        try {
            Drawable wallpaperDrawable = WallpaperManager.getCurrentWallpaper(context);
            view.setBackground(wallpaperDrawable);
            
            // Appliquer l'effet de flou si nécessaire
            applyBlurEffect(context, view);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback vers un fond par défaut
            view.setBackgroundResource(WallpaperManager.getSavedWallpaper(context));
        }
    }
    
    /**
     * Applique l'effet de flou selon les paramètres utilisateur
     */
    public static void applyBlurEffect(Context context, View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int blurRadius = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
                    .getInt("wallpaper_blur_radius", 0);
            
            if (blurRadius > 0) {
                // Convertir le pourcentage en valeur réelle (0-25)
                float actualBlurRadius = (blurRadius / 100f) * 25f;
                RenderEffect blurEffect = RenderEffect.createBlurEffect(
                    actualBlurRadius, actualBlurRadius, Shader.TileMode.CLAMP);
                view.setRenderEffect(blurEffect);
            } else {
                view.setRenderEffect(null);
            }
        }
    }
    
    /**
     * Crée un bitmap redimensionné avec les bonnes proportions
     */
    public static Bitmap createScaledBitmap(Bitmap original, int targetWidth, int targetHeight, 
                                          WallpaperDrawable.ScaleType scaleType) {
        if (original == null) return null;
        
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        
        if (originalWidth == 0 || originalHeight == 0) return original;
        
        float scaleX, scaleY, scale;
        int scaledWidth, scaledHeight;
        int offsetX = 0, offsetY = 0;
        
        switch (scaleType) {
            case CENTER_CROP:
                scaleX = (float) targetWidth / originalWidth;
                scaleY = (float) targetHeight / originalHeight;
                scale = Math.max(scaleX, scaleY);
                scaledWidth = Math.round(originalWidth * scale);
                scaledHeight = Math.round(originalHeight * scale);
                offsetX = (targetWidth - scaledWidth) / 2;
                offsetY = (targetHeight - scaledHeight) / 2;
                break;
                
            case CENTER_INSIDE:
                scaleX = (float) targetWidth / originalWidth;
                scaleY = (float) targetHeight / originalHeight;
                scale = Math.min(scaleX, scaleY);
                scaledWidth = Math.round(originalWidth * scale);
                scaledHeight = Math.round(originalHeight * scale);
                offsetX = (targetWidth - scaledWidth) / 2;
                offsetY = (targetHeight - scaledHeight) / 2;
                break;
                
            case FIT_XY:
            default:
                scaledWidth = targetWidth;
                scaledHeight = targetHeight;
                break;
        }
        
        Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        if (scaleType == WallpaperDrawable.ScaleType.FIT_XY) {
            // Étirement simple
            Bitmap scaled = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true);
            canvas.drawBitmap(scaled, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
            if (scaled != original) scaled.recycle();
        } else {
            // Redimensionnement avec proportions
            Bitmap scaled = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true);
            canvas.drawBitmap(scaled, offsetX, offsetY, new Paint(Paint.ANTI_ALIAS_FLAG));
            if (scaled != original) scaled.recycle();
        }
        
        return result;
    }
    
    /**
     * Optimise les options de décodage pour éviter les OutOfMemoryError
     */
    public static BitmapFactory.Options getOptimizedBitmapOptions(Context context, int targetWidth, int targetHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // Utilise moins de mémoire
        options.inDither = true;
        options.inScaled = false;
        
        // Calculer la taille d'échantillonnage appropriée
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        
        // Utiliser la taille d'écran si les dimensions cibles ne sont pas spécifiées
        if (targetWidth <= 0) targetWidth = screenWidth;
        if (targetHeight <= 0) targetHeight = screenHeight;
        
        // Pas d'échantillonnage pour les petites images
        options.inSampleSize = 1;
        
        return options;
    }
    
    /**
     * Libère la mémoire des bitmaps inutilisés
     */
    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
    
    /**
     * Vérifie si l'image nécessite un redimensionnement
     */
    public static boolean needsResizing(int imageWidth, int imageHeight, int targetWidth, int targetHeight) {
        return imageWidth > targetWidth * 1.5f || imageHeight > targetHeight * 1.5f;
    }
}
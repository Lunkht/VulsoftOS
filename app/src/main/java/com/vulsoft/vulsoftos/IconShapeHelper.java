package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.graphics.drawable.AdaptiveIconDrawable;
import android.os.Build;

public class IconShapeHelper {

    public static final String PREFS_NAME = "launcher_prefs";
    public static final String KEY_ICON_SHAPE = "icon_shape";

    public static final String SHAPE_ORIGINAL = "original";
    public static final String SHAPE_CIRCLE = "circle";
    public static final String SHAPE_SQUIRCLE = "squircle";
    public static final String SHAPE_ROUNDED_SQUARE = "rounded_square";
    public static final String SHAPE_TEARDROP = "teardrop";

    public static String getSavedShape(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ICON_SHAPE, SHAPE_ORIGINAL);
    }

    public static void saveShape(Context context, String shape) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ICON_SHAPE, shape).apply();
    }

    public static Drawable applyShape(Context context, Drawable original, String shapeMode) {
        if (original == null) return null;
        if (SHAPE_ORIGINAL.equals(shapeMode)) return original;

        // If it's adaptive, we construct the shaped bitmap directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && original instanceof AdaptiveIconDrawable) {
             return new BitmapDrawable(context.getResources(), getAdaptiveBitmap((AdaptiveIconDrawable) original, shapeMode));
        }

        Bitmap legacyBitmap = drawableToBitmap(original);
        if (legacyBitmap == null) return original;

        Bitmap shapedBitmap = getShapedBitmap(legacyBitmap, shapeMode);
        return new BitmapDrawable(context.getResources(), shapedBitmap);
    }

    private static Bitmap getAdaptiveBitmap(AdaptiveIconDrawable icon, String shapeMode) {
        int width = icon.getIntrinsicWidth();
        int height = icon.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            width = 192; height = 192;
        }
        
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        
        // 1. Create the Path for the shape
        Path path = getShapePath(shapeMode, width, height);
        
        // 2. Clip Canvas to Path
        canvas.save();
        canvas.clipPath(path);
        
        // 3. Draw Background
        Drawable bg = icon.getBackground();
        if (bg != null) {
            bg.setBounds(0, 0, width, height);
            bg.draw(canvas);
        } else {
            // If no background, maybe white? Or transparent? Standard adaptive icons have background.
            canvas.drawColor(0xFFFFFFFF); 
        }
        
        // 4. Draw Foreground
        Drawable fg = icon.getForeground();
        if (fg != null) {
            fg.setBounds(0, 0, width, height);
            fg.draw(canvas);
        }
        
        canvas.restore();
        
        return output;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        if (width <= 0 || height <= 0) {
            width = 192; // Default size if intrinsic is invalid
            height = 192;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static Bitmap getShapedBitmap(Bitmap bitmap, String shapeMode) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, width, height);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);

        Path path = getShapePath(shapeMode, width, height);
        canvas.drawPath(path, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private static Path getShapePath(String shapeMode, int width, int height) {
        Path path = new Path();
        float w = width;
        float h = height;

        switch (shapeMode) {
            case SHAPE_CIRCLE:
                path.addCircle(w / 2, h / 2, Math.min(w, h) / 2, Path.Direction.CW);
                break;
            case SHAPE_ROUNDED_SQUARE:
                float radius = Math.min(w, h) * 0.2f;
                path.addRoundRect(0, 0, w, h, radius, radius, Path.Direction.CW);
                break;
            case SHAPE_SQUIRCLE:
                // Approximation of a squircle
                path.moveTo(0, h / 2);
                path.cubicTo(0, h * 0.05f, w * 0.05f, 0, w / 2, 0);
                path.cubicTo(w * 0.95f, 0, w, h * 0.05f, w, h / 2);
                path.cubicTo(w, h * 0.95f, w * 0.95f, h, w / 2, h);
                path.cubicTo(w * 0.05f, h, 0, h * 0.95f, 0, h / 2);
                path.close();
                break;
            case SHAPE_TEARDROP:
                 path.addCircle(w / 2, h / 2, Math.min(w, h) / 2, Path.Direction.CW);
                 // We need to 'fill' one corner to make it a teardrop, usually bottom-right is round, 
                 // wait, teardrop usually has one sharp corner. Let's say top-left is sharp.
                 // Actually standard Android adaptive icon teardrop is a bit different.
                 // Let's implement a simple version: Circle + a square in one corner?
                 // Let's do a custom path.
                 
                 // Reset path for teardrop
                 path.reset();
                 // Top-left is 0,0. Let's make top-left sharp? Or bottom-right?
                 // Common teardrop icon: Round except top-left.
                 // Draw a rounded rect but with top-left radius = 0
                 float r = Math.min(w, h) / 2; // Radius for 3 corners
                 float[] radii = new float[] {
                     0, 0,           // Top-left
                     r*2, r*2,       // Top-right (fully round)
                     r*2, r*2,       // Bottom-right (fully round)
                     r*2, r*2        // Bottom-left (fully round)
                 };
                 // Wait, addRoundRect takes radii array.
                 // Actually simple teardrop:
                 // Top Left corner is sharp.
                 // Center at w/2, h/2. Radius w/2.
                 // It's basically a circle but the top-left quadrant is filled as a square?
                 // No, that would be bigger than the circle.
                 
                 // Better approximation:
                 // 0,0 to w,h
                 // Top-Right, Bottom-Right, Bottom-Left are rounded. Top-Left is sharp.
                 float cornerRadius = Math.min(w, h) * 0.5f; // Full circle radius for corners
                 // But wait, if radius is 0.5w, and we use it for 3 corners, it becomes a circle with one sharp corner if the rect is square.
                 // Let's try 0 radius for top-left.
                 float[] corners = new float[]{
                         0, 0,        // Top Left
                         cornerRadius, cornerRadius, // Top Right
                         cornerRadius, cornerRadius, // Bottom Right
                         cornerRadius, cornerRadius  // Bottom Left
                 };
                 // Note: corners must be capable of fitting.
                 // If we want a teardrop shape inside the square bound, we might need to scale it down slightly 
                 // or just use the full bound and let the user accept it fills the corners.
                 // Usually adaptive icons are masked within a safe zone.
                 // Here we are shaping the full bitmap.
                 
                 // Let's use a slightly smaller radius for general rounded square feel but sharp top left
                 float tdRadius = Math.min(w, h) * 0.4f;
                 float[] tdRadii = new float[] {
                         0, 0,
                         tdRadius, tdRadius,
                         tdRadius, tdRadius,
                         tdRadius, tdRadius
                 };
                 
                 // Actually, standard teardrop shape:
                 path.moveTo(w * 0.2f, h * 0.2f); // Start near top left
                 path.lineTo(w - tdRadius, 0);
                 // This is getting complicated to code manually without SVG path data.
                 // Let's use the rounded rect approach with one sharp corner, it's the standard "message bubble" or teardrop look.
                 path.addRoundRect(0, 0, w, h, corners, Path.Direction.CW);
                 
                 // Wait, if cornerRadius is w/2, then:
                 // TopRight starts at w/2,0 arc to w,h/2
                 // BottomRight starts at w,h/2 arc to w/2,h
                 // BottomLeft starts at w/2,h arc to 0,h/2
                 // TopLeft is sharp 0,0.
                 // This forms exactly a teardrop (or location pin rotated).
                 break;
            default:
                 // Default to original/empty path (won't draw anything if not handled)
                 path.addRect(0, 0, w, h, Path.Direction.CW);
                 break;
        }
        return path;
    }
}

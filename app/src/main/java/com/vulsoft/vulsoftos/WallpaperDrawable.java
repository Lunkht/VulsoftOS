package com.vulsoft.vulsoftos;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Drawable personnalisé qui gère correctement le redimensionnement des fonds d'écran
 * en préservant les proportions et en appliquant un crop center
 */
public class WallpaperDrawable extends Drawable {
    private Bitmap bitmap;
    private Paint paint;
    private Matrix matrix;
    private ScaleType scaleType;

    public enum ScaleType {
        CENTER_CROP,    // Remplit complètement en coupant si nécessaire
        CENTER_INSIDE,  // Ajuste pour que tout soit visible
        FIT_XY         // Étire pour remplir (comportement par défaut Android)
    }

    public WallpaperDrawable(Bitmap bitmap) {
        this(bitmap, ScaleType.CENTER_CROP);
    }

    public WallpaperDrawable(Bitmap bitmap, ScaleType scaleType) {
        this.bitmap = bitmap;
        this.scaleType = scaleType;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        this.matrix = new Matrix();
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }

        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }

        updateMatrix(bounds);
        canvas.drawBitmap(bitmap, matrix, paint);
    }

    private void updateMatrix(Rect bounds) {
        if (bitmap == null) return;

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        int boundsWidth = bounds.width();
        int boundsHeight = bounds.height();

        if (bitmapWidth == 0 || bitmapHeight == 0 || boundsWidth == 0 || boundsHeight == 0) {
            return;
        }

        matrix.reset();

        switch (scaleType) {
            case CENTER_CROP:
                applyCenterCrop(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight);
                break;
            case CENTER_INSIDE:
                applyCenterInside(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight);
                break;
            case FIT_XY:
                applyFitXY(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight);
                break;
        }

        matrix.postTranslate(bounds.left, bounds.top);
    }

    private void applyCenterCrop(int bitmapWidth, int bitmapHeight, int boundsWidth, int boundsHeight) {
        float scaleX = (float) boundsWidth / bitmapWidth;
        float scaleY = (float) boundsHeight / bitmapHeight;
        float scale = Math.max(scaleX, scaleY);

        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;

        float dx = (boundsWidth - scaledWidth) * 0.5f;
        float dy = (boundsHeight - scaledHeight) * 0.5f;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
    }

    private void applyCenterInside(int bitmapWidth, int bitmapHeight, int boundsWidth, int boundsHeight) {
        float scaleX = (float) boundsWidth / bitmapWidth;
        float scaleY = (float) boundsHeight / bitmapHeight;
        float scale = Math.min(scaleX, scaleY);

        float scaledWidth = bitmapWidth * scale;
        float scaledHeight = bitmapHeight * scale;

        float dx = (boundsWidth - scaledWidth) * 0.5f;
        float dy = (boundsHeight - scaledHeight) * 0.5f;

        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
    }

    private void applyFitXY(int bitmapWidth, int bitmapHeight, int boundsWidth, int boundsHeight) {
        float scaleX = (float) boundsWidth / bitmapWidth;
        float scaleY = (float) boundsHeight / bitmapHeight;
        matrix.setScale(scaleX, scaleY);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return bitmap != null && !bitmap.hasAlpha() ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return bitmap != null ? bitmap.getWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return bitmap != null ? bitmap.getHeight() : -1;
    }

    public void setScaleType(ScaleType scaleType) {
        if (this.scaleType != scaleType) {
            this.scaleType = scaleType;
            invalidateSelf();
        }
    }

    public ScaleType getScaleType() {
        return scaleType;
    }
}
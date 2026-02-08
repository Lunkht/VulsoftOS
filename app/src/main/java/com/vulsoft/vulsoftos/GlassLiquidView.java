package com.vulsoft.vulsoftos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GlassLiquidView extends View {
    private Paint liquidPaint;
    private Paint glassPaint;
    private Paint highlightPaint;
    private Paint shadowPaint;
    private List<Wave> waves;
    private List<Bubble> bubbles;
    private float liquidLevel = 0.7f;
    private long startTime;
    private Random random;
    private int width, height;
    private Bitmap liquidBitmap;
    private Canvas liquidCanvas;

    public GlassLiquidView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        random = new Random();
        waves = new ArrayList<>();
        bubbles = new ArrayList<>();
        startTime = System.currentTimeMillis();

        liquidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        liquidPaint.setStyle(Paint.Style.FILL);

        glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassPaint.setStyle(Paint.Style.STROKE);
        glassPaint.setStrokeWidth(8);
        glassPaint.setColor(Color.argb(100, 255, 255, 255));

        highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setStyle(Paint.Style.FILL);

        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(Color.argb(50, 0, 0, 0));

        for (int i = 0; i < 3; i++) {
            waves.add(new Wave());
        }

        for (int i = 0; i < 8; i++) {
            bubbles.add(new Bubble());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        liquidBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        liquidCanvas = new Canvas(liquidBitmap);

        // Gradient pour le liquide
        LinearGradient gradient = new LinearGradient(
                0, h * (1 - liquidLevel), 0, h,
                new int[]{
                        Color.argb(200, 100, 200, 255),
                        Color.argb(220, 50, 150, 255),
                        Color.argb(240, 20, 100, 200)
                },
                null,
                Shader.TileMode.CLAMP
        );
        liquidPaint.setShader(gradient);

        for (Wave wave : waves) {
            wave.init(w, h);
        }

        for (Bubble bubble : bubbles) {
            bubble.init(w, h, liquidLevel);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long elapsed = System.currentTimeMillis() - startTime;
        float time = elapsed / 1000f;

        canvas.drawColor(Color.argb(255, 30, 30, 40));

        // Dessiner le verre
        drawGlass(canvas);

        // Préparer le liquide
        liquidCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Dessiner le liquide avec les vagues
        drawLiquid(liquidCanvas, time);

        // Dessiner les bulles
        for (Bubble bubble : bubbles) {
            bubble.update(time);
            bubble.draw(liquidCanvas);
        }

        // Appliquer le liquide sur le canvas principal
        canvas.drawBitmap(liquidBitmap, 0, 0, null);

        // Reflets et effets de verre
        drawGlassEffects(canvas);

        invalidate();
    }

    private void drawGlass(Canvas canvas) {
        float glassWidth = width * 0.6f;
        float glassHeight = height * 0.8f;
        float left = (width - glassWidth) / 2;
        float top = height * 0.1f;

        // Ombre du verre
        canvas.drawRoundRect(
                left + 10, top + 10,
                left + glassWidth + 10, top + glassHeight + 10,
                40, 40, shadowPaint
        );

        // Contour du verre
        canvas.drawRoundRect(
                left, top,
                left + glassWidth, top + glassHeight,
                40, 40, glassPaint
        );
    }

    private void drawLiquid(Canvas canvas, float time) {
        float glassWidth = width * 0.6f;
        float glassHeight = height * 0.8f;
        float left = (width - glassWidth) / 2;
        float top = height * 0.1f;

        Path liquidPath = new Path();
        float liquidTop = top + glassHeight * (1 - liquidLevel);

        liquidPath.moveTo(left, liquidTop);

        // Créer les vagues
        for (int x = 0; x <= glassWidth; x += 5) {
            float waveY = liquidTop;
            for (Wave wave : waves) {
                waveY += wave.getHeight(left + x, time);
            }
            liquidPath.lineTo(left + x, waveY);
        }

        liquidPath.lineTo(left + glassWidth, top + glassHeight - 40);
        liquidPath.quadTo(
                left + glassWidth, top + glassHeight,
                left + glassWidth - 40, top + glassHeight
        );
        liquidPath.lineTo(left + 40, top + glassHeight);
        liquidPath.quadTo(
                left, top + glassHeight,
                left, top + glassHeight - 40
        );
        liquidPath.close();

        canvas.drawPath(liquidPath, liquidPaint);
    }

    private void drawGlassEffects(Canvas canvas) {
        float glassWidth = width * 0.6f;
        float glassHeight = height * 0.8f;
        float left = (width - glassWidth) / 2;
        float top = height * 0.1f;

        // Reflet lumineux
        LinearGradient highlight = new LinearGradient(
                left, top, left + glassWidth * 0.3f, top + glassHeight * 0.3f,
                Color.argb(80, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
        );
        highlightPaint.setShader(highlight);

        Path highlightPath = new Path();
        highlightPath.moveTo(left + 20, top + 20);
        highlightPath.lineTo(left + glassWidth * 0.3f, top + 20);
        highlightPath.lineTo(left + glassWidth * 0.2f, top + glassHeight * 0.4f);
        highlightPath.lineTo(left + 20, top + glassHeight * 0.3f);
        highlightPath.close();

        canvas.drawPath(highlightPath, highlightPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            // Créer une perturbation dans les vagues
            for (Wave wave : waves) {
                wave.disturb(event.getX(), event.getY());
            }
            // Ajouter des bulles
            if (random.nextFloat() > 0.7f) {
                bubbles.add(new Bubble(event.getX(), event.getY(), width, height));
            }
        }
        return true;
    }

    private class Wave {
        float amplitude;
        float frequency;
        float phase;
        float speed;
        float disturbance = 0;
        float disturbX = 0;

        void init(int w, int h) {
            amplitude = 5 + random.nextFloat() * 10;
            frequency = 0.01f + random.nextFloat() * 0.02f;
            phase = random.nextFloat() * (float) Math.PI * 2;
            speed = 0.5f + random.nextFloat() * 1.5f;
        }

        float getHeight(float x, float time) {
            float baseWave = amplitude * (float) Math.sin(frequency * x + phase + speed * time);
            float dist = (float) Math.exp(-Math.pow((x - disturbX) / 100, 2)) * disturbance;
            disturbance *= 0.95f;
            return baseWave + dist;
        }

        void disturb(float x, float y) {
            disturbX = x;
            disturbance = 20;
        }
    }

    private class Bubble {
        float x, y;
        float radius;
        float speed;
        float wobble;
        float wobbleSpeed;
        int w, h;
        float liquidTop;
        Paint bubblePaint;

        Bubble() {
            bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            bubblePaint.setStyle(Paint.Style.FILL);
        }

        Bubble(float x, float y, int w, int h) {
            this();
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.liquidTop = h * 0.1f + h * 0.8f * (1 - liquidLevel);
            radius = 5 + random.nextFloat() * 15;
            speed = 20 + random.nextFloat() * 40;
            wobble = random.nextFloat() * (float) Math.PI * 2;
            wobbleSpeed = 2 + random.nextFloat() * 3;
        }

        void init(int w, int h, float level) {
            this.w = w;
            this.h = h;
            this.liquidTop = h * 0.1f + h * 0.8f * (1 - level);
            float glassWidth = w * 0.6f;
            float left = (w - glassWidth) / 2;
            x = left + 40 + random.nextFloat() * (glassWidth - 80);
            y = liquidTop + random.nextFloat() * (h * 0.8f * level - 80);
            radius = 5 + random.nextFloat() * 15;
            speed = 20 + random.nextFloat() * 40;
            wobble = random.nextFloat() * (float) Math.PI * 2;
            wobbleSpeed = 2 + random.nextFloat() * 3;
        }

        void update(float time) {
            y -= speed * 0.016f;
            wobble += wobbleSpeed * 0.016f;
            x += (float) Math.sin(wobble) * 1.5f;

            if (y + radius < liquidTop) {
                float glassWidth = w * 0.6f;
                float left = (w - glassWidth) / 2;
                x = left + 40 + random.nextFloat() * (glassWidth - 80);
                y = h * 0.1f + h * 0.8f - 40;
                radius = 5 + random.nextFloat() * 15;
                speed = 20 + random.nextFloat() * 40;
            }
        }

        void draw(Canvas canvas) {
            bubblePaint.setColor(Color.argb(100, 200, 230, 255));
            canvas.drawCircle(x, y, radius, bubblePaint);

            bubblePaint.setColor(Color.argb(150, 255, 255, 255));
            canvas.drawCircle(x - radius * 0.3f, y - radius * 0.3f, radius * 0.3f, bubblePaint);
        }
    }
}
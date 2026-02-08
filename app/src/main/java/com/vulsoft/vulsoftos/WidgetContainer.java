package com.vulsoft.vulsoftos;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class WidgetContainer extends FrameLayout {

    private GestureDetector gestureDetector;
    private boolean isDragging = false;
    private boolean isResizing = false;
    private float lastX, lastY;
    private OnWidgetChangedListener listener;
    private View widgetView; // The child widget view
    private ImageView resizeHandle;
    private int minWidth = 100;
    private int minHeight = 100;

    public interface OnWidgetChangedListener {
        void onWidgetMoved(View widget, float x, float y);
        void onWidgetResized(View widget, int width, int height);
    }

    public WidgetContainer(Context context) {
        super(context);
        init();
    }

    public WidgetContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize Gesture Detector for Long Press (Drag)
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                isDragging = true;
                lastX = e.getRawX();
                lastY = e.getRawY();
                // Optional: Vibrate here
                try {
                    android.os.Vibrator v = (android.os.Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(50);
                    }
                } catch (Exception ex) {
                    // Ignore
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                
                // Show resize handle when selected/dragging
                resizeHandle.setVisibility(View.VISIBLE);
            }
            
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Hide resize handle on tap outside? 
                // For now, let's keep it simple. Tapping the widget passes through.
                return false; 
            }
        });

        // Initialize Resize Handle
        resizeHandle = new ImageView(getContext());
        resizeHandle.setImageResource(R.drawable.ic_resize); // Ensure this resource exists
        resizeHandle.setVisibility(View.GONE); // Hidden by default
        
        FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(
                64, 64 // Fixed size for handle
        );
        handleParams.gravity = Gravity.BOTTOM | Gravity.END;
        addView(resizeHandle, handleParams);
    }

    public void setWidgetView(View view) {
        this.widgetView = view;
        // Add widget view at index 0 so handle is on top
        if (view.getParent() != null) {
            ((android.view.ViewGroup)view.getParent()).removeView(view);
        }
        addView(view, 0, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    public void setOnWidgetChangedListener(OnWidgetChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if touching resize handle
            if (resizeHandle.getVisibility() == View.VISIBLE) {
                int[] location = new int[2];
                resizeHandle.getLocationOnScreen(location);
                float rawX = ev.getRawX();
                float rawY = ev.getRawY();
                
                if (rawX >= location[0] && rawX <= location[0] + resizeHandle.getWidth() &&
                    rawY >= location[1] && rawY <= location[1] + resizeHandle.getHeight()) {
                    isResizing = true;
                    lastX = rawX;
                    lastY = rawY;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true; // Intercept for resizing
                }
            }

            // Reset drag state
            isDragging = false;
            lastX = ev.getRawX();
            lastY = ev.getRawY();
        }
        
        // Pass to detector to check for long press
        gestureDetector.onTouchEvent(ev);

        // If we are dragging, intercept events
        return isDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If we are here, we are either dragging, resizing, or the child didn't handle the touch
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaX = event.getRawX() - lastX;
                    float deltaY = event.getRawY() - lastY;

                    float newX = getX() + deltaX;
                    float newY = getY() + deltaY;

                    setX(newX);
                    setY(newY);

                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    return true;
                } else if (isResizing) {
                    float deltaX = event.getRawX() - lastX;
                    float deltaY = event.getRawY() - lastY;
                    
                    int newWidth = Math.max(minWidth, getWidth() + (int)deltaX);
                    int newHeight = Math.max(minHeight, getHeight() + (int)deltaY);
                    
                    android.view.ViewGroup.LayoutParams params = getLayoutParams();
                    params.width = newWidth;
                    params.height = newHeight;
                    setLayoutParams(params);
                    
                    lastX = event.getRawX();
                    lastY = event.getRawY();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    if (listener != null) {
                        listener.onWidgetMoved(this, getX(), getY());
                    }
                    getParent().requestDisallowInterceptTouchEvent(false);
                    // Keep resize handle visible after drag for adjustment?
                    // Or hide it? Let's keep it visible for now until clicked outside.
                    return true;
                }
                if (isResizing) {
                    isResizing = false;
                    if (listener != null) {
                        listener.onWidgetResized(this, getWidth(), getHeight());
                    }
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                
                // If we tapped outside (not dragging/resizing), maybe hide handle?
                // For now, let's toggle handle visibility on long press only.
                break;
        }
        
        return super.onTouchEvent(event);
    }
}

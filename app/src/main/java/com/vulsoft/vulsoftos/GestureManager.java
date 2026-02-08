package com.vulsoft.vulsoftos;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureManager {

    public interface GestureListener {
        void onSwipeDown();

        void onSwipeUp();

        void onDoubleTap();

        void onLongPress(MotionEvent e);

        void onTwoFingerSwipeDown();
    }

    private final GestureDetector detector;
    private final GestureListener listener;
    
    // Multi-touch tracking variables
    private boolean isTwoFingerGesture = false;
    private boolean hasTriggeredTwoFingerGesture = false;
    private float startY1, startY2;
    private static final int SWIPE_THRESHOLD = 100;

    public GestureManager(Context context, GestureListener listener) {
        this.listener = listener;
        this.detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (listener != null)
                    listener.onLongPress(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (listener != null)
                    listener.onDoubleTap();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null)
                    return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            if (listener != null)
                                listener.onSwipeDown();
                        } else {
                            if (listener != null)
                                listener.onSwipeUp();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    isTwoFingerGesture = true;
                    hasTriggeredTwoFingerGesture = false;
                    startY1 = event.getY(0);
                    startY2 = event.getY(1);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (isTwoFingerGesture && !hasTriggeredTwoFingerGesture && event.getPointerCount() >= 2) {
                    float currY1 = event.getY(0);
                    float currY2 = event.getY(1);
                    
                    float diffY1 = currY1 - startY1;
                    float diffY2 = currY2 - startY2;
                    
                    if (diffY1 > SWIPE_THRESHOLD && diffY2 > SWIPE_THRESHOLD) {
                        if (listener != null) {
                            listener.onTwoFingerSwipeDown();
                            hasTriggeredTwoFingerGesture = true;
                            return true;
                        }
                    }
                }
                break;
                
            case MotionEvent.ACTION_POINTER_UP:
                // Only reset if we drop below 2 fingers
                if (event.getPointerCount() <= 2) {
                    isTwoFingerGesture = false;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTwoFingerGesture = false;
                hasTriggeredTwoFingerGesture = false;
                break;
        }
        
        return detector.onTouchEvent(event);
    }
}

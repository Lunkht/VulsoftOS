package com.vulsoft.vulsoftos;

import android.view.View;
import androidx.viewpager2.widget.ViewPager2;

public class AnimationTransformerFactory {

    public static ViewPager2.PageTransformer getTransformer(String type) {
        switch (type) {
            case "zoom":
                return new ZoomOutPageTransformer();
            case "depth":
                return new DepthPageTransformer();
            case "cube":
                return new CubeOutTransformer();
            case "flip":
                return new FlipTransformer();
            case "rotate":
                return new RotateTransformer();
            default:
                return null; // Default behavior
        }
    }

    private static void resetPage(View page) {
        page.setAlpha(1f);
        page.setTranslationX(0f);
        page.setTranslationY(0f);
        page.setScaleX(1f);
        page.setScaleY(1f);
        page.setRotation(0f);
        page.setRotationX(0f);
        page.setRotationY(0f);
        page.setPivotX(page.getWidth() / 2f);
        page.setPivotY(page.getHeight() / 2f);
        page.setVisibility(View.VISIBLE);
        float density = page.getResources().getDisplayMetrics().density;
        page.setCameraDistance(1280 * density);
    }

    public static class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            resetPage(view);
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                        (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    public static class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            resetPage(view);
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    public static class CubeOutTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View view, float position) {
            resetPage(view);
            if (position < -1){    // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 0){    // [-1,0]
                view.setAlpha(1);
                view.setPivotX(view.getWidth());
                view.setRotationY(-90 * Math.abs(position));

            } else if (position <= 1){    // (0,1]
                view.setAlpha(1);
                view.setPivotX(0);
                view.setRotationY(90 * Math.abs(position));

            } else {    // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public static class FlipTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            resetPage(page);
            page.setTranslationX(-position * page.getWidth());
            // page.setCameraDistance(12000); // Already set in resetPage with density aware value

            if (position < 0.5 && position > -0.5) {
                page.setVisibility(View.VISIBLE);
            } else {
                page.setVisibility(View.INVISIBLE);
            }

            if (position < -1) {     // [-Infinity,-1)
                page.setAlpha(0);
            } else if (position <= 0) {    // [-1,0]
                page.setAlpha(1);
                page.setRotationY(180 * (1 - Math.abs(position) + 1));
            } else if (position <= 1) {    // (0,1]
                page.setAlpha(1);
                page.setRotationY(-180 * (1 - Math.abs(position) + 1));
            } else {    // (1,+Infinity]
                page.setAlpha(0);
            }
        }
    }

    public static class RotateTransformer implements ViewPager2.PageTransformer {
        @Override
        public void transformPage(View page, float position) {
            resetPage(page);
            if (position < -1) {
                page.setAlpha(0);
            } else if (position <= 0) {
                page.setAlpha(1);
                page.setPivotX(page.getWidth() * 0.5f);
                page.setPivotY(page.getHeight());
                page.setRotation(-15 * Math.abs(position));
            } else if (position <= 1) {
                page.setAlpha(1);
                page.setPivotX(page.getWidth() * 0.5f);
                page.setPivotY(page.getHeight());
                page.setRotation(15 * Math.abs(position));
            } else {
                page.setAlpha(0);
            }
        }
    }
}

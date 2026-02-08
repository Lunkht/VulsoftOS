package com.vulsoft.vulsoftos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.content.res.Configuration;
import android.view.Surface;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.animation.LinearInterpolator;

import androidx.core.app.NotificationCompat;

public class DynamicIslandService extends Service {

    private WindowManager windowManager;
    private View dynamicIslandView;
    private WindowManager.LayoutParams params;
    private static final String CHANNEL_ID = "DynamicIslandChannel";
    private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable collapseRunnable = this::collapseIsland;

    private android.animation.ValueAnimator currentAnimator;
    private String currentNotificationPackage;

    private android.content.BroadcastReceiver systemReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                showCharging(context);
            }
        }
    };

    private android.content.BroadcastReceiver notificationReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW".equals(action)) {
                String pkg = intent.getStringExtra("package");
                String title = intent.getStringExtra("title");
                String text = intent.getStringExtra("text");
                showNotification(pkg, title, text);
            } else if ("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW_APPS".equals(action)) {
                java.util.ArrayList<String> packages = intent.getStringArrayListExtra("packages");
                if (packages != null) {
                    showApps(packages);
                }
            } else if ("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_MEDIA_CONTROL".equals(action)) {
                android.media.session.MediaSession.Token token = intent.getParcelableExtra("token");
                String pkg = intent.getStringExtra("package");
                handleMediaControl(token, pkg);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW_APPS".equals(action)) {
                java.util.ArrayList<String> packages = intent.getStringArrayListExtra("packages");
                if (packages != null) {
                    showApps(packages);
                }
            } else if ("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW".equals(action)) {
                String pkg = intent.getStringExtra("package");
                String title = intent.getStringExtra("title");
                String text = intent.getStringExtra("text");
                showNotification(pkg, title, text);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Register Internal Receiver (Secure)
        android.content.IntentFilter internalFilter = new android.content.IntentFilter();
        internalFilter.addAction("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW");
        internalFilter.addAction("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW_APPS");
        internalFilter.addAction("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_MEDIA_CONTROL");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, internalFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, internalFilter);
        }
        
        // Register System Receiver (Exported for System Events)
        android.content.IntentFilter systemFilter = new android.content.IntentFilter();
        systemFilter.addAction(Intent.ACTION_POWER_CONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(systemReceiver, systemFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(systemReceiver, systemFilter);
        }

        // Create Notification Channel for Foreground Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Dynamic Island Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Start Foreground Service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Dynamic Island")
                .setContentText("Active")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use existing icon, ensure it exists or use default
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(2, notification);
        }

        // Initialize WindowManager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Create the view
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dynamicIslandView = inflater.inflate(R.layout.layout_dynamic_island, null);
        
        applyStyle();
        
        // Add click listener
        dynamicIslandView.setOnClickListener(v -> {
            if (currentNotificationPackage != null) {
                try {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentNotificationPackage);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                        // Optional: collapse immediately on click
                        collapseIsland();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        // Define Layout Parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                dpToPx(120), // Width from xml
                dpToPx(36),  // Height from xml
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, // Allow touches outside to pass through
                PixelFormat.TRANSLUCENT
        );

        // Initial Layout Update
        updateLayout();

        // Add view to window manager
        try {
            windowManager.addView(dynamicIslandView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    private void updateLayout() {
        if (dynamicIslandView == null || windowManager == null || params == null) return;
        
        applyStyle();
        
        boolean isExpanded = dynamicIslandView.getTag() != null && (boolean) dynamicIslandView.getTag();
        
        if (isExpanded) {
             dynamicIslandView.setTag(false); 
             
             // Hide content
             android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
             android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
             if (iconView != null) iconView.setVisibility(View.GONE);
             if (textView != null) textView.setVisibility(View.GONE);
        }

        int rotation = windowManager.getDefaultDisplay().getRotation();
        IslandDimensions dims = getDimensionsForRotation(rotation);

        params.width = dims.width;
        params.height = dims.height;
        params.gravity = dims.gravity;
        params.x = dims.x;
        params.y = dims.y;

        if (dynamicIslandView.isAttachedToWindow()) {
            try {
                windowManager.updateViewLayout(dynamicIslandView, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void applyStyle() {
        if (dynamicIslandView == null) return;
        
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        String style = prefs.getString("dynamic_island_style", "default");
        
        int bgResId;
        int textColor = 0xFFFFFFFF; // White default
        
        switch (style) {
            case "glass_dark":
                bgResId = R.drawable.bg_dynamic_island_glass_dark;
                break;
            case "glass_blur":
                bgResId = R.drawable.bg_dynamic_island_glass_blur;
                textColor = 0xFF000000;
                break;
            case "liquid_blue":
                bgResId = R.drawable.bg_dynamic_island_liquid_blue;
                break;
            default:
                bgResId = R.drawable.bg_dynamic_island;
                break;
        }
        
        dynamicIslandView.setBackgroundResource(bgResId);
        
        android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
        if (textView != null) {
            textView.setTextColor(textColor);
        }
    }

    private static class IslandDimensions {
        int width, height, gravity, x, y;
    }
    
    private IslandDimensions getDimensionsForRotation(int rotation) {
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        int yOffsetDp = prefs.getInt("dynamic_island_y_offset", 0);
        int yOffsetPx = dpToPx(yOffsetDp);

        IslandDimensions d = new IslandDimensions();
        if (rotation == Surface.ROTATION_90) {
            // Landscape, Notch on Left
            d.width = dpToPx(36);
            d.height = dpToPx(120);
            d.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            d.x = dpToPx(6) + yOffsetPx;
            d.y = 0;
        } else if (rotation == Surface.ROTATION_270) {
            // Landscape, Notch on Right
            d.width = dpToPx(36);
            d.height = dpToPx(120);
            d.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
            d.x = dpToPx(6) + yOffsetPx;
            d.y = 0;
        } else {
            // Portrait (0 or 180)
            d.width = dpToPx(120);
            d.height = dpToPx(36);
            d.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            d.x = 0;
            d.y = dpToPx(6) + yOffsetPx;
        }
        return d;
    }

    private void showNotification(String pkg, String title, String text) {
        if (dynamicIslandView == null) return;
        
        this.currentNotificationPackage = pkg;
        
        // Ensure FLAG_NOT_FOCUSABLE is KEPT so back button works for underlying apps
        // We do NOT remove FLAG_NOT_FOCUSABLE here.
        // params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        
        // Keep NOT_TOUCH_MODAL so outside touches go to other apps
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL; 
        
        try {
            windowManager.updateViewLayout(dynamicIslandView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Cancel any pending collapse
        handler.removeCallbacks(collapseRunnable);

        android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
        android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
        
        if (iconView != null && textView != null) {
            try {
                android.content.pm.PackageManager pm = getPackageManager();
                android.graphics.drawable.Drawable icon = pm.getApplicationIcon(pkg);
                iconView.setImageDrawable(icon);
            } catch (Exception e) {
                iconView.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            textView.setText(title != null ? title : (text != null ? text : "Notification"));
            
            iconView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
        }
        
        // Expand Animation
        // Calculate target Y (lower in portrait to avoid status bar overlap)
        int rotation = windowManager.getDefaultDisplay().getRotation();
        IslandDimensions dims = getDimensionsForRotation(rotation);
        int targetY = dims.y;
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            targetY = dims.y + dpToPx(24); // Expand downwards from current Y
        }

        animateSize(dpToPx(320), dpToPx(80), targetY); // Expand to wider and taller
        dynamicIslandView.setTag(true); // Mark as expanded

        // Schedule collapse
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        int duration = prefs.getInt("dynamic_island_duration", 4000);
        handler.postDelayed(collapseRunnable, duration);
    }
    
    private void showApps(java.util.ArrayList<String> packages) {
        if (dynamicIslandView == null) {
             android.widget.Toast.makeText(this, "Erreur: Vue non initialisée", android.widget.Toast.LENGTH_SHORT).show();
             return;
        }
        
        // Enable touch events
        // params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        
        try {
            windowManager.updateViewLayout(dynamicIslandView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Cancel collapse
        handler.removeCallbacks(collapseRunnable);
        
        // Hide regular notification content
        android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
        android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
        if (iconView != null) iconView.setVisibility(View.GONE);
        if (textView != null) textView.setVisibility(View.GONE);
        
        // Show Apps Container
        android.widget.LinearLayout appsContainer = dynamicIslandView.findViewById(R.id.islandAppsContainer);
        if (appsContainer != null) {
            appsContainer.setVisibility(View.VISIBLE);
            appsContainer.removeAllViews(); // Clear previous
            
            android.content.pm.PackageManager pm = getPackageManager();
            // Increase icon size for better visibility
            int iconSize = dpToPx(48); 
            // Adjust margins for balanced spacing
            int margin = dpToPx(6);
            
            for (String pkg : packages) {
                try {
                    android.graphics.drawable.Drawable icon = pm.getApplicationIcon(pkg);
                    android.widget.ImageView img = new android.widget.ImageView(this);
                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(iconSize, iconSize);
                    lp.setMargins(margin, 0, margin, 0);
                    img.setLayoutParams(lp);
                    img.setImageDrawable(icon);
                    img.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                    img.setOnClickListener(v -> {
                        Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launchIntent);
                            collapseIsland();
                        }
                    });
                    appsContainer.addView(img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Expand Animation (Adjust width to fit content: 4 * (48 + 12) + 24 padding = ~264 -> 280dp for safe measure)
        // Height: 48 + 24 = 72 -> 84dp
        
        // Calculate target Y (lower in portrait to avoid status bar overlap)
        int rotation = windowManager.getDefaultDisplay().getRotation();
        IslandDimensions dims = getDimensionsForRotation(rotation);
        int targetY = dims.y;
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            targetY = dims.y + dpToPx(24); // Expand downwards from current Y
        }
        
        animateSize(dpToPx(280), dpToPx(84), targetY);
        dynamicIslandView.setTag(true);
        
        // Schedule collapse
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        int duration = prefs.getInt("dynamic_island_duration", 4000);
        handler.postDelayed(collapseRunnable, duration + 1000);
    }

    private void collapseIsland() {
         if (dynamicIslandView == null) return;
         
         params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
         
         android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
         android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
         
         if (iconView != null) iconView.setVisibility(View.GONE);
         if (textView != null) textView.setVisibility(View.GONE);
         
         // Also hide apps container
         View appsContainer = dynamicIslandView.findViewById(R.id.islandAppsContainer);
         if (appsContainer != null) appsContainer.setVisibility(View.GONE);
         
         // Hide Battery and Visualizer
         View batteryBar = dynamicIslandView.findViewById(R.id.islandBatteryBar);
         if (batteryBar != null) batteryBar.setVisibility(View.GONE);
         
         View visualizer = dynamicIslandView.findViewById(R.id.islandVisualizer);
         if (visualizer != null) {
             visualizer.setVisibility(View.GONE);
             if (visualizer instanceof android.view.ViewGroup) {
                 android.view.ViewGroup group = (android.view.ViewGroup) visualizer;
                 for (int i = 0; i < group.getChildCount(); i++) {
                     group.getChildAt(i).clearAnimation();
                 }
             }
         }
         
         // Animate back to default size based on rotation
         int rotation = windowManager.getDefaultDisplay().getRotation();
         IslandDimensions dims = getDimensionsForRotation(rotation);
         
         animateSize(dims.width, dims.height, dims.y);
         dynamicIslandView.setTag(false);
    }
    
    private void animateSize(int targetWidth, int targetHeight, int targetY) {
        if (params == null || windowManager == null) return;
        
        if (currentAnimator != null && currentAnimator.isRunning()) {
            currentAnimator.cancel();
        }
        
        int startWidth = params.width;
        int startHeight = params.height;
        int startY = params.y;
        
        currentAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        currentAnimator.setDuration(300); // 300ms duration
        currentAnimator.setInterpolator(new android.view.animation.OvershootInterpolator(1.2f)); // Bouncy effect
        
        currentAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            
            params.width = (int) (startWidth + (targetWidth - startWidth) * fraction);
            params.height = (int) (startHeight + (targetHeight - startHeight) * fraction);
            params.y = (int) (startY + (targetY - startY) * fraction);
            
            try {
                windowManager.updateViewLayout(dynamicIslandView, params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        currentAnimator.start();
    }

    private android.media.session.MediaController mediaController;
    private android.media.session.MediaController.Callback mediaCallback = new android.media.session.MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(android.media.session.PlaybackState state) {
            updateMediaUI();
        }
        @Override
        public void onMetadataChanged(android.media.MediaMetadata metadata) {
            updateMediaUI();
        }
    };

    private void handleMediaControl(android.media.session.MediaSession.Token token, String pkg) {
        if (token == null) return;
        
        // Clean up old controller
        if (mediaController != null) {
            mediaController.unregisterCallback(mediaCallback);
        }
        
        try {
            mediaController = new android.media.session.MediaController(this, token);
            mediaController.registerCallback(mediaCallback);
            this.currentNotificationPackage = pkg; // Update current package for click
            updateMediaUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMediaUI() {
        if (mediaController == null || dynamicIslandView == null) return;
        
        android.media.MediaMetadata metadata = mediaController.getMetadata();
        android.media.session.PlaybackState state = mediaController.getPlaybackState();
        
        if (metadata == null || state == null) return;
        
        boolean isPlaying = state.getState() == android.media.session.PlaybackState.STATE_PLAYING || 
                            state.getState() == android.media.session.PlaybackState.STATE_BUFFERING;
                            
        if (!isPlaying) {
             return;
        }
        
        // Cancel collapse since we are playing
        handler.removeCallbacks(collapseRunnable);
        
        // Enable touch
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        
        try {
            windowManager.updateViewLayout(dynamicIslandView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Update UI Content
        android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
        android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
        View appsContainer = dynamicIslandView.findViewById(R.id.islandAppsContainer);
        if (appsContainer != null) appsContainer.setVisibility(View.GONE);
        
        if (iconView != null && textView != null) {
            // Art
            android.graphics.Bitmap art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) {
                art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART);
            }
            
            if (art != null) {
                iconView.setImageBitmap(art);
            } else {
                try {
                    android.graphics.drawable.Drawable appIcon = getPackageManager().getApplicationIcon(currentNotificationPackage);
                    iconView.setImageDrawable(appIcon);
                } catch (Exception e) {
                    iconView.setImageResource(R.drawable.ic_launcher_foreground);
                }
            }
            iconView.setColorFilter(null);
            
            // Text
            String title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE);
            String artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST);
            String text = (title != null ? title : "") + (artist != null ? " • " + artist : "");
            
            textView.setText(text);
            textView.setTextColor(android.graphics.Color.WHITE);
            textView.setSelected(true);
            
            iconView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            
            // Show Visualizer
            View visualizer = dynamicIslandView.findViewById(R.id.islandVisualizer);
            if (visualizer != null) {
                visualizer.setVisibility(View.VISIBLE);
                animateVisualizer(visualizer);
            }
        }
        
        // Expand for Media
        int rotation = windowManager.getDefaultDisplay().getRotation();
        IslandDimensions dims = getDimensionsForRotation(rotation);
        int targetY = dims.y;
         if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            targetY = dpToPx(30);
        }
        
        if (!Boolean.TRUE.equals(dynamicIslandView.getTag())) {
             animateSize(dpToPx(240), dpToPx(48), targetY);
             dynamicIslandView.setTag(true);
        }
    }

    private void showCharging(Context context) {
        if (dynamicIslandView == null) return;
        
        // Enable touch events
        params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        
        try {
            windowManager.updateViewLayout(dynamicIslandView, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Cancel collapse
        handler.removeCallbacks(collapseRunnable);
        
        // Hide apps container
        View appsContainer = dynamicIslandView.findViewById(R.id.islandAppsContainer);
        if (appsContainer != null) appsContainer.setVisibility(View.GONE);
        
        // Show charging content
        android.widget.ImageView iconView = dynamicIslandView.findViewById(R.id.islandIcon);
        android.widget.TextView textView = dynamicIslandView.findViewById(R.id.islandText);
        
        if (iconView != null && textView != null) {
            iconView.setImageResource(android.R.drawable.ic_lock_idle_charging); // Use system charging icon
            if (iconView.getDrawable() == null) {
                 iconView.setImageResource(R.drawable.ic_launcher_foreground); // Fallback
            }
            iconView.setColorFilter(android.graphics.Color.GREEN); // Make it green
            
            // Get Battery Level
            android.os.BatteryManager bm = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            
            textView.setText("En charge " + batteryLevel + "%");
            textView.setTextColor(android.graphics.Color.GREEN);
            
            iconView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            
            android.widget.ProgressBar batteryBar = dynamicIslandView.findViewById(R.id.islandBatteryBar);
            if (batteryBar != null) {
                batteryBar.setProgress(batteryLevel);
                batteryBar.setVisibility(View.VISIBLE);
            }
        }
        
        // Expand Animation
        // Calculate target Y (lower in portrait to avoid status bar overlap)
        int rotation = windowManager.getDefaultDisplay().getRotation();
        IslandDimensions dims = getDimensionsForRotation(rotation);
        int targetY = dims.y;
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            targetY = dpToPx(30); // Move down to 30dp in portrait
        }
        
        animateSize(dpToPx(200), dpToPx(50), targetY); // Smaller pill for charging
        dynamicIslandView.setTag(true);
        
        // Schedule collapse
        android.content.SharedPreferences prefs = getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
        int duration = prefs.getInt("dynamic_island_duration", 4000);
        handler.postDelayed(collapseRunnable, duration);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            unregisterReceiver(notificationReceiver);
        }
        if (systemReceiver != null) {
            unregisterReceiver(systemReceiver);
        }
        if (dynamicIslandView != null && windowManager != null) {
            try {
                windowManager.removeView(dynamicIslandView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void animateVisualizer(View container) {
        if (!(container instanceof android.view.ViewGroup)) return;
        android.view.ViewGroup group = (android.view.ViewGroup) container;
        
        for (int i = 0; i < group.getChildCount(); i++) {
            View bar = group.getChildAt(i);
            bar.setPivotY(bar.getHeight()); // Scale from bottom
            
            ObjectAnimator anim = ObjectAnimator.ofFloat(bar, "scaleY", 0.3f, 1.0f);
            anim.setDuration(200 + (long)(Math.random() * 300));
            anim.setRepeatCount(ObjectAnimator.INFINITE);
            anim.setRepeatMode(ObjectAnimator.REVERSE);
            anim.setInterpolator(new LinearInterpolator());
            anim.start();
        }
    }
}
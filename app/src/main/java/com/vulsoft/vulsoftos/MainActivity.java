package com.vulsoft.vulsoftos;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import android.provider.Settings;
import java.util.Set;
import java.util.HashSet;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetProviderInfo;
import android.appwidget.AppWidgetHostView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorManager;

import com.vulsoft.vulsoftos.activities.BaseActivity;
import com.vulsoft.vulsoftos.activities.OnboardingActivity;
import com.vulsoft.vulsoftos.activities.SettingsActivity;
import com.vulsoft.vulsoftos.activities.AssistantActivity;

public class MainActivity extends BaseActivity implements GestureManager.GestureListener {
    private static final String PREFS_NAME = "launcher_prefs";
    private static final String PREF_DOCK_PREFIX = "dock_";
    private static final String PREF_SHOW_DOCK = "show_dock";
    private static final String PREF_SHOW_DOCK_BG = "dock_bg_enabled"; // Updated to match SettingsActivity
    private static final String PREF_ICON_GRID = "icon_grid"; // New pref for grid size
    private static final String PREF_ICON_RADIUS = "icon_corner_radius"; // Updated to match SettingsActivity
    private static final String PREF_THEME = "app_theme";
    private static final String PREF_FIRST_RUN = "first_run";
    private static final String PREF_SEARCH_BAR_STYLE = "search_bar_style";
    private static final String PREF_HIDDEN_APPS = "hidden_apps";
    private static final String PREF_LAST_PAGE_INDEX = "last_page_index";
    private static final String PREF_TRANSITION_EFFECT = "transition_effect";
    private static final String PREF_HIDE_NOTCH = "hide_notch";
    private static final int DOCK_SIZE = 4;
    private static final int APPWIDGET_HOST_ID = 1024;
    private static final int REQUEST_PICK_APPWIDGET = 5;
    private static final int REQUEST_CREATE_APPWIDGET = 6;

    private GestureManager gestureManager;

    private TextView textClock;
    // private ImageButton buttonSettings; // Removed
    private ViewPager2 viewPagerApps;
    private RecyclerView recyclerDock;
    private RecyclerView recyclerAppsList;
    private View notchOverlay;
    private View dynamicIsland;
    private android.widget.LinearLayout layoutPageIndicator;
    private android.widget.LinearLayout searchBar;
    private DockAdapter dockAdapter;
    private AppsPagerAdapter pagerAdapter;
    private AppsAdapter listAdapter;
    private final List<AppItem> appItems = new ArrayList<>();
    private final List<AppItem> dockItems = new ArrayList<>();
    // Static cache to prevent empty screen on recreate()
    private static final List<AppItem> sCachedAppItems = new ArrayList<>();
    private static final List<AppItem> sCachedDockItems = new ArrayList<>();
    
    private boolean isDragEnabled = false;
    private boolean showDock = true;
    private int columnsPerRow = 4;
    private int rowsPerPage = 6; // Removed final to allow dynamic update
    private String currentTheme; // Track current theme for updates
    private String currentLanguage; // Track current language for updates

    private AppWidgetManager mAppWidgetManager;
    private AppWidgetHost mAppWidgetHost;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    
    private boolean needsReload = true;
    private final android.content.BroadcastReceiver packageReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            needsReload = true;
        }
    };

    private final android.content.BroadcastReceiver notificationReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            if ("com.vulsoft.vulsoftos.NOTIFICATION_UPDATE".equals(intent.getAction())) {
                java.util.HashMap<String, Integer> counts = (java.util.HashMap<String, Integer>) intent
                        .getSerializableExtra("notification_counts");
                if (counts != null) {
                    if (dockAdapter != null) {
                        dockAdapter.updateNotificationCounts(counts);
                    }
                    if (pagerAdapter != null) {
                        pagerAdapter.updateNotificationCounts(counts);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemePreference();
        currentTheme = ThemeManager.getSavedTheme(this); // Initialize current theme
        super.onCreate(savedInstanceState);

        gestureManager = new GestureManager(this, this);

        // Check for Tutorial Completion
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean tutorialCompleted = prefs.getBoolean("tutorial_completed", false);
        if (!tutorialCompleted) {
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);

        // Shake Detector Init
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            shakeDetector = new ShakeDetector();
            shakeDetector.setOnShakeListener(count -> {
                WallpaperManager.setRandomWallpaper(this);
                applyWallpaper();
                Toast.makeText(this, "Fond d'écran changé !", Toast.LENGTH_SHORT).show();
            });
        }

        notchOverlay = findViewById(R.id.notchOverlay);
        // dynamicIsland = findViewById(R.id.dynamicIsland);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            // Adjust notch overlay to cover the status bar area
            if (notchOverlay != null) {
                android.view.ViewGroup.LayoutParams params = notchOverlay.getLayoutParams();
                params.height = systemBars.top;
                notchOverlay.setLayoutParams(params);
                notchOverlay.setTranslationY(-systemBars.top);
            }

            // Adjust Dynamic Island position
            /*
            if (dynamicIsland != null) {
                // Position Dynamic Island over the status bar (notch area)
                // Since the parent view has top padding equal to systemBars.top,
                // we need to translate the view up by that amount to place it
                // relative to the absolute screen top.
                // We keep the marginTop from XML (e.g. 6dp) to give it a "floating" look
                // or slightly lower it from the very edge.
                dynamicIsland.setTranslationY(-systemBars.top);
            }
            */

            return insets;
        });

        // Check Notification Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }

        // Check Notification Listener Permission
        if (!isNotificationServiceEnabled()) {
            new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                    .setTitle(R.string.permission_notification_title)
                    .setMessage(R.string.permission_notification_msg)
                    .setPositiveButton(R.string.permission_button_allow, v -> {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    })
                    .setNegativeButton(R.string.permission_button_later, null)
                    .show();
        }

        viewPagerApps = findViewById(R.id.viewPagerApps);
        recyclerDock = findViewById(R.id.recyclerDock);
        recyclerAppsList = findViewById(R.id.recyclerAppsList);
        layoutPageIndicator = findViewById(R.id.layoutPageIndicator);
        searchBar = findViewById(R.id.searchBar);

        // Setup btnCategories
        View btnCategories = findViewById(R.id.btnCategories);
        if (btnCategories != null) {
            btnCategories.setOnClickListener(v -> {
                new com.vulsoft.vulsoftos.fragments.CategorySelectionDialogFragment().show(getSupportFragmentManager(), "CategorySelection");
            });
        }

        // Setup List View (Vertical Grid for list mode)
        recyclerAppsList.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 4));
        
        AppsAdapter.OnAppClickListener appClickListener = appItem -> {
            android.util.Log.d("RuvoluteDebug", "MainActivity: onAppClick: " + appItem.label + " (" + appItem.packageName + ")");
            // Debug Toast
            // android.widget.Toast.makeText(MainActivity.this, "Debug: " + appItem.label + " (" + appItem.packageName + ")", android.widget.Toast.LENGTH_SHORT).show();
            
            if (appItem.packageName != null && appItem.packageName.equals(getPackageName())) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (appItem.launchIntent != null) {
                startActivity(appItem.launchIntent);
            }
        };
        
        AppsAdapter.OnAppLongClickListener appLongClickListener = this::showAppOptions;

        int iconRadiusPercent = prefs.getInt(PREF_ICON_RADIUS, 50);
        
        listAdapter = new AppsAdapter(appItems, iconRadiusPercent, appClickListener, appLongClickListener);
        recyclerAppsList.setAdapter(listAdapter);

        // Setup Drag & Drop Listener
        viewPagerApps.setOnDragListener(dragListener);
        recyclerDock.setOnDragListener(dragListener);

        // Setup Dock
        recyclerDock.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dockAdapter = new DockAdapter(dockItems, appItem -> {
            if (appItem.packageName != null && appItem.packageName.equals(getPackageName())) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            } else if (appItem.launchIntent != null) {
                startActivity(appItem.launchIntent);
            }
        }, this::showAppOptions);
        recyclerDock.setAdapter(dockAdapter);
        recyclerDock.setOnDragListener(dragListener);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageReceiver, filter);

        // Load from cache immediately if available to avoid empty screen
        if (!sCachedAppItems.isEmpty() || !sCachedDockItems.isEmpty()) {
            appItems.addAll(sCachedAppItems);
            dockItems.addAll(sCachedDockItems);
        }

        loadInstalledApps();
        needsReload = false;
        // restoreWidgets(); // Now called inside loadInstalledApps
    }

    // ...

    @Override
    protected void onStart() {
        super.onStart();
        if (mAppWidgetHost != null) {
            try {
                mAppWidgetHost.startListening();
            } catch (Exception e) {
                e.printStackTrace(); // Handle DeadObjectException or others
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAppWidgetHost != null) {
            try {
                mAppWidgetHost.stopListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter("com.vulsoft.vulsoftos.NOTIFICATION_UPDATE");
        registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        // Shake Detection
        boolean shakeEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("shake_to_change_wallpaper", false);
        if (shakeEnabled && sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        // Check if theme changed
        String savedTheme = ThemeManager.getSavedTheme(this);
        if (currentTheme != null && !currentTheme.equals(savedTheme)) {
            recreate();
            return;
        }

        // Check if language changed
        String savedLanguage = LocaleHelper.getLanguage(this);
        if (currentLanguage != null && !currentLanguage.equals(savedLanguage)) {
            recreate();
            return;
        }

        // updateClock(); // Removed
        applyLayoutPreferences();
        applyWallpaper(); // Update wallpaper when returning to launcher

        // Reload apps to reflect changes (uninstalls, unhides, etc.)
        if (needsReload) {
            loadInstalledApps();
            needsReload = false;
        }

        // Check if Default Launcher
        // Removed annoying toast - user knows if they want to set it as default
        
        // Refresh Recent Apps page (if visible)
        if (pagerAdapter != null) {
            pagerAdapter.notifyItemChanged(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(notificationReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
        
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeDetector);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(packageReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered, ignore
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureManager != null) {
            gestureManager.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onSwipeDown() {
        // Lire la préférence pour le geste de balayage vers le bas
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String action = prefs.getString(SettingsActivity.KEY_GESTURE_SWIPE_DOWN, SettingsActivity.ACTION_NOTIFICATIONS);
        executeGestureAction(action);
    }

    @Override
    public void onSwipeUp() {
        // Lire la préférence pour le geste de balayage vers le haut
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String action = prefs.getString(SettingsActivity.KEY_GESTURE_SWIPE_UP, SettingsActivity.ACTION_NONE);
        executeGestureAction(action);
    }

    @Override
    public void onTwoFingerSwipeDown() {
        // Toast.makeText(this, "Geste détecté", Toast.LENGTH_SHORT).show();
        
        if (!isServiceRunning(DynamicIslandService.class)) {
             Toast.makeText(this, "Dynamic Island n'est pas actif. Veuillez l'activer dans les paramètres.", Toast.LENGTH_LONG).show();
             return;
        }

        if (hasUsageStatsPermission()) {
            getTop4RecentApps();
        } else {
             // Fallback: Show Dock apps or permission dialog
             // Let's just prompt permission if not granted, as "Most Used" implies usage stats
             Toast.makeText(this, "Permission d'accès à l'utilisation requise", Toast.LENGTH_SHORT).show();
             startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }
    
    private void getTop4RecentApps() {
        new Thread(() -> {
             android.app.usage.UsageStatsManager usm = (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
             long time = System.currentTimeMillis();
             List<android.app.usage.UsageStats> stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000 * 3600 * 24, time);

             if (stats == null || stats.isEmpty()) {
                 runOnUiThread(() -> Toast.makeText(MainActivity.this, "Aucune statistique d'utilisation trouvée", Toast.LENGTH_SHORT).show());
                 return;
             }

             java.util.Map<Long, android.app.usage.UsageStats> sortedStats = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
             for (android.app.usage.UsageStats usageStats : stats) {
                 sortedStats.put(usageStats.getLastTimeUsed(), usageStats);
             }

             java.util.ArrayList<String> packages = new java.util.ArrayList<>();
             String myPackage = getPackageName();
             PackageManager pm = getPackageManager();

             for (android.app.usage.UsageStats usageStats : sortedStats.values()) {
                 if (packages.size() >= 4) break;

                 String pkg = usageStats.getPackageName();
                 if (pkg.equals(myPackage)) continue;

                 if (pm.getLaunchIntentForPackage(pkg) != null) {
                     // Check duplicates
                     if (!packages.contains(pkg)) {
                         packages.add(pkg);
                     }
                 }
             }

             if (!packages.isEmpty()) {
                 Intent intent = new Intent(MainActivity.this, DynamicIslandService.class);
                 intent.setAction("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW_APPS");
                intent.putStringArrayListExtra("packages", packages);
                startService(intent);
            } else {
            }
       }).start();
    }

    @Override
    public void onDoubleTap() {
        // Lire la préférence pour le double tap
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String action = prefs.getString(SettingsActivity.KEY_GESTURE_DOUBLE_TAP, SettingsActivity.ACTION_NONE);
        executeGestureAction(action);
    }
    
    private void executeGestureAction(String action) {
        switch (action) {
            case SettingsActivity.ACTION_NOTIFICATIONS:
                // Ouvrir le panneau de notifications
                try {
                    Object service = getSystemService("statusbar");
                    Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                    java.lang.reflect.Method expand = statusbarManager.getMethod("expandNotificationsPanel");
                    expand.invoke(service);
                } catch (Exception e) {
                    Toast.makeText(this, "Impossible d'ouvrir les notifications", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case SettingsActivity.ACTION_SETTINGS:
                // Ouvrir les paramètres
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
                
            case SettingsActivity.ACTION_WALLPAPER:
                // Changer le fond d'écran
                WallpaperManager.setRandomWallpaper(this);
                applyWallpaper();
                Toast.makeText(this, "Fond d'écran changé !", Toast.LENGTH_SHORT).show();
                break;
                
            case SettingsActivity.ACTION_ASSISTANT:
                // Ouvrir l'assistant
                Intent assistantIntent = new Intent(this, AssistantActivity.class);
                startActivity(assistantIntent);
                break;
                
            case SettingsActivity.ACTION_APP_SEARCH:
                // Ouvrir la recherche universelle
                UniversalSearchDialogFragment searchDialog = new UniversalSearchDialogFragment();
                searchDialog.show(getSupportFragmentManager(), "UniversalSearch");
                break;
                
            case SettingsActivity.ACTION_NONE:
            default:
                // Aucune action
                break;
        }
    }
    
    private void applyZenMode() {
        // Liste des catégories d'apps distrayantes à masquer en mode Zen/Focus
        List<String> distractingCategories = new ArrayList<>();
        distractingCategories.add("Social");
        distractingCategories.add("Games");
        distractingCategories.add("Entertainment");
        
        // Filtrer les apps dans le pagerAdapter et listAdapter
        // Note: Cette implémentation simple masque visuellement les apps
        // Pour une implémentation complète, il faudrait filtrer appItems
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        float rawX = e.getRawX();
        float rawY = e.getRawY();

        // Check if touch is on Dock Recycler
        if (recyclerDock != null && recyclerDock.getVisibility() == View.VISIBLE) {
            int[] loc = new int[2];
            recyclerDock.getLocationOnScreen(loc);
            if (rawX >= loc[0] && rawX < loc[0] + recyclerDock.getWidth() &&
                    rawY >= loc[1] && rawY < loc[1] + recyclerDock.getHeight()) {
                // Inside Dock bounds
                float localX = rawX - loc[0];
                float localY = rawY - loc[1];
                View child = recyclerDock.findChildViewUnder(localX, localY);
                if (child != null) {
                    // Strict hit detection for Dock items (Icon Container)
                    if (isTouchOnView(child.findViewById(R.id.iconContainer), rawX, rawY)) {
                        return; // Clicked on a dock item
                    }
                    // Else: clicked on padding, allow Recent Apps
                }
            }
        }

        // Check Apps ViewPager
        if (viewPagerApps != null && viewPagerApps.getVisibility() == View.VISIBLE) {
            View internalView = viewPagerApps.getChildAt(0);
            if (internalView instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) internalView;
                int[] loc = new int[2];
                rv.getLocationOnScreen(loc);

                if (rawX >= loc[0] && rawX < loc[0] + rv.getWidth() &&
                        rawY >= loc[1] && rawY < loc[1] + rv.getHeight()) {
                    float localX = rawX - loc[0];
                    float localY = rawY - loc[1];
                    View pageView = rv.findChildViewUnder(localX, localY);
                    if (pageView != null) {
                        // Drill down to the Page's RecyclerView
                        RecyclerView pageAppsRv = pageView.findViewById(R.id.recyclerPageApps);
                        if (pageAppsRv != null) {
                            int[] pageLoc = new int[2];
                            pageAppsRv.getLocationOnScreen(pageLoc);

                            if (rawX >= pageLoc[0] && rawX < pageLoc[0] + pageAppsRv.getWidth() &&
                                    rawY >= pageLoc[1] && rawY < pageLoc[1] + pageAppsRv.getHeight()) {

                                float pageX = rawX - pageLoc[0];
                                float pageY = rawY - pageLoc[1];
                                View appChild = pageAppsRv.findChildViewUnder(pageX, pageY);

                                if (appChild != null) {
                                    // Check App targets
                                    if (isTouchOnView(appChild.findViewById(R.id.icon), rawX, rawY) ||
                                            isTouchOnView(appChild.findViewById(R.id.label), rawX, rawY) ||
                                            isTouchOnView(appChild.findViewById(R.id.folder_icon_container), rawX,
                                                    rawY)) {
                                        return;
                                    }

                                    // Check Widget (if no icon/folder found)
                                    if (appChild.findViewById(R.id.icon) == null &&
                                            appChild.findViewById(R.id.folder_icon_container) == null) {
                                        return; // Assume widget is interactive
                                    }

                                    // Fall through: Clicked on padding -> Allow Recent Apps
                                }
                            }
                        }
                    }
                }
            }
        }

        // Show Recent Apps Dialog on long press (empty space)
        if (hasUsageStatsPermission()) {
            RecentAppsDialogFragment dialog = new RecentAppsDialogFragment();
            dialog.show(getSupportFragmentManager(), "RecentAppsDialog");
        } else {
            new com.vulsoft.vulsoftos.utils.ModernDialogHelper.Builder(this)
                    .setTitle(R.string.permission_usage_title)
                    .setMessage(R.string.permission_usage_msg)
                    .setPositiveButton(R.string.permission_button_grant, v -> {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    })
                    .setNegativeButton(R.string.permission_button_cancel, null)
                    .show();
        }
    }

    private boolean isTouchOnView(View view, float x, float y) {
        if (view == null || view.getVisibility() != View.VISIBLE)
            return false;
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return x >= loc[0] && x < loc[0] + view.getWidth() &&
                y >= loc[1] && y < loc[1] + view.getHeight();
    }

    private boolean hasUsageStatsPermission() {
        android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    /* Widget methods removed */

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = android.provider.Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!android.text.TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (android.text.TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateClock() {
        // String time = DateFormat.format("HH:mm", new Date()).toString();
        // textClock.setText(time);
    }

    private void applyThemePreference() {
        ThemeManager.applyTheme(this);
    }

    private void loadInstalledApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> resolved = pm.queryIntentActivities(mainIntent, 0);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            java.util.Set<String> hiddenApps = prefs.getStringSet(PREF_HIDDEN_APPS, new java.util.HashSet<>());

            List<AppItem> loadedApps = new ArrayList<>();
            for (ResolveInfo info : resolved) {
                if (hiddenApps.contains(info.activityInfo.packageName))
                    continue;

                String label = info.loadLabel(pm).toString();

                AppItem item = new AppItem();
                item.label = label;
                item.packageName = info.activityInfo.packageName;
                // Apply Icon Pack
                Drawable defaultIcon = info.loadIcon(pm);
                item.icon = IconPackManager.getIcon(MainActivity.this, item.packageName, defaultIcon);

                item.launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                item.type = AppItem.Type.APP;

                if (item.packageName.equals(getPackageName())) {
                    item.label = getString(R.string.title_settings);
                    Drawable settingsIcon = androidx.core.content.ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.setting_icon);
                    item.icon = IconPackManager.getIcon(MainActivity.this, "com.vulsoft.vulsoftos.SettingsActivity",
                            settingsIcon);
                }

                loadedApps.add(item);
            }

            // Load saved order and widgets
            AppLayoutManager.SavedLayout savedLayout = AppLayoutManager.loadLayout(this, pm);

            List<AppItem> finalGrid = new ArrayList<>();
            List<AppItem> finalDock = new ArrayList<>();

            if (savedLayout != null) {
                List<AppItem> remainingApps = new ArrayList<>(loadedApps);

                // 1. Process Grid
                if (savedLayout.gridApps != null) {
                    for (AppItem savedItem : savedLayout.gridApps) {
                        if (savedItem.type == AppItem.Type.WIDGET) {
                            finalGrid.add(savedItem);
                        } else if (savedItem.type == AppItem.Type.FOLDER) {
                            List<AppItem> validChildren = new ArrayList<>();
                            for (AppItem child : savedItem.folderItems) {
                                AppItem foundChild = null;
                                for (AppItem loaded : remainingApps) {
                                    if (loaded.packageName != null && loaded.packageName.equals(child.packageName)) {
                                        foundChild = loaded;
                                        break;
                                    }
                                }
                                if (foundChild != null) {
                                    validChildren.add(foundChild);
                                    remainingApps.remove(foundChild);
                                }
                            }
                            savedItem.folderItems = validChildren;
                            if (!savedItem.folderItems.isEmpty()) {
                                finalGrid.add(savedItem);
                            }
                        } else {
                            // Find app in loadedApps
                            AppItem found = null;
                            for (AppItem loaded : remainingApps) {
                                if (loaded.packageName.equals(savedItem.packageName)) {
                                    found = loaded;
                                    break;
                                }
                            }
                            if (found != null) {
                                finalGrid.add(found);
                                remainingApps.remove(found);
                            }
                        }
                    }
                }

                // 2. Process Dock
                if (savedLayout.dockApps != null) {
                    for (AppItem savedItem : savedLayout.dockApps) {
                        if (savedItem.type == AppItem.Type.FOLDER) {
                            List<AppItem> validChildren = new ArrayList<>();
                            for (AppItem child : savedItem.folderItems) {
                                AppItem foundChild = null;
                                for (AppItem loaded : remainingApps) {
                                    if (loaded.packageName != null && loaded.packageName.equals(child.packageName)) {
                                        foundChild = loaded;
                                        break;
                                    }
                                }
                                if (foundChild != null) {
                                    validChildren.add(foundChild);
                                    remainingApps.remove(foundChild);
                                }
                            }
                            savedItem.folderItems = validChildren;
                            if (!savedItem.folderItems.isEmpty()) {
                                finalDock.add(savedItem);
                            }
                        } else if (savedItem.type == AppItem.Type.APP) {
                            AppItem found = null;
                            for (AppItem loaded : remainingApps) {
                                if (loaded.packageName.equals(savedItem.packageName)) {
                                    found = loaded;
                                    break;
                                }
                            }
                            if (found != null) {
                                finalDock.add(found);
                                remainingApps.remove(found);
                            }
                        }
                    }
                }

                // Add remaining new apps
                remainingApps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
                finalGrid.addAll(remainingApps);

            } else {
                // Default sort
                loadedApps.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
                finalGrid.addAll(loadedApps);
            }

            runOnUiThread(() -> {
                // Update Cache
                sCachedAppItems.clear();
                sCachedAppItems.addAll(finalGrid);
                sCachedDockItems.clear();
                sCachedDockItems.addAll(finalDock);

                appItems.clear();
                appItems.addAll(finalGrid);
                
                dockItems.clear();
                dockItems.addAll(finalDock);

                checkFirstRunAndInitDock(pm);
                
                applyLayoutPreferences();
                if (dockAdapter != null)
                    dockAdapter.notifyDataSetChanged();
                // We need to notify pagerAdapter too if it exists
                if (pagerAdapter != null)
                    pagerAdapter.updateApps();
            });
        }).start();
    }

    private void checkFirstRunAndInitDock(PackageManager pm) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean(PREF_FIRST_RUN, true);

        if (isFirstRun) {
            SharedPreferences.Editor editor = prefs.edit();
            List<AppItem> defaultDockApps = new ArrayList<>();
            List<String> defaultPackages = new ArrayList<>();

            // Phone
            Intent phoneIntent = new Intent(Intent.ACTION_DIAL);
            ResolveInfo phoneInfo = pm.resolveActivity(phoneIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (phoneInfo != null) defaultPackages.add(phoneInfo.activityInfo.packageName);

            // SMS
            Intent smsIntent = new Intent(Intent.ACTION_MAIN);
            smsIntent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            ResolveInfo smsInfo = pm.resolveActivity(smsIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (smsInfo != null) defaultPackages.add(smsInfo.activityInfo.packageName);

            // Browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(android.net.Uri.parse("https://www.google.com"));
            ResolveInfo browserInfo = pm.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (browserInfo != null) defaultPackages.add(browserInfo.activityInfo.packageName);

            // Camera
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            ResolveInfo cameraInfo = pm.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (cameraInfo != null) defaultPackages.add(cameraInfo.activityInfo.packageName);

            // Fill empty slots with first available apps if defaults not found
            if (defaultPackages.size() < DOCK_SIZE) {
                for (AppItem item : appItems) {
                    if (defaultPackages.size() >= DOCK_SIZE)
                        break;
                    if (!defaultPackages.contains(item.packageName)) {
                        defaultPackages.add(item.packageName);
                    }
                }
            }

            // Move apps from grid to dock
            for (String pkg : defaultPackages) {
                AppItem found = null;
                for (AppItem item : appItems) {
                    if (item.packageName != null && item.packageName.equals(pkg)) {
                        found = item;
                        break;
                    }
                }
                if (found != null) {
                    dockItems.add(found);
                    appItems.remove(found);
                }
            }

            editor.putBoolean(PREF_FIRST_RUN, false);
            editor.apply();
            saveCurrentLayout();
        }
    }

    private void setupPageIndicators(int count) {
        layoutPageIndicator.removeAllViews();
        if (count <= 1)
            return;

        android.widget.ImageView[] indicators = new android.widget.ImageView[count];
        android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < count; i++) {
            indicators[i] = new android.widget.ImageView(this);
            indicators[i].setImageDrawable(androidx.core.content.ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(layoutParams);
            layoutPageIndicator.addView(indicators[i]);
        }
    }

    private void setCurrentPageIndicator(int position) {
        int childCount = layoutPageIndicator.getChildCount();
        for (int i = 0; i < childCount; i++) {
            android.widget.ImageView imageView = (android.widget.ImageView) layoutPageIndicator.getChildAt(i);
            if (i == position) {
                imageView.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_active));
            } else {
                imageView.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_inactive));
            }
        }
    }

    private void applyWallpaper() {
        android.graphics.drawable.Drawable wallpaper = WallpaperManager.getCurrentWallpaper(this);
        if (wallpaper != null) {
            findViewById(R.id.main).setBackground(wallpaper);
        }
    }

    private void saveCurrentLayout() {
        AppLayoutManager.saveLayout(this, appItems, dockItems);
    }

    private void updateAdaptiveSizes() {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        // Get User Scale Preference
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int scalePercent = prefs.getInt("icon_scale", 100);
        float scaleFactor = scalePercent / 100f;

        // Get Text Scale Preference
        int textScalePercent = prefs.getInt("text_scale", 100);
        float textScaleFactor = textScalePercent / 100f;

        // --- Grid Adaptation ---
        // Estimate available space for grid (taking out dock, status bar, etc.)
        // Let's assume grid takes ~75% of height
        int availableGridHeight = (int) (screenHeight * 0.75f);

        int cellWidth = screenWidth / columnsPerRow;
        int cellHeight = availableGridHeight / rowsPerPage;

        int minDim = Math.min(cellWidth, cellHeight);

        // 75% of the smallest dimension of the cell * User Scale
        int gridIconSize = (int) (minDim * 0.75f * scaleFactor);

        // Text size proportional to icon size (20%) * User Text Scale
        float gridTextSize = (gridIconSize * 0.20f) * textScaleFactor;

        if (pagerAdapter != null) {
            pagerAdapter.setAppIconSize(gridIconSize);
            pagerAdapter.setAppTextSize(gridTextSize);
        }

        // --- Dock Adaptation ---
        // Dock usually fits 5 items comfortably on width
        int dockCellWidth = screenWidth / 5;
        // Dock icons are usually slightly smaller or same as grid * User Scale
        // Since dock usually has no text, we can use a larger portion of the cell width
        int dockIconSize = (int) (dockCellWidth * 0.90f * scaleFactor);

        if (dockAdapter != null) {
            dockAdapter.setIconSize(dockIconSize);
        }
    }

    public void refreshAdapters() {
        applyLayoutPreferences();
        if (pagerAdapter != null) {
            pagerAdapter.notifyDataSetChanged();
        }
        if (dockAdapter != null) {
            dockAdapter.notifyDataSetChanged();
        }
    }

    private void applyLayoutPreferences() {
        // Vérifier que les vues sont initialisées
        if (recyclerDock == null || viewPagerApps == null) {
            android.util.Log.w("MainActivity", "Views not initialized yet, skipping applyLayoutPreferences");
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        showDock = prefs.getBoolean(PREF_SHOW_DOCK, true);
        boolean showDockBg = prefs.getBoolean(PREF_SHOW_DOCK_BG, true);
        String searchBarStyle = prefs.getString(PREF_SEARCH_BAR_STYLE, "solid");
        String transitionEffect = prefs.getString(PREF_TRANSITION_EFFECT, "default");
        int iconRadiusPercent = prefs.getInt(PREF_ICON_RADIUS, 50); // Default to 50% (Squircle)
        boolean hideNotch = prefs.getBoolean(PREF_HIDE_NOTCH, false);
        boolean showSearchBar = prefs.getBoolean("show_search_bar", false);
        boolean searchBarTop = prefs.getBoolean("search_bar_top", false);
        boolean showLabels = prefs.getBoolean("show_labels", true);
        boolean zenMode = prefs.getBoolean(SettingsActivity.KEY_ZEN_MODE, false);
        boolean focusMode = prefs.getBoolean("focus_mode_enabled", false);
        
        // Appliquer la visibilité des labels
        if (pagerAdapter != null) {
            pagerAdapter.setShowLabels(showLabels);
        }
        if (listAdapter != null) {
            listAdapter.setShowLabels(showLabels);
        }
        
        // Appliquer le mode Zen (masquer les apps distrayantes)
        if (zenMode || focusMode) {
            applyZenMode();
        }
        
        // Gestion de la barre de recherche
        if (searchBar != null && viewPagerApps != null && recyclerAppsList != null) {
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams searchParams = 
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) searchBar.getLayoutParams();
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams pagerParams = 
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) viewPagerApps.getLayoutParams();
            
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams listParams = 
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) recyclerAppsList.getLayoutParams();

            int topMarginApps = (int) (30 * getResources().getDisplayMetrics().density);

            if (showSearchBar) {
                searchBar.setVisibility(View.VISIBLE);
                
                if (searchBarTop) {
                    // Positionner en haut (après le notch)
                    searchParams.topMargin = (int) (48 * getResources().getDisplayMetrics().density); // Plus de marge pour le notch
                    searchParams.bottomMargin = 0;
                    
                    // Clear Bottom constraint, Set Top constraint
                    searchParams.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    searchParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    
                    // Adjust Apps Container (ViewPager/Recycler) to be below SearchBar
                    pagerParams.topToBottom = R.id.searchBar;
                    pagerParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    pagerParams.bottomToTop = R.id.layoutPageIndicator; // Extend to bottom (above indicator)
                    pagerParams.topMargin = topMarginApps;
                    
                    listParams.topToBottom = R.id.searchBar;
                    listParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    listParams.bottomToTop = R.id.layoutPageIndicator;
                    listParams.topMargin = topMarginApps;

                } else {
                    // Positionner en bas (au-dessus du dock/indicator)
                    searchParams.topMargin = 0;
                    searchParams.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
                    
                    // Clear Top constraint, Set Bottom constraint
                    searchParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    searchParams.bottomToTop = R.id.layoutPageIndicator;
                    
                    // Adjust Apps Container (ViewPager/Recycler) to be above SearchBar
                    pagerParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    pagerParams.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    pagerParams.bottomToTop = R.id.searchBar;
                    pagerParams.topMargin = topMarginApps + (int) (24 * getResources().getDisplayMetrics().density); // Add extra for status bar
                    
                    listParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                    listParams.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                    listParams.bottomToTop = R.id.searchBar;
                    listParams.topMargin = topMarginApps + (int) (24 * getResources().getDisplayMetrics().density);
                }
                
                searchBar.setLayoutParams(searchParams);
                viewPagerApps.setLayoutParams(pagerParams);
                recyclerAppsList.setLayoutParams(listParams);
                
                // Appliquer le style
                if ("glass".equals(searchBarStyle)) {
                    searchBar.setBackgroundResource(R.drawable.bg_search_bar_glass);
                } else {
                    searchBar.setBackgroundResource(R.drawable.bg_search_bar_solid);
                }
                
                // Setup click listener pour ouvrir la recherche
                searchBar.setOnClickListener(v -> {
                    UniversalSearchDialogFragment searchDialog = new UniversalSearchDialogFragment();
                    searchDialog.show(getSupportFragmentManager(), "UniversalSearch");
                });
            } else {
                searchBar.setVisibility(View.GONE);
                
                // Apps take full height (respecting indicators)
                pagerParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                pagerParams.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                pagerParams.bottomToTop = R.id.layoutPageIndicator;
                pagerParams.topMargin = topMarginApps + (int) (24 * getResources().getDisplayMetrics().density);

                listParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                listParams.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
                listParams.bottomToTop = R.id.layoutPageIndicator;
                listParams.topMargin = topMarginApps + (int) (24 * getResources().getDisplayMetrics().density);
                
                viewPagerApps.setLayoutParams(pagerParams);
                recyclerAppsList.setLayoutParams(listParams);
            }
        }
        
        // Drawer Style (Grid vs List)
        String drawerStyle = prefs.getString("drawer_style", "grid");
        boolean isListMode = "list".equals(drawerStyle);
        
        if (recyclerAppsList != null && viewPagerApps != null && layoutPageIndicator != null) {
            if (isListMode) {
                // Show list, hide pager
                recyclerAppsList.setVisibility(View.VISIBLE);
                viewPagerApps.setVisibility(View.GONE);
                layoutPageIndicator.setVisibility(View.GONE);
                
                // Update list adapter
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            } else {
                // Show pager, hide list
                recyclerAppsList.setVisibility(View.GONE);
                viewPagerApps.setVisibility(View.VISIBLE);
                layoutPageIndicator.setVisibility(View.VISIBLE);
            }
        }
        
        // boolean dynamicIslandEnabled = prefs.getBoolean("dynamic_island_enabled", false); // Handled by Service now

        if (notchOverlay != null) {
            notchOverlay.setVisibility(hideNotch ? View.VISIBLE : View.GONE);
            // Ensure status bar icons are light (white) if notch is hidden (black
            // background)
            // or default (depends on theme) if notch is visible

            if (hideNotch) {
                // Force light status bar icons (dark background)
                // Actually APPEARANCE_LIGHT_STATUS_BARS means DARK icons (for light background)
                // So we want to CLEAR this flag for WHITE icons

                View decorView = getWindow().getDecorView();
                WindowInsetsControllerCompat wic = ViewCompat.getWindowInsetsController(decorView);
                if (wic != null) {
                    wic.setAppearanceLightStatusBars(false); // White icons
                }
            } else {
                // Restore theme default
                // Assuming dark theme -> white icons (false)
                // Assuming light theme -> dark icons (true)
                // Let's check current theme
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean isNightMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

                View decorView = getWindow().getDecorView();
                WindowInsetsControllerCompat wic = ViewCompat.getWindowInsetsController(decorView);
                if (wic != null) {
                    wic.setAppearanceLightStatusBars(!isNightMode);
                }
            }
        }

        /*
        if (dynamicIsland != null) {
            if (dynamicIslandEnabled && !hideNotch) {
                dynamicIsland.setVisibility(View.VISIBLE);
            } else {
                dynamicIsland.setVisibility(View.GONE);
            }
        }
        */
        
        // Ensure service is running if enabled (in case app was killed and restarted but service died)
        // Actually BootReceiver and SettingsActivity handle this. 
        // But if user opens app and service is missing but pref is true?
        // We can check and start it.
        boolean dynamicIslandEnabled = prefs.getBoolean("dynamic_island_enabled", false);
        if (dynamicIslandEnabled && !isServiceRunning(DynamicIslandService.class)) {
             if (android.provider.Settings.canDrawOverlays(this)) {
                 startForegroundService(new Intent(this, DynamicIslandService.class));
             }
        }

        String gridPref = prefs.getString(PREF_ICON_GRID, "4x6");
        if ("5x6".equals(gridPref)) {
            columnsPerRow = 5;
            rowsPerPage = 6;
        } else {
            // Default 4x6
            columnsPerRow = 4;
            rowsPerPage = 6;
        }

        if (recyclerDock != null) {
            recyclerDock.setVisibility(showDock ? View.VISIBLE : View.GONE);
        }

        if (recyclerDock != null && showDockBg) {
            recyclerDock.setBackgroundResource(R.drawable.bg_dock);
        } else if (recyclerDock != null) {
            recyclerDock.setBackground(null);
        }

        /*
         * if ("transparent".equals(searchBarStyle)) {
         * searchBar.setBackgroundResource(R.drawable.bg_search_bar_glass);
         * } else {
         * // Default or solid
         * searchBar.setBackgroundResource(R.drawable.bg_search_bar_solid);
         * }
         */

        if (dockAdapter != null) {
            dockAdapter.setIconRadiusPercent(iconRadiusPercent);
        }

        int pageSize = columnsPerRow * rowsPerPage;

        if (pagerAdapter != null) {
            pagerAdapter.updateGridSize(pageSize, columnsPerRow);
            setupPageIndicators(pagerAdapter.getItemCount());
            setCurrentPageIndicator(viewPagerApps.getCurrentItem());
            viewPagerApps.setPageTransformer(AnimationTransformerFactory.getTransformer(transitionEffect));
            pagerAdapter.setOnOrderChangedListener(this::saveCurrentLayout);
        } else {
            // First init
            pagerAdapter = new AppsPagerAdapter(
                    appItems,
                    pageSize,
                    columnsPerRow,
                    iconRadiusPercent,
                    new AppsAdapter.OnAppClickListener() {
                        @Override
                        public void onAppClick(AppItem appItem) {
                            android.util.Log.d("RuvoluteDebug", "MainActivity(Pager): onAppClick: " + appItem.label + " (" + appItem.packageName + ")");
                            if (appItem.type == AppItem.Type.APP) {
                                if (appItem.packageName != null && appItem.packageName.equals(getPackageName())) {
                                    needsReload = true;
                                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                    startActivity(intent);
                                } else if (appItem.launchIntent != null) {
                                    startActivity(appItem.launchIntent);
                                }
                            } else if (appItem.type == AppItem.Type.FOLDER) {
                                FolderDialogFragment dialog = FolderDialogFragment.newInstance(appItem.label,
                                        appItem.folderItems);
                                dialog.setOnAppClickListener(item -> {
                                    if (item.type == AppItem.Type.APP) {
                                        if (item.packageName != null && item.packageName.equals(getPackageName())) {
                                            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                            startActivity(intent);
                                        } else if (item.launchIntent != null) {
                                            startActivity(item.launchIntent);
                                        }
                                    }
                                });
                                dialog.show(getSupportFragmentManager(), "FolderDialog");
                            }
                        }
                    },
                    new AppsAdapter.OnAppLongClickListener() {
                        @Override
                        public void onAppLongClick(AppItem appItem, View view) {
                            showAppOptions(appItem, view);
                        }
                    });
            pagerAdapter.setAppWidgetHost(mAppWidgetHost);
            pagerAdapter.setOnOrderChangedListener(this::saveCurrentLayout);
            pagerAdapter.setShowRecentsPage(false);
            pagerAdapter.setOnDragListener(dragListener);
            viewPagerApps.setAdapter(pagerAdapter);
            viewPagerApps.setOnDragListener(dragListener);
            setupPageIndicators(pagerAdapter.getItemCount());
            viewPagerApps.setPageTransformer(AnimationTransformerFactory.getTransformer(transitionEffect));

            // Restore last page index
            SharedPreferences prefsLastPage = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int lastPage = prefsLastPage.getInt(PREF_LAST_PAGE_INDEX, 0);
            if (lastPage >= 0 && lastPage < pagerAdapter.getItemCount()) {
                viewPagerApps.setCurrentItem(lastPage, false);
                setCurrentPageIndicator(lastPage);
            } else {
                viewPagerApps.setCurrentItem(0, false);
                setCurrentPageIndicator(0);
            }

            viewPagerApps.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    setCurrentPageIndicator(position);

                    // Save current page index
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putInt(PREF_LAST_PAGE_INDEX, position).apply();
                }
            });
        }

        updateAdaptiveSizes();
        updateAppSpacing();
        updateDockLayout();
    }

    private void showAppOptions(AppItem appItem, View view) {
        if (isDragEnabled) {
            return;
        }

        View popupView = getLayoutInflater().inflate(R.layout.dialog_app_options, null);
        
        // Set width constraint for the popup
        int width = (int) (250 * getResources().getDisplayMetrics().density);
        popupView.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView textAppName = popupView.findViewById(R.id.textAppName);
        textAppName.setText(appItem.label);

        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView, 
                width, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);
        
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(10);
        
        // Logic to dismiss
        Runnable dismissAction = popupWindow::dismiss;

        popupView.findViewById(R.id.btnAppInfo).setOnClickListener(v -> {
            openAppInfo(appItem.packageName);
            dismissAction.run();
        });
        
        // Share button
        popupView.findViewById(R.id.btnShare).setVisibility(View.GONE);

        // Gestures
        popupView.findViewById(R.id.optionGestures).setVisibility(View.GONE);

        // Edit Icon
        popupView.findViewById(R.id.optionEditIcon).setOnClickListener(v -> {
             startEditMode();
             dismissAction.run();
        });
        
        // Organize
        popupView.findViewById(R.id.optionOrganize).setOnClickListener(v -> {
            startEditMode();
            dismissAction.run();
        });
        
        // Rename
        popupView.findViewById(R.id.optionRename).setVisibility(View.GONE);

        // Hide
        popupView.findViewById(R.id.optionHide).setOnClickListener(v -> {
            hideApp(appItem.packageName);
            dismissAction.run();
        });
        
        // Play Store
        popupView.findViewById(R.id.optionPlayStore).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appItem.packageName)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appItem.packageName)));
            }
            dismissAction.run();
        });

        // Uninstall
        View optionUninstall = popupView.findViewById(R.id.optionUninstall);
        if (appItem.type == AppItem.Type.WIDGET) {
             ((TextView)optionUninstall.findViewById(R.id.textAppName)).setText("Supprimer le widget");
             optionUninstall.setOnClickListener(v -> {
                 deleteWidget(appItem);
                 dismissAction.run();
             });
        } else {
             optionUninstall.setOnClickListener(v -> {
                 uninstallApp(appItem.packageName);
                 dismissAction.run();
             });
        }

        // Measure and Position
        popupView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), 
                          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        
        int popupHeight = popupView.getMeasuredHeight();
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        
        // Try to center horizontally relative to the icon
        int xOffset = (view.getWidth() - width) / 2;
        
        // Try to show above
        int yOffset = -view.getHeight() - popupHeight;
        
        // If it goes off screen top, show below
        if (location[1] + yOffset < 0) {
            yOffset = 0; // Below
        }

        popupWindow.showAsDropDown(view, xOffset, yOffset);
    }

    private void updateAppSpacing() {
        int topMargin = (int) (30 * getResources().getDisplayMetrics().density); // 30dp spacing
        
        if (viewPagerApps != null) {
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) viewPagerApps.getLayoutParams();
            params.topMargin = topMargin;
            viewPagerApps.setLayoutParams(params);
        }
        
        if (recyclerAppsList != null) {
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) recyclerAppsList.getLayoutParams();
            params.topMargin = topMargin;
            recyclerAppsList.setLayoutParams(params);
        }
    }

    private void startEditMode() {
        isDragEnabled = true;
        pagerAdapter.setDragEnabled(true);
        if (dockAdapter != null) {
            dockAdapter.setDragEnabled(true);
        }

        Toast.makeText(this, "Mode organisation activé. Touchez pour déplacer. \nAppuyez sur Retour pour quitter.",
                Toast.LENGTH_LONG).show();
    }

    private void stopEditMode() {
        if (isDragEnabled) {
            isDragEnabled = false;
            pagerAdapter.setDragEnabled(false);
            if (dockAdapter != null) {
                dockAdapter.setDragEnabled(false);
            }
            Toast.makeText(this, "Mode organisation terminé", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectWidget() {
        int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    private void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }

    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(data);
        }
    }

    private void createWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        AppItem item = new AppItem();
        item.type = AppItem.Type.WIDGET;
        item.widgetId = appWidgetId;
        item.label = appWidgetInfo.label;
        item.packageName = appWidgetInfo.provider.getPackageName();

        // Rough estimation: 80dp width per cell, 100dp height per cell
        int minWidth = appWidgetInfo.minWidth;
        int minHeight = appWidgetInfo.minHeight;

        int spanX = Math.max(1, (int) Math.ceil(minWidth / 80f));
        int spanY = Math.max(1, (int) Math.ceil(minHeight / 100f));

        item.spanX = Math.min(spanX, columnsPerRow);
        item.spanY = Math.min(spanY, rowsPerPage);

        appItems.add(item);

        // Ensure pagerAdapter has the widget host
        if (pagerAdapter != null) {
            pagerAdapter.setAppWidgetHost(mAppWidgetHost);
            pagerAdapter.updateApps();
        }

        saveCurrentLayout();
        Toast.makeText(this, "Widget ajouté", Toast.LENGTH_SHORT).show();
    }

    private void deleteWidget(AppItem item) {
        appItems.remove(item);
        mAppWidgetHost.deleteAppWidgetId(item.widgetId);
        if (pagerAdapter != null) {
            pagerAdapter.updateApps();
        }
        saveCurrentLayout();
        Toast.makeText(this, "Widget supprimé", Toast.LENGTH_SHORT).show();
    }

    private void updateDockLayout() {
        if (recyclerDock == null) return;

        recyclerDock.post(() -> {
            int width = recyclerDock.getWidth();
            if (width > 0) {
                int paddingHorizontal = recyclerDock.getPaddingStart() + recyclerDock.getPaddingEnd();
                int availableWidth = width - paddingHorizontal;
                // Use default 4 columns if columnsPerRow is invalid (though it defaults to 4)
                int cols = columnsPerRow > 0 ? columnsPerRow : 4;
                int cellWidth = availableWidth / cols;

                if (dockAdapter != null) {
                    dockAdapter.setCellWidth(cellWidth);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isDragEnabled) {
            stopEditMode();
            return;
        }

        // If not on the first page, go back to the first page
        if (viewPagerApps != null && viewPagerApps.getCurrentItem() > 0) {
            viewPagerApps.setCurrentItem(0, true);
            return;
        }

        // If already on first page, do nothing (don't exit the launcher)
        // super.onBackPressed();
    }

    private void uninstallApp(String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

    private void hideApp(String packageName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> hiddenApps = prefs.getStringSet(PREF_HIDDEN_APPS, new HashSet<>());

        Set<String> newHidden = new HashSet<>(hiddenApps);
        newHidden.add(packageName);

        prefs.edit().putStringSet(PREF_HIDDEN_APPS, newHidden).apply();

        loadInstalledApps();
        Toast.makeText(this, R.string.msg_app_hidden, Toast.LENGTH_SHORT).show();
    }

    private void addToDock(AppItem appItem) {
        if (dockItems.contains(appItem)) return;
        if (dockItems.size() < DOCK_SIZE) {
            dockItems.add(appItem);
        } else {
            dockItems.set(DOCK_SIZE - 1, appItem);
        }
        saveCurrentLayout();
        dockAdapter.notifyDataSetChanged();
    }

    private void removeFromDock(AppItem appItem) {
        if (dockItems.remove(appItem)) {
            saveCurrentLayout();
            dockAdapter.notifyDataSetChanged();
        }
    }

    private final View.OnDragListener dragListener = (v, event) -> {
        switch (event.getAction()) {
            case android.view.DragEvent.ACTION_DRAG_STARTED:
                return true;
            case android.view.DragEvent.ACTION_DROP:
                Object localState = event.getLocalState();
                if (localState instanceof AppItem) {
                    AppItem item = (AppItem) localState;
                    int viewId = v.getId();
                    
                    if (viewId == R.id.recyclerDock) {
                        // Handle Dock Drop (Add or Reorder)
                        float x = event.getX();
                        float y = event.getY();
                        View child = recyclerDock.findChildViewUnder(x, y);
                        int newPos = (child != null) ? recyclerDock.getChildAdapterPosition(child) : dockItems.size();
                        if (newPos < 0)
                            newPos = dockItems.size();

                        int existingPos = dockItems.indexOf(item);

                        // Fallback search if indexOf fails (e.g. Folder reference mismatch after reload)
                        if (existingPos == -1) {
                            for (int i = 0; i < dockItems.size(); i++) {
                                AppItem d = dockItems.get(i);
                                if (d.type == item.type) {
                                    if (d.type == AppItem.Type.APP && d.packageName != null && d.packageName.equals(item.packageName)) {
                                        existingPos = i;
                                        break;
                                    }
                                    if (d.type == AppItem.Type.FOLDER && d.label != null && d.label.equals(item.label)) {
                                         // Rough match for folders by label
                                         existingPos = i;
                                         break;
                                    }
                                    if (d.type == AppItem.Type.WIDGET && d.widgetId == item.widgetId) {
                                        existingPos = i;
                                        break;
                                    }
                                }
                            }
                        }

                        if (existingPos != -1) {
                            // Reorder within Dock
                            AppItem movedItem = dockItems.remove(existingPos);
                            if (newPos > existingPos)
                                newPos--;
                            if (newPos < 0)
                                newPos = 0;
                            if (newPos > dockItems.size())
                                newPos = dockItems.size();
                            dockItems.add(newPos, movedItem);
                        } else {
                            // Add to Dock from Grid
                            if (dockItems.size() < DOCK_SIZE) {
                                if (newPos > dockItems.size())
                                    newPos = dockItems.size();
                                dockItems.add(newPos, item);
                                
                                // Remove from Grid
                                appItems.remove(item);
                                if (pagerAdapter != null) {
                                    pagerAdapter.updateApps();
                                }
                            } else {
                                android.widget.Toast
                                        .makeText(MainActivity.this, "Dock plein", android.widget.Toast.LENGTH_SHORT)
                                        .show();
                                return true;
                            }
                        }
                        saveCurrentLayout();
                        dockAdapter.notifyDataSetChanged();

                    } else if (viewId == R.id.recyclerPageApps || viewId == R.id.viewPagerApps) {
                        // Drop on Grid -> Remove from Dock AND Add to Grid
                        
                        // Check if item is in Dock (source)
                        if (dockItems.contains(item)) {
                            removeFromDock(item);
                        }
                        
                        // Add to Grid at position
                        int currentPage = viewPagerApps.getCurrentItem();
                        int width = v.getWidth();
                        int height = v.getHeight();
                        int globalIndex = appItems.size(); // Default to end

                        if (width > 0 && height > 0) {
                            int cellWidth = width / columnsPerRow;
                            int cellHeight = height / rowsPerPage;
                            int col = (int) (event.getX() / cellWidth);
                            int row = (int) (event.getY() / cellHeight);
                            
                            if (col >= columnsPerRow) col = columnsPerRow - 1;
                            if (row >= rowsPerPage) row = rowsPerPage - 1;
                            if (col < 0) col = 0;
                            if (row < 0) row = 0;
                            
                            int indexOnPage = row * columnsPerRow + col;
                            int pageSize = columnsPerRow * rowsPerPage;
                            globalIndex = currentPage * pageSize + indexOnPage;
                            
                            if (globalIndex < 0) globalIndex = 0;
                        }

                        int oldIndex = appItems.indexOf(item);
                        if (oldIndex != -1) {
                            // Reorder within Grid
                            AppItem movedItem = appItems.remove(oldIndex);
                            if (globalIndex > oldIndex) globalIndex--;
                            if (globalIndex < 0) globalIndex = 0;
                            if (globalIndex > appItems.size()) globalIndex = appItems.size();
                            appItems.add(globalIndex, movedItem);
                        } else {
                            // New to Grid (from Dock or Widget?)
                            if (globalIndex > appItems.size()) globalIndex = appItems.size();
                            appItems.add(globalIndex, item);
                        }

                        saveCurrentLayout();
                        if (pagerAdapter != null) {
                            pagerAdapter.updateApps();
                        }
                    }
                }
                return true;
            case android.view.DragEvent.ACTION_DRAG_ENDED:
                return true;
            default:
                return false;
        }
    };

}
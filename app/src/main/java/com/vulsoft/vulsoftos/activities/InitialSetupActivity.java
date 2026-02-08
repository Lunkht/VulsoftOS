package com.vulsoft.vulsoftos.activities;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.vulsoft.vulsoftos.MainActivity;
import com.vulsoft.vulsoftos.R;

public class InitialSetupActivity extends AppCompatActivity {

    private ImageView iconNotifAccess, iconUsageStats, iconOverlay;
    private Button btnGrantNotif, btnGrantUsage, btnGrantOverlay;
    private Button btnSetDefault, btnFinishSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial_setup);

        // Initialize Views
        iconNotifAccess = findViewById(R.id.iconNotifAccess);
        iconUsageStats = findViewById(R.id.iconUsageStats);
        iconOverlay = findViewById(R.id.iconOverlay);

        btnGrantNotif = findViewById(R.id.btnGrantNotif);
        btnGrantUsage = findViewById(R.id.btnGrantUsage);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);

        btnSetDefault = findViewById(R.id.btnSetDefault);
        btnFinishSetup = findViewById(R.id.btnFinishSetup);

        // Setup Listeners
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        checkDefaultLauncher();
    }

    private void setupListeners() {
        // Notification Access
        btnGrantNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                } catch (Exception e) {
                    Toast.makeText(this, "Impossible d'ouvrir les paramètres de notification", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Usage Stats
        btnGrantUsage.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Impossible d'ouvrir les paramètres d'utilisation", Toast.LENGTH_SHORT).show();
            }
        });

        // Overlay
        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Impossible d'ouvrir les paramètres de superposition", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Default Launcher
        btnSetDefault.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // RoleManager logic could be here, but standard intent works often
                Intent intent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                }
            } else {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            }
        });

        // Finish
        btnFinishSetup.setOnClickListener(v -> {
            // Save setup completed pref if needed (already done in Onboarding, but good to confirm)
            getSharedPreferences("launcher_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("initial_setup_completed", true)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void checkPermissions() {
        // 1. Notification Access
        boolean notifGranted = isNotificationServiceEnabled();
        updatePermissionUI(iconNotifAccess, btnGrantNotif, notifGranted);

        // 2. Usage Stats
        boolean usageGranted = hasUsageStatsPermission();
        updatePermissionUI(iconUsageStats, btnGrantUsage, usageGranted);

        // 3. Overlay
        boolean overlayGranted = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlayGranted = Settings.canDrawOverlays(this);
        } else {
            overlayGranted = true; // Implicitly granted below M
        }
        updatePermissionUI(iconOverlay, btnGrantOverlay, overlayGranted);
    }

    private void updatePermissionUI(ImageView icon, Button button, boolean granted) {
        if (granted) {
            icon.setImageResource(R.drawable.ic_check); // Assuming ic_check exists from previous ls
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_green_light));
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
        } else {
            icon.setImageResource(R.drawable.dot_inactive);
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_light));
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (flat != null && !flat.isEmpty()) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final android.content.ComponentName cn = android.content.ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (pkgName.equals(cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void checkDefaultLauncher() {
        PackageManager pm = getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo != null && getPackageName().equals(resolveInfo.activityInfo.packageName)) {
            btnSetDefault.setText("Déjà défini");
            btnSetDefault.setEnabled(false);
            btnSetDefault.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
        } else {
            btnSetDefault.setText("Définir par défaut");
            btnSetDefault.setEnabled(true);
            btnSetDefault.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorAccent));
        }
    }
}

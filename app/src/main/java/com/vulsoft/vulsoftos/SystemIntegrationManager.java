package com.vulsoft.vulsoftos;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import java.util.List;

public class SystemIntegrationManager {
    private static final String PREFS_NAME = "system_integration_prefs";

    public static class IntegrationStatus {
        public boolean isDefaultLauncher = false;
        public boolean hasNotificationAccess = false;
        public boolean hasSystemAlertPermission = false;
        public boolean hasWriteSettingsPermission = false;
        public boolean canDrawOverlays = false;
    }

    public static IntegrationStatus checkIntegrationStatus(Context context) {
        IntegrationStatus status = new IntegrationStatus();

        // Vérifier si c'est le launcher par défaut
        status.isDefaultLauncher = isDefaultLauncher(context);

        // Vérifier l'accès aux notifications
        status.hasNotificationAccess = hasNotificationAccess(context);

        // Vérifier les permissions système
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            status.hasSystemAlertPermission = Settings.canDrawOverlays(context);
            status.hasWriteSettingsPermission = Settings.System.canWrite(context);
            status.canDrawOverlays = Settings.canDrawOverlays(context);
        } else {
            status.hasSystemAlertPermission = true;
            status.hasWriteSettingsPermission = true;
            status.canDrawOverlays = true;
        }

        return status;
    }

    public static boolean isDefaultLauncher(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        PackageManager pm = context.getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName.equals(context.getPackageName());
        }

        // Fallback for some devices or scenarios
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolveInfos) {
            if (info.activityInfo.packageName.equals(context.getPackageName())) {
                // This doesn't guarantee it's the default, but it's a candidate.
                // However, resolveActivity is the source of truth for "active default".
                break;
            }
        }
        return false;
    }

    public static void requestDefaultLauncherRole(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
                        // Role request needs to be started for result usually,
                        // but here we just launch it. If context is not an Activity,
                        // we need NEW_TASK.
                        if (context instanceof android.app.Activity) {
                            ((android.app.Activity) context).startActivityForResult(intent, 123);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }
                        return;
                    } else {
                        // Même si le rôle est déjà détenu, on ouvre les paramètres pour laisser l'utilisateur vérifier
                        // ou changer s'il le souhaite. On continue vers le fallback.
                        android.widget.Toast.makeText(context, "ValkuntOS est déjà défini par défaut",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }

            // Fallback for older versions or if RoleManager fails/is unavailable
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // Ultimate fallback: Main settings or Home settings with different action
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                android.widget.Toast.makeText(context, "Veuillez définir ValkuntOS comme lanceur dans les paramètres",
                        android.widget.Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                android.util.Log.e("SystemIntegration", "Failed to open settings", e2);
            }
        }
    }

    public static boolean hasNotificationAccess(Context context) {
        String packageName = context.getPackageName();
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");

        if (flat != null && !flat.isEmpty()) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName componentName = ComponentName.unflattenFromString(name);
                if (componentName != null && packageName.equals(componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requestNotificationAccess(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void requestSystemPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Permission pour dessiner par-dessus les autres apps
            if (!Settings.canDrawOverlays(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

            // Permission pour modifier les paramètres système
            if (!Settings.System.canWrite(context)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    public static void saveIntegrationPreferences(Context context, IntegrationStatus status) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean("is_default_launcher", status.isDefaultLauncher)
                .putBoolean("has_notification_access", status.hasNotificationAccess)
                .putBoolean("has_system_permissions", status.hasSystemAlertPermission)
                .apply();
    }

    public static void enableSystemIntegration(Context context) {
        try {
            // Activer le receiver de boot
            PackageManager pm = context.getPackageManager();
            ComponentName bootReceiver = new ComponentName(context, BootReceiver.class);
            pm.setComponentEnabledSetting(
                    bootReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            // Choisir le bon service selon les permissions
            if (hasNotificationAccess(context)) {
                // Utiliser le service de notifications complet
                Intent serviceIntent = new Intent(context, NotificationService.class);
                startServiceSafely(context, serviceIntent);
            } else {
                // Utiliser le service d'intégration basique
                Intent serviceIntent = new Intent(context, NotificationService.class);
                startServiceSafely(context, serviceIntent);
            }
        } catch (Exception e) {
            android.util.Log.e("SystemIntegration", "Error enabling system integration", e);
        }
    }

    private static void startServiceSafely(Context context, Intent serviceIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            android.util.Log.w("SystemIntegration", "Cannot start service: " + e.getMessage());
        }
    }

    public static void disableSystemIntegration(Context context) {
        // Désactiver le receiver de boot
        PackageManager pm = context.getPackageManager();
        ComponentName bootReceiver = new ComponentName(context, BootReceiver.class);
        pm.setComponentEnabledSetting(
                bootReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // Arrêter le service de notifications
        Intent serviceIntent = new Intent(context, NotificationService.class);
        context.stopService(serviceIntent);
    }

    public static void optimizeForLauncher(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);

        // Optimisations par défaut pour un launcher
        prefs.edit()
                .putBoolean("auto_hide_navigation", true)
                .putBoolean("immersive_mode", true)
                .putBoolean("fast_app_switching", true)
                .putBoolean("memory_optimization", true)
                .putInt("animation_speed", 1) // Animations rapides
                .apply();
    }

    @android.annotation.SuppressLint("WrongConstant")
    public static void expandNotifications(Context context) {
        try {
            Object service = context.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            java.lang.reflect.Method expand = statusbarManager.getMethod("expandNotificationsPanel");
            expand.invoke(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void lockScreen(Context context) {
        android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) context
                .getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isAdminActive(new ComponentName(context, LauncherAdminReceiver.class))) {
            dpm.lockNow();
        } else {
            android.widget.Toast
                    .makeText(context, "Veuillez activer l'administration de l'appareil dans les paramètres",
                            android.widget.Toast.LENGTH_LONG)
                    .show();
            Intent intent = new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    new ComponentName(context, LauncherAdminReceiver.class));
            intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Requis pour verrouiller l'écran avec le double tap");
            context.startActivity(intent);
        }
    }
}
package com.vulsoft.vulsoftos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.vulsoft.vulsoftos.MainActivity;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            // Analyser la santé du système et des applications au démarrage
            com.vulsoft.vulsoftos.health.AppHealthManager.analyzeSystemHealth(context, true);

            // Vérifier si ValkuntOS est défini comme launcher par défaut
            SharedPreferences prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE);
            boolean isDefaultLauncher = prefs.getBoolean("is_default_launcher", false);
            
            if (isDefaultLauncher) {
                // Démarrer le launcher après le boot
                Intent launcherIntent = new Intent(context, MainActivity.class);
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launcherIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(launcherIntent);
            }
            
            // Initialiser les services système
            initializeSystemServices(context);
        }
    }
    
    private void initializeSystemServices(Context context) {
        try {
            // NotificationListenerService is automatically bound by the system when permission is granted.
            // No need to manually start it.
            
            // LockScreenService removed as requested
            
            // Check other initializations if needed
        } catch (Exception e) {
            android.util.Log.e("BootReceiver", "Error initializing system services", e);
        }
    }
}
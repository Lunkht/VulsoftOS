package com.vulsoft.vulsoftos;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        sendNotificationUpdate();
        sendDynamicIslandUpdate(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        sendNotificationUpdate();
    }
    
    private void sendDynamicIslandUpdate(StatusBarNotification sbn) {
        if (isSystemNotification(sbn)) return;
        
        android.app.Notification notification = sbn.getNotification();
        android.os.Bundle extras = notification.extras;
        
        // Check for Media Session
        if (extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION)) {
            android.media.session.MediaSession.Token token = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION);
            if (token != null) {
                Intent intent = new Intent("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_MEDIA_CONTROL");
                intent.setPackage(getPackageName());
                intent.putExtra("token", token);
                intent.putExtra("package", sbn.getPackageName());
                sendBroadcast(intent);
                return; // Handle as media only
            }
        }
        
        // Filter out ongoing notifications (e.g. background services, downloads) to avoid constant popup
        if (sbn.isOngoing()) {
            return;
        }

        // Filter out low importance notifications (only show IMPORTANCE_DEFAULT (3) or higher)
        try {
            Ranking ranking = new Ranking();
            RankingMap rankingMap = getCurrentRanking();
            if (rankingMap != null && rankingMap.getRanking(sbn.getKey(), ranking)) {
                 if (ranking.getImportance() < android.app.NotificationManager.IMPORTANCE_DEFAULT) {
                     return;
                 }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String title = extras.getString(android.app.Notification.EXTRA_TITLE);
        CharSequence textCharSeq = extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
        String text = textCharSeq != null ? textCharSeq.toString() : "";
        
        // We can't easily pass the Icon object or Drawable across process boundaries via Broadcast Intent extras if it's large.
        // But since we are in the same app process, we can use a Singleton or EventBus.
        // Or for simplicity in a broadcast, we can just pass the package name and try to load the icon in the service,
        // or pass the SmallIcon resId if it's a resource.
        // The safest way for arbitrary apps is to pass package name and let receiver load the app icon.
        // Or better: use a local broadcast if they are in same process.
        
        Intent intent = new Intent("com.vulsoft.vulsoftos.DYNAMIC_ISLAND_SHOW");
        intent.setPackage(getPackageName());
        intent.putExtra("package", sbn.getPackageName());
        intent.putExtra("title", title);
        intent.putExtra("text", text);
        sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // NotificationListenerService is managed by the system.
        // We don't need to manually create channels or start foreground for it
        // unless we want to keep it alive separately from the system binding.
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Do not start foreground to avoid persistent icon.
        // The system binds this service automatically.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private void sendNotificationUpdate() {
        java.util.HashMap<String, Integer> counts = new java.util.HashMap<>();
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                for (StatusBarNotification notification : notifications) {
                    if (!notification.getPackageName().equals(getPackageName()) &&
                            !isSystemNotification(notification)) {
                        String pkg = notification.getPackageName();
                        int count = counts.getOrDefault(pkg, 0);
                        counts.put(pkg, count + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent updateIntent = new Intent("com.vulsoft.vulsoftos.NOTIFICATION_UPDATE");
        updateIntent.setPackage(getPackageName());
        updateIntent.putExtra("notification_counts", counts);
        sendBroadcast(updateIntent);
    }

    private boolean isSystemNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        return packageName.equals("android") ||
                packageName.equals("com.android.systemui") ||
                packageName.startsWith("com.android.system");
    }
}

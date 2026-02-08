package com.vulsoft.vulsoftos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.vulsoft.vulsoftos.activities.ScreenRecordPermissionActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecorderService extends Service {

    public static final String ACTION_SHOW_CONTROLS = "com.vulsoft.vulsoftos.action.SHOW_CONTROLS";
    public static final String ACTION_START_RECORDING = "com.vulsoft.vulsoftos.action.START_RECORDING";
    public static final String ACTION_STOP_SERVICE = "com.vulsoft.vulsoftos.action.STOP_SERVICE";
    
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_AUDIO_ENABLED = "audio_enabled";

    private static final String CHANNEL_ID = "ScreenRecorderChannel";
    private static final int NOTIFICATION_ID = 1002;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;

    private int screenDensity;
    private int displayWidth;
    private int displayHeight;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private boolean isAudioEnabled = true;

    private ImageView btnRecord;
    private ImageView btnPause;
    private ImageView btnStop;
    private ImageView btnClose;
    private ImageView btnSoundOn;
    private ImageView btnSoundOff;
    private TextView tvTimer;
    
    private Vibrator vibrator;

    private long startTime = 0L;
    private long pausedTime = 0L;
    private long totalPausedDuration = 0L;
    private android.os.Handler timerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime - totalPausedDuration;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            int hours = minutes / 60;
            minutes = minutes % 60;

            if (tvTimer != null) {
                if (hours > 0) {
                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
                } else {
                    tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                }
            }
            timerHandler.postDelayed(this, 500);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        createNotificationChannel();
        
        // Get Screen Metrics
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        screenDensity = metrics.densityDpi;
        displayWidth = metrics.widthPixels;
        displayHeight = metrics.heightPixels;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_SHOW_CONTROLS.equals(action)) {
            // For Android 14+, we must not start mediaProjection foreground service here
            // It must be started after user grants permission in the activity
            // But we need to show controls first.
            // We can start a different foreground service type or just show UI if overlay permission exists.
            // However, to keep service alive, we need foreground.
            // Let's use "specialUse" or standard foreground service for controls, 
            // and promote to "mediaProjection" later? No, type is declared in manifest.
            
            // Fix: Start as standard foreground service (if possible) or just show controls.
            // The crash happens because we called startForeground with mediaProjection type WITHOUT the permission being granted yet?
            // Actually, the permission check happens when we call createMediaProjection, not startForeground.
            // Wait, the error says: "requires permissions: ... FOREGROUND_SERVICE_MEDIA_PROJECTION".
            // This is a normal permission we need to request in Manifest.
            
            // We ALREADY added FOREGROUND_SERVICE_MEDIA_PROJECTION to Manifest in previous turn?
            // Let's check Manifest.
            
            startForegroundService();
            showFloatingControls();
        } else if (ACTION_START_RECORDING.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            isAudioEnabled = intent.getBooleanExtra(EXTRA_AUDIO_ENABLED, true);
            
            // Now we have permission, upgrade to mediaProjection foreground service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent notificationIntent = new Intent(this, ScreenRecorderService.class);
                PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Enregistrement en cours")
                        .setContentText("Appuyez pour arrêter")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .build();
                        
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            startRecording(resultCode, resultData);
        } else if (ACTION_STOP_SERVICE.equals(action)) {
            stopRecording();
            stopSelf();
        }

        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, ScreenRecorderService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Enregistrement d'écran")
                .setContentText("Service d'enregistrement d'écran actif")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // On Android 14 (SDK 34+), we cannot start MEDIA_PROJECTION foreground service 
                // BEFORE getting the MediaProjection token/permission from the user.
                // So we should start with a different type or no type if allowed, 
                // OR we must ensure we only call this AFTER onActivityResult in the PermissionActivity.
                
                // BUT we want to show controls FIRST.
                // Strategy: 
                // 1. SHOW_CONTROLS -> Start Foreground Service with type SPECIAL_USE or NONE (if allowed).
                // 2. User clicks Record -> Permission Activity -> START_RECORDING.
                // 3. START_RECORDING -> Update Foreground Service to type MEDIA_PROJECTION.
                
                // Let's try starting without specific type first for controls.
                // If targetSdk >= 34, we must specify type if we want to do that thing later?
                // Actually, if we declare mediaProjection in manifest, we must use it?
                
                // Workaround: Use "specialUse" for the initial state if possible, or just standard.
                // If we pass 0 as type, it defaults to manifest?
                // The error says "Starting FGS with type mediaProjection ... requires permissions".
                // This implies we ARE asking for mediaProjection type.
                // The permission FOREGROUND_SERVICE_MEDIA_PROJECTION is a normal permission in Manifest.
                // DID WE ADD IT? 
                
                // If we added it, maybe we need to request it at runtime? No, it's a normal permission.
                // BUT Android 14 enforces that you must have the MediaProjection consent BEFORE starting the FGS.
                
                // So:
                // 1. We cannot start FGS with type mediaProjection in SHOW_CONTROLS.
                // 2. We should start FGS with type "specialUse" (since we have it for others) or "0" (default).
                
                int type = 0;
                if (Build.VERSION.SDK_INT >= 34) { // Android 14
                     type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE; 
                } else {
                     type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
                }
                
                // However, our service is declared with foregroundServiceType="mediaProjection".
                // We should add "specialUse" to the manifest declaration too.
                
                startForeground(NOTIFICATION_ID, notification, type);
            } catch (Exception e) {
                // Fallback
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void showFloatingControls() {
        if (floatingView != null) return;

        // Use a themed context to ensure attributes like ?attr/selectableItemBackgroundBorderless resolve correctly
        ContextThemeWrapper themedContext = new ContextThemeWrapper(this, R.style.Theme_Ruvolute);
        floatingView = LayoutInflater.from(themedContext).inflate(R.layout.layout_screen_recorder_controls, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        btnRecord = floatingView.findViewById(R.id.btn_record);
        btnPause = floatingView.findViewById(R.id.btn_pause);
        btnStop = floatingView.findViewById(R.id.btn_stop);
        btnClose = floatingView.findViewById(R.id.btn_close_overlay);
        btnSoundOn = floatingView.findViewById(R.id.btn_sound_on);
        btnSoundOff = floatingView.findViewById(R.id.btn_sound_off);
        tvTimer = floatingView.findViewById(R.id.timer_display);

        // Initial state
        if (isAudioEnabled) {
            btnSoundOn.setVisibility(View.VISIBLE);
            btnSoundOff.setVisibility(View.GONE);
        } else {
            btnSoundOn.setVisibility(View.GONE);
            btnSoundOff.setVisibility(View.VISIBLE);
        }

        btnSoundOn.setOnClickListener(v -> {
            vibrate();
            isAudioEnabled = false;
            btnSoundOn.setVisibility(View.GONE);
            btnSoundOff.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Micro désactivé", Toast.LENGTH_SHORT).show();
        });

        btnSoundOff.setOnClickListener(v -> {
            vibrate();
            isAudioEnabled = true;
            btnSoundOn.setVisibility(View.VISIBLE);
            btnSoundOff.setVisibility(View.GONE);
            Toast.makeText(this, "Micro activé", Toast.LENGTH_SHORT).show();
        });

        btnRecord.setOnClickListener(v -> {
            vibrate();
            // Request Permission via Activity
            Intent intent = new Intent(this, ScreenRecordPermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(EXTRA_AUDIO_ENABLED, isAudioEnabled);
            startActivity(intent);
        });

        btnPause.setOnClickListener(v -> {
            vibrate();
            if (isRecording) {
                if (isPaused) {
                    resumeRecording();
                } else {
                    pauseRecording();
                }
            }
        });

        btnStop.setOnClickListener(v -> {
            vibrate();
            stopRecording();
            updateUIState(false);
        });

        btnClose.setOnClickListener(v -> {
            stopRecording();
            stopSelf();
        });

        // Drag functionality
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(Xdiff) < 10 && Math.abs(Ydiff) < 10) {
                            v.performClick(); // Handle click
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        // Forward clicks from touch listener to child views manually if needed, 
        // but standard OnClickListener works if OnTouch returns false or handles click.
        // Simplified above: touch listener on root consumes events. 
        // Better: Set OnTouchListener only for a drag handle or handle interception.
        // For simplicity: Let's remove the root OnTouchListener and rely on a drag handle or specific area.
        // OR: use a simplified drag implementation that doesn't block clicks.
        
        // Correct approach for buttons + drag:
        // Set OnTouchListener on the root layout for dragging.
        // If movement is small, dispatch click? No, buttons intercept touch.
        // We will make the root draggable, but buttons clickable.
        // This requires buttons to NOT consume touch unless clicked?
        // Actually, if buttons are on top, they get the event.
        // Let's attach Drag listener to a specific part or just handle correctly.
        // Simplest: Drag listener on root. Buttons are children. 
        // If user touches button, button handles it. If user touches background, drag.
        
        windowManager.addView(floatingView, params);
    }
    
    private void startRecording(int resultCode, Intent resultData) {
        initMediaRecorder();
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopRecording();
                }
            }, null);
            
            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorder",
                    displayWidth, displayHeight, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null);
            
            mediaRecorder.start();
            isRecording = true;
            isPaused = false;
            
            startTime = System.currentTimeMillis();
            totalPausedDuration = 0;
            timerHandler.postDelayed(timerRunnable, 0);
            
            updateUIState(true);
            Toast.makeText(this, "Enregistrement démarré", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur de démarrage: " + e.getMessage(), Toast.LENGTH_LONG).show();
            stopRecording();
        }
    }

    private void initMediaRecorder() {
        try {
            mediaRecorder = new MediaRecorder();
            if (isAudioEnabled) {
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            if (isAudioEnabled) {
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            }
            mediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(displayWidth, displayHeight);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                android.content.ContentValues values = new android.content.ContentValues();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                values.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "REC_" + timeStamp + ".mp4");
                values.put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Ruvolute");
                
                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                    if (pfd != null) {
                        mediaRecorder.setOutputFile(pfd.getFileDescriptor());
                    }
                }
            } else {
                // Legacy path
                String filePath = getRecordingFilePath();
                mediaRecorder.setOutputFile(filePath);
            }
            
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRecordingFilePath() {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Ruvolute");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return folder.getAbsolutePath() + "/REC_" + timeStamp + ".mp4";
    }

    private void stopRecording() {
        if (isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
            isRecording = false;
            timerHandler.removeCallbacks(timerRunnable);
            if (tvTimer != null) tvTimer.setText("00:00");
            Toast.makeText(this, "Enregistrement sauvegardé", Toast.LENGTH_LONG).show();
        }
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        updateUIState(false);
    }
    
    private void pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.pause();
            isPaused = true;
            
            pausedTime = System.currentTimeMillis();
            timerHandler.removeCallbacks(timerRunnable);
            
            btnPause.setImageResource(android.R.drawable.ic_media_play);
            Toast.makeText(this, "Pause", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder.resume();
            isPaused = false;
            
            long now = System.currentTimeMillis();
            totalPausedDuration += (now - pausedTime);
            timerHandler.postDelayed(timerRunnable, 0);
            
            btnPause.setImageResource(android.R.drawable.ic_media_pause);
            Toast.makeText(this, "Reprise", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUIState(boolean recording) {
        if (floatingView == null) return;
        
        if (recording) {
            btnRecord.setVisibility(View.GONE);
            btnPause.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);
            btnClose.setVisibility(View.GONE);
            if (tvTimer != null) tvTimer.setVisibility(View.VISIBLE);
        } else {
            btnRecord.setVisibility(View.VISIBLE);
            btnPause.setVisibility(View.GONE);
            btnStop.setVisibility(View.GONE);
            btnClose.setVisibility(View.VISIBLE);
            btnPause.setImageResource(android.R.drawable.ic_media_pause); // Reset icon
            if (tvTimer != null) tvTimer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        stopRecording();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Recorder Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void vibrate() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
}

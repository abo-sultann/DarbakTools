package com.abosultan.darbaktools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public final class AccessibilitySettingsHelper {
    public static final int FAILED = 0;
    public static final int OPENED_ACCESSIBILITY = 1;
    public static final int OPENED_GENERAL_SETTINGS = 2;

    private AccessibilitySettingsHelper() {}

    public static int open(Context context) {
        // Standard Android route.
        if (tryStart(context, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))) {
            return OPENED_ACCESSIBILITY;
        }

        // Some car ROMs expose the same page only through explicit AOSP activities.
        String[] activities = new String[]{
                "com.android.settings.Settings$AccessibilitySettingsActivity",
                "com.android.settings.AccessibilitySettings",
                "com.android.settings.Settings$AccessibilitySettingsDashboardActivity",
                "com.android.settings.Settings$AccessibilityDashboardActivity"
        };
        for (String activity : activities) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", activity));
            if (tryStart(context, intent)) return OPENED_ACCESSIBILITY;
        }

        // Alternate action used by a number of vendor ROMs.
        if (tryStart(context, new Intent("android.settings.ACCESSIBILITY_SETTINGS"))) {
            return OPENED_ACCESSIBILITY;
        }

        // Final fallback: at least open the ROM settings app instead of failing silently.
        if (tryStart(context, new Intent(Settings.ACTION_SETTINGS))) {
            return OPENED_GENERAL_SETTINGS;
        }

        Intent explicitSettings = new Intent();
        explicitSettings.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
        if (tryStart(context, explicitSettings)) {
            return OPENED_GENERAL_SETTINGS;
        }

        return FAILED;
    }

    private static boolean tryStart(Context context, Intent intent) {
        try {
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}

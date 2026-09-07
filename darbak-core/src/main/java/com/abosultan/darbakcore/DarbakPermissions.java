package com.abosultan.darbakcore;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/** Minimal runtime-permission helper; UI wording remains app-specific but state handling is consistent. */
public final class DarbakPermissions {
    private DarbakPermissions() {}

    public static boolean granted(Activity activity, String permission) {
        return Build.VERSION.SDK_INT < 23 || activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean allGranted(Activity activity, String... permissions) {
        if (Build.VERSION.SDK_INT < 23) return true;
        for (String permission : permissions) {
            if (!granted(activity, permission)) return false;
        }
        return true;
    }

    public static void requestMissing(Activity activity, int requestCode, String... permissions) {
        if (Build.VERSION.SDK_INT < 23 || allGranted(activity, permissions)) return;
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        for (String permission : permissions) if (!granted(activity, permission)) missing.add(permission);
        activity.requestPermissions(missing.toArray(new String[0]), requestCode);
    }
}

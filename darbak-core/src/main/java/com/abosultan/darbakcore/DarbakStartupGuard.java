package com.abosultan.darbakcore;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Crash-loop guard. Host apps call beginLaunch() before expensive startup work and markHealthy()
 * after the main screen becomes usable. Three incomplete launches recommend Safe Mode.
 */
public final class DarbakStartupGuard {
    private static final String PREF = "darbak_startup_guard";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_FAILURES = "failures";
    private static final int SAFE_MODE_THRESHOLD = 3;

    private DarbakStartupGuard() {}

    public static boolean beginLaunch(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        int failures = p.getInt(KEY_FAILURES, 0);
        if (p.getBoolean(KEY_PENDING, false)) failures++;
        p.edit().putBoolean(KEY_PENDING, true).putInt(KEY_FAILURES, failures).apply();
        return failures >= SAFE_MODE_THRESHOLD;
    }

    public static void markHealthy(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PENDING, false).putInt(KEY_FAILURES, 0).apply();
    }

    public static int incompleteLaunches(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_FAILURES, 0);
    }

    public static void reset(Context context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }
}

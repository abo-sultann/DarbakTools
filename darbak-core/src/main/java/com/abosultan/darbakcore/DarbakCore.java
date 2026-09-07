package com.abosultan.darbakcore;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.View;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Shared runtime foundation for all Darbak car-screen applications. */
public final class DarbakCore {
    private static volatile boolean installed;
    private static Thread.UncaughtExceptionHandler previousHandler;

    private DarbakCore() {}

    public static synchronized void install(Context context) {
        if (installed) return;
        final Context app = context.getApplicationContext();
        ensureDirectories(app);
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            persistCrash(app, thread, error);
            if (previousHandler != null) previousHandler.uncaughtException(thread, error);
        });
        installed = true;
    }

    public static void prepareCarScreen(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        View decor = activity.getWindow().getDecorView();
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decor.setSystemUiVisibility(flags);
    }

    public static File reportsDir(Context context) {
        File dir = new File(context.getFilesDir(), "darbak_reports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File updatesDir(Context context) {
        File dir = new File(context.getCacheDir(), "darbak_updates");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static void ensureDirectories(Context context) {
        reportsDir(context);
        updatesDir(context);
    }

    private static void persistCrash(Context context, Thread thread, Throwable error) {
        try {
            File file = new File(reportsDir(context), "last_crash.txt");
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            try (FileWriter writer = new FileWriter(file, false)) {
                writer.write("Darbak Crash Report\n");
                writer.write("Time: " + now + "\n");
                writer.write("Thread: " + thread.getName() + "\n");
                writer.write("Android: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n\n");
                writer.write(sw.toString());
            }
        } catch (Throwable ignored) {
            // A crash handler must never cause a second crash.
        }
    }
}

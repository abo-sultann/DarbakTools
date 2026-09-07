package com.abosultan.darbakcore;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.StatFs;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Lightweight diagnostics designed for the low-memory Android 7.1 car screen. */
public final class DarbakDiagnostics {
    public enum Health { HEALTHY, WARNING, CRITICAL }

    public static final class Snapshot {
        public final Health health;
        public final long freeStorageMb;
        public final long freeHeapMb;
        public final String versionName;
        public final int versionCode;
        public final String report;

        Snapshot(Health health, long freeStorageMb, long freeHeapMb,
                 String versionName, int versionCode, String report) {
            this.health = health;
            this.freeStorageMb = freeStorageMb;
            this.freeHeapMb = freeHeapMb;
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.report = report;
        }
    }

    private DarbakDiagnostics() {}

    public static Snapshot inspect(Context context) {
        long freeStorageMb = freeStorageMb(context);
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long freeHeapMb = Math.max(0L, (rt.maxMemory() - used) / 1024L / 1024L);

        String versionName = "?";
        int versionCode = 0;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = info.versionName == null ? "?" : info.versionName;
            versionCode = Build.VERSION.SDK_INT >= 28 ? (int) info.getLongVersionCode() : info.versionCode;
        } catch (Exception ignored) { }

        Health health = Health.HEALTHY;
        if (freeStorageMb < 100 || freeHeapMb < 20) health = Health.WARNING;
        if (freeStorageMb < 30 || freeHeapMb < 8) health = Health.CRITICAL;

        String report = "Darbak Health Check\n"
                + "Package: " + context.getPackageName() + "\n"
                + "Version: " + versionName + " (" + versionCode + ")\n"
                + "Android: " + Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT + "\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Free storage MB: " + freeStorageMb + "\n"
                + "Free heap MB: " + freeHeapMb + "\n"
                + "Health: " + health.name() + "\n";

        return new Snapshot(health, freeStorageMb, freeHeapMb, versionName, versionCode, report);
    }

    public static File export(Context context) throws Exception {
        Snapshot snapshot = inspect(context);
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(DarbakCore.reportsDir(context), "health_" + stamp + ".txt");
        try (FileWriter writer = new FileWriter(out, false)) {
            writer.write(snapshot.report);
        }
        return out;
    }

    private static long freeStorageMb(Context context) {
        try {
            StatFs stat = new StatFs(context.getFilesDir().getAbsolutePath());
            long bytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            return bytes / 1024L / 1024L;
        } catch (Throwable ignored) {
            return -1L;
        }
    }
}

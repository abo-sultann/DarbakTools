package com.abosultan.darbaktools;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Lightweight diagnostics shared pattern for Darbak apps.
 * No network dependency, no background analytics and no sensitive collection.
 */
public final class DarbakDiagnostics {
    private DarbakDiagnostics() {}

    public static String buildSummary(Context context) {
        StringBuilder out = new StringBuilder();
        out.append("Darbak Diagnostics\n");
        out.append("التطبيق: ").append(context.getApplicationInfo().loadLabel(context.getPackageManager())).append('\n');
        out.append("الإصدار: ").append(versionName(context)).append(" (").append(versionCode(context)).append(")\n");
        out.append("Android: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append('\n');
        out.append("الجهاز: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        out.append("اللوحة: ").append(Build.BOARD).append('\n');
        out.append("الذاكرة القصوى للتطبيق: ").append(formatBytes(Runtime.getRuntime().maxMemory())).append('\n');
        out.append("الذاكرة المستخدمة: ").append(formatBytes(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())).append('\n');

        File storage = Environment.getExternalStorageDirectory();
        try {
            StatFs stat = new StatFs(storage.getAbsolutePath());
            long block = stat.getBlockSizeLong();
            long available = stat.getAvailableBlocksLong() * block;
            long total = stat.getBlockCountLong() * block;
            out.append("التخزين المتاح: ").append(formatBytes(available)).append(" من ").append(formatBytes(total)).append('\n');
        } catch (Throwable ignored) {
            out.append("التخزين: غير متاح للقراءة\n");
        }

        out.append("الحزمة: ").append(context.getPackageName()).append('\n');
        out.append("الهوية: Darbak Settings UI V1\n");
        out.append("تصميم وتطوير • أبوسلطان");
        return out.toString();
    }

    public static String versionName(Context context) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return p.versionName == null ? "—" : p.versionName;
        } catch (Exception e) {
            return "—";
        }
    }

    @SuppressWarnings("deprecation")
    public static long versionCode(Context context) {
        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) return p.getLongVersionCode();
            return p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 MB";
        double mb = bytes / (1024d * 1024d);
        if (mb < 1024d) return new DecimalFormat("0").format(mb) + " MB";
        return new DecimalFormat("0.0").format(mb / 1024d) + " GB";
    }
}

package com.abosultan.darbaksystem.bridge;

import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

final class DeviceDiagnostics {
    private DeviceDiagnostics() {
    }

    static String localIp(Context context) {
        try {
            WifiManager manager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (manager != null) {
                WifiInfo info = manager.getConnectionInfo();
                if (info != null && info.getIpAddress() != 0) {
                    int value = info.getIpAddress();
                    return (value & 0xff) + "." +
                            ((value >> 8) & 0xff) + "." +
                            ((value >> 16) & 0xff) + "." +
                            ((value >> 24) & 0xff);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "غير متوفر";
    }

    static String buildReport(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("Darbak System Bridge 0.1.0\n");
        report.append("================================\n");
        add(report, "IP", localIp(context));
        add(report, "Android الظاهر", Build.VERSION.RELEASE);
        add(report, "API الحقيقي", String.valueOf(Build.VERSION.SDK_INT));
        add(report, "الشركة", Build.MANUFACTURER);
        add(report, "الموديل", Build.MODEL);
        add(report, "الجهاز", Build.DEVICE);
        add(report, "اللوحة", Build.BOARD);
        add(report, "المنتج", Build.PRODUCT);
        add(report, "الإصدار", Build.DISPLAY);
        add(report, "Fingerprint", Build.FINGERPRINT);
        add(report, "ABI", joinAbis());
        add(report, "RAM", totalRam(context));
        add(report, "التخزين الداخلي", storageSummary());
        report.append('\n');
        report.append("خصائص النظام\n");
        report.append("--------------------------------\n");
        String[] properties = {
                "ro.build.version.release",
                "ro.build.version.sdk",
                "ro.product.board",
                "ro.build.display.id",
                "ro.build.type",
                "ro.debuggable",
                "ro.secure",
                "ro.adb.secure",
                "service.adb.tcp.port",
                "persist.adb.tcp.port",
                "persist.sys.usb.config",
                "sys.usb.config",
                "sys.usb.state"
        };
        for (String property : properties) {
            add(report, property, emptyAsDash(RootShell.getProperty(property)));
        }
        add(report, "su binary", RootShell.hasSuBinary() ? "موجود" : "غير موجود");
        return report.toString();
    }

    private static void add(StringBuilder report, String key, String value) {
        report.append(key).append(": ").append(emptyAsDash(value)).append('\n');
    }

    private static String emptyAsDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private static String joinAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null) {
            StringBuilder result = new StringBuilder();
            for (String abi : Build.SUPPORTED_ABIS) {
                if (result.length() > 0) {
                    result.append(", ");
                }
                result.append(abi);
            }
            return result.toString();
        }
        return Build.CPU_ABI;
    }

    private static String totalRam(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return "غير متوفر";
        }
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        return formatBytes(info.totalMem);
    }

    private static String storageSummary() {
        try {
            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            long total;
            long free;
            if (Build.VERSION.SDK_INT >= 18) {
                total = stat.getTotalBytes();
                free = stat.getAvailableBytes();
            } else {
                total = (long) stat.getBlockCount() * stat.getBlockSize();
                free = (long) stat.getAvailableBlocks() * stat.getBlockSize();
            }
            return formatBytes(free) + " متاح من " + formatBytes(total);
        } catch (Exception error) {
            return "غير متوفر";
        }
    }

    private static String formatBytes(long bytes) {
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        return new DecimalFormat("0.00").format(gib) + " GB";
    }
}

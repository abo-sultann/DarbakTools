package com.abosultan.darbaksystem.bridge;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

final class DeviceDiagnostics {
    private static final String AOSP_PLATFORM_SHA256 =
            "C8A2E9BCCF597C2FB6DC66BEE293FC13F2FC47EC77BC6B2B0D52C11F51192AB8";

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
        report.append("Darbak System Bridge 0.1.5\n");
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
        String platformSha256 = platformSignatureSha256(context);
        add(report, "مفتاح منصة AOSP", AOSP_PLATFORM_SHA256.equals(platformSha256)
                ? "مطابق للمفتاح الافتراضي"
                : "غير مطابق للمفتاح الافتراضي");
        add(report, "Platform SHA-256", platformSha256);
        report.append('\n');
        report.append("فحص مسارات الوصول — قراءة فقط\n");
        report.append("--------------------------------\n");
        String shellSha256 = packageSignatureSha256(context, "com.android.shell");
        add(report, "Shell SHA-256", shellSha256);
        add(report, "علاقة مفتاح Shell", platformSha256.equals(shellSha256)
                ? "نفس مفتاح المنصة"
                : "مفتاح مختلف عن المنصة");
        add(report, "Shell UID", packageUid(context, "com.android.shell"));
        add(report, "Settings SHA-256", packageSignatureSha256(context, "com.android.settings"));
        add(report, "SystemUI SHA-256", packageSignatureSha256(context, "com.android.systemui"));
        add(report, "هوية تطبيق دربك", emptyAsDash(RootShell.runNormal("id").output));
        add(report, "حالة SELinux", emptyAsDash(RootShell.runNormal("getenforce 2>/dev/null").output));
        add(report, "adb_enabled", emptyAsDash(RootShell.runNormal(
                "settings get global adb_enabled 2>/dev/null").output));
        add(report, "تفاصيل su", suDetails());
        report.append('\n');
        report.append("جاهزية ADB — قراءة فقط\n");
        report.append("--------------------------------\n");
        String developmentEnabled = globalSetting(context, "development_settings_enabled");
        String adbEnabled = globalSetting(context, "adb_enabled");
        String adbdState = RootShell.getProperty("init.svc.adbd");
        String tcpPort = RootShell.getProperty("service.adb.tcp.port");
        String persistentTcpPort = RootShell.getProperty("persist.adb.tcp.port");
        boolean port5555Listening = isAdbTcpReachable(context);
        add(report, "خيارات المطور محفوظة", settingState(developmentEnabled));
        add(report, "تصحيح USB محفوظ", settingState(adbEnabled));
        add(report, "قيمة adb_enabled المباشرة", emptyAsDash(adbEnabled));
        add(report, "حالة خدمة adbd", emptyAsDash(adbdState));
        add(report, "منفذ ADB المؤقت", emptyAsDash(tcpPort));
        add(report, "منفذ ADB الدائم", emptyAsDash(persistentTcpPort));
        add(report, "المنفذ 5555", port5555Listening ? "يستمع للاتصال" : "غير مستمع");
        add(report, "عملية adbd", processDetails("adbd"));
        add(report, "ملف adbd", adbBinaryDetails());
        add(report, "صلاحية WRITE_SECURE_SETTINGS", hasPermission(
                context, "android.permission.WRITE_SECURE_SETTINGS") ? "ممنوحة" : "غير ممنوحة");
        add(report, "التشخيص", adbDiagnosis(adbEnabled, adbdState, tcpPort, port5555Listening));
        return report.toString();
    }

    private static String platformSignatureSha256(Context context) {
        return packageSignatureSha256(context, "android");
    }

    private static String packageSignatureSha256(Context context, String packageName) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
            Signature[] signatures = info.signatures;
            if (signatures == null || signatures.length == 0) {
                return "غير متوفر";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = digest.digest(signatures[0].toByteArray());
            StringBuilder hex = new StringBuilder(value.length * 2);
            for (byte item : value) {
                hex.append(String.format("%02X", item & 0xff));
            }
            return hex.toString();
        } catch (Exception error) {
            return "تعذر الفحص: " + error.getClass().getSimpleName();
        }
    }

    private static String packageUid(Context context, String packageName) {
        try {
            return String.valueOf(context.getPackageManager()
                    .getApplicationInfo(packageName, 0).uid);
        } catch (Exception error) {
            return "غير متوفر";
        }
    }

    private static String suDetails() {
        ShellResult result = RootShell.runNormal(
                "for f in /system/bin/su /system/xbin/su /sbin/su /vendor/bin/su; do " +
                "if [ -e \"$f\" ]; then ls -l \"$f\"; fi; done");
        return emptyAsDash(result.output).replace('\n', ' ');
    }

    private static String globalSetting(Context context, String key) {
        try {
            String value = Settings.Global.getString(context.getContentResolver(), key);
            return value == null ? "-" : value.trim();
        } catch (Exception error) {
            return "تعذر الفحص: " + error.getClass().getSimpleName();
        }
    }

    private static String settingState(String value) {
        if ("1".equals(value)) {
            return "نعم";
        }
        if ("0".equals(value)) {
            return "لا";
        }
        return "غير محدد";
    }

    static boolean isAdbTcpReachable(Context context) {
        String ip = localIp(context);
        if (!"غير متوفر".equals(ip) && canConnect(ip, 5555)) {
            return true;
        }
        return canConnect("127.0.0.1", 5555);
    }

    private static boolean canConnect(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), 600);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static String processDetails(String processName) {
        ShellResult result = RootShell.runNormal(
                "ps 2>/dev/null | grep '[" + processName.substring(0, 1) + "]" +
                        processName.substring(1) + "' | head -n 1");
        return emptyAsDash(result.output).replace('\n', ' ');
    }

    private static String adbBinaryDetails() {
        ShellResult result = RootShell.runNormal(
                "for f in /sbin/adbd /system/bin/adbd; do " +
                        "if [ -e \"$f\" ]; then ls -l \"$f\"; fi; done");
        return emptyAsDash(result.output).replace('\n', ' ');
    }

    private static boolean hasPermission(Context context, String permission) {
        return context.getPackageManager().checkPermission(permission, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED;
    }

    private static String adbDiagnosis(String adbEnabled, String adbdState, String tcpPort,
                                       boolean port5555Listening) {
        if (port5555Listening || "5555".equals(tcpPort.trim())) {
            return "ADB اللاسلكي جاهز";
        }
        if ("running".equals(adbdState.trim())) {
            return "خدمة ADB تعمل ولكنها ليست على Wi-Fi";
        }
        if ("1".equals(adbEnabled)) {
            return "تصحيح USB محفوظ لكن خدمة ADB متوقفة";
        }
        if ("0".equals(adbEnabled)) {
            return "تصحيح USB غير مفعّل";
        }
        return "نحتاج نتيجة القيم أعلاه لتحديد المسار";
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

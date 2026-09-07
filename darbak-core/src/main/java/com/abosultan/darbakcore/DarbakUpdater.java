package com.abosultan.darbakcore;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Generic, dependency-light update engine for Darbak APKs. */
public final class DarbakUpdater {
    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName;
        public final String apkUrl;
        public final String sha256;
        public final String notes;

        public UpdateInfo(int versionCode, String versionName, String apkUrl, String sha256, String notes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.notes = notes;
        }
    }

    public interface CheckListener {
        void onResult(UpdateInfo info, boolean newer);
        void onError(String message, Throwable error);
    }

    public interface Listener {
        void onProgress(int percent);
        void onReady(UpdateInfo info, File apk);
        void onError(String message, Throwable error);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private DarbakUpdater() {}

    public static UpdateInfo parseManifest(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        return new UpdateInfo(
                o.getInt("versionCode"),
                o.optString("versionName", String.valueOf(o.getInt("versionCode"))),
                o.getString("apkUrl"),
                o.optString("sha256", "").trim().toLowerCase(Locale.US),
                o.optString("notes", "")
        );
    }

    public static void check(Context context, String manifestUrl, CheckListener listener) {
        IO.execute(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(manifestUrl).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                c.setInstanceFollowRedirects(true);
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                UpdateInfo info = parseManifest(json.toString());
                listener.onResult(info, isNewer(context, info));
            } catch (Throwable error) {
                listener.onError("تعذر التحقق من وجود تحديث", error);
            } finally {
                if (c != null) c.disconnect();
            }
        });
    }

    public static boolean isNewer(Context context, UpdateInfo info) {
        try {
            android.content.pm.PackageInfo current = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            long currentCode = Build.VERSION.SDK_INT >= 28 ? current.getLongVersionCode() : current.versionCode;
            return info.versionCode > currentCode;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void download(Context context, UpdateInfo info, Listener listener) {
        IO.execute(() -> {
            File partial = new File(DarbakCore.updatesDir(context), "update.apk.part");
            File target = new File(DarbakCore.updatesDir(context), "update.apk");
            try {
                if (partial.exists()) partial.delete();
                if (target.exists()) target.delete();

                HttpURLConnection c = (HttpURLConnection) new URL(info.apkUrl).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setInstanceFollowRedirects(true);
                c.connect();
                int code = c.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);

                long total = c.getContentLengthLong();
                long done = 0;
                byte[] buffer = new byte[32 * 1024];
                try (BufferedInputStream in = new BufferedInputStream(c.getInputStream());
                     FileOutputStream out = new FileOutputStream(partial)) {
                    int n;
                    int last = -1;
                    while ((n = in.read(buffer)) != -1) {
                        out.write(buffer, 0, n);
                        done += n;
                        if (total > 0) {
                            int p = (int) Math.min(100, done * 100L / total);
                            if (p != last) {
                                last = p;
                                listener.onProgress(p);
                            }
                        }
                    }
                    out.getFD().sync();
                } finally {
                    c.disconnect();
                }

                if (!info.sha256.isEmpty()) {
                    String actual = sha256(partial);
                    if (!actual.equalsIgnoreCase(info.sha256)) {
                        partial.delete();
                        throw new SecurityException("APK SHA-256 mismatch");
                    }
                }

                if (!partial.renameTo(target)) throw new IllegalStateException("Cannot finalize update APK");
                listener.onReady(info, target);
            } catch (Throwable error) {
                if (partial.exists()) partial.delete();
                listener.onError("تعذر تنزيل أو التحقق من التحديث", error);
            }
        });
    }

    public static boolean install(Context context, File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !context.getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settings);
            return false;
        }

        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".darbakcore.fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
        return true;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[32 * 1024];
        try (FileInputStream in = new FileInputStream(file)) {
            int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.US, "%02x", b));
        return out.toString();
    }
}

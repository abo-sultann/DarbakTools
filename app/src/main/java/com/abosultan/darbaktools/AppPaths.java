package com.abosultan.darbaktools;

import android.os.Environment;

import java.io.File;

public final class AppPaths {
    private AppPaths() {}

    public static File root() {
        File dir = new File(Environment.getExternalStorageDirectory(), "DarbakTools");
        ensure(dir);
        return dir;
    }

    public static File inbox() {
        File dir = new File(root(), "Inbox");
        ensure(dir);
        return dir;
    }

    public static File downloads() {
        File dir = new File(root(), "Downloads");
        ensure(dir);
        return dir;
    }

    public static File apks() {
        File dir = new File(root(), "APK");
        ensure(dir);
        return dir;
    }

    public static File linksFile() {
        return new File(root(), "links.txt");
    }

    public static void ensure(File dir) {
        if (dir != null && !dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }
}

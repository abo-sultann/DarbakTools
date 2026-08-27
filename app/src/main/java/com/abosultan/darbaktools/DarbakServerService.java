package com.abosultan.darbaktools;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class DarbakServerService extends Service {
    private static final String CHANNEL_ID = "darbak_local_server";
    private static volatile boolean running;
    private static volatile int activePort = 8080;
    private static volatile String lastError = "";

    private TransferServer server;

    public static void ensureStarted(Context context) {
        Intent intent = new Intent(context, DarbakServerService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
            else context.startService(intent);
        } catch (Exception ignored) {
            try { context.startService(intent); } catch (Exception ignoredAgain) {}
        }
    }

    public static boolean isRunning() {
        return running;
    }

    public static int getPort() {
        return activePort;
    }

    public static String getLastError() {
        return lastError == null ? "" : lastError;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startAsForeground();
        startServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server == null) startServer();
        return START_STICKY;
    }

    private void startServer() {
        if (server != null) return;
        lastError = "";
        for (int port = 8080; port <= 8083; port++) {
            try {
                TransferServer candidate = new TransferServer(getApplicationContext(), port, AppPaths.inbox(), AppPaths.linksFile(), null);
                candidate.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                server = candidate;
                activePort = port;
                running = true;
                return;
            } catch (IOException e) {
                lastError = e.getMessage() == null ? "تعذر تشغيل الخادم" : e.getMessage();
            } catch (Exception e) {
                lastError = e.getMessage() == null ? "خطأ غير معروف" : e.getMessage();
            }
        }
        running = false;
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "DarbakTools local connection", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the local iPhone connection available");
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder = new Notification.Builder(this, CHANNEL_ID);
        else builder = new Notification.Builder(this);
        builder.setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("دربك Tools")
                .setContentText("اتصال الآيفون المحلي جاهز")
                .setOngoing(true)
                .setShowWhen(false);
        startForeground(2407, builder.build());
    }

    @Override
    public void onDestroy() {
        running = false;
        if (server != null) {
            server.stop();
            server = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

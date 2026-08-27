package com.abosultan.darbaktools;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class TransferServer extends NanoHTTPD {
    public interface Listener {
        void onFileReceived(String fileName);
        void onLinkReceived(String url);
    }

    private final Context context;
    private final File inbox;
    private final File linksFile;
    private final Listener listener;

    public TransferServer(Context context, int port, File inbox, File linksFile, Listener listener) {
        super(port);
        this.context = context.getApplicationContext();
        this.inbox = inbox;
        this.linksFile = linksFile;
        this.listener = listener;
        AppPaths.ensure(inbox);
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            if (Method.GET.equals(session.getMethod()) && ("/".equals(uri) || "/index.html".equals(uri))) return serveAsset("remote.html", "text/html; charset=utf-8");
            if (Method.GET.equals(session.getMethod()) && "/health".equals(uri)) return text(Response.Status.OK, "OK");
            if (Method.GET.equals(session.getMethod()) && "/api/status".equals(uri)) return json(Response.Status.OK, statusJson());
            if (Method.GET.equals(session.getMethod()) && "/api/files".equals(uri)) return json(Response.Status.OK, filesJson());
            if (Method.GET.equals(session.getMethod()) && "/api/apps".equals(uri)) return json(Response.Status.OK, appsJson());
            if (Method.GET.equals(session.getMethod()) && "/screen.jpg".equals(uri)) return screenFrame();
            if (Method.GET.equals(session.getMethod()) && "/download".equals(uri)) return download(session);
            if (Method.POST.equals(session.getMethod()) && "/upload".equals(uri)) return receiveUpload(session);
            if (Method.POST.equals(session.getMethod()) && "/link".equals(uri)) return receiveLink(session);
            if (Method.POST.equals(session.getMethod()) && "/api/tap".equals(uri)) return tap(session);
            if (Method.POST.equals(session.getMethod()) && "/api/long".equals(uri)) return longPress(session);
            if (Method.POST.equals(session.getMethod()) && "/api/swipe".equals(uri)) return swipe(session);
            if (Method.POST.equals(session.getMethod()) && "/api/key".equals(uri)) return key(session);
            if (Method.POST.equals(session.getMethod()) && "/api/text".equals(uri)) return sendText(session);
            if (Method.POST.equals(session.getMethod()) && "/api/launch".equals(uri)) return launch(session);
            return text(Response.Status.NOT_FOUND, "Not found");
        } catch (Exception e) {
            return text(Response.Status.INTERNAL_ERROR, e.getMessage() == null ? "حدث خطأ" : e.getMessage());
        }
    }

    private Response serveAsset(String name, String mime) throws IOException {
        InputStream in = context.getAssets().open(name);
        byte[] data = readAll(in);
        in.close();
        Response response = newFixedLengthResponse(Response.Status.OK, mime, new ByteArrayInputStream(data), data.length);
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        return response;
    }

    private Response receiveUpload(IHTTPSession session) throws Exception {
        AppPaths.ensure(inbox);
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tempPath = files.get("file");
        String requestedName = session.getParms().get("file");
        if (tempPath == null) return text(Response.Status.BAD_REQUEST, "لم يصل ملف");

        String name = safeName(requestedName);
        if (name.length() == 0 || "file".equalsIgnoreCase(name)) {
            name = "received-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        }
        File destination = uniqueFile(inbox, name);
        copy(new File(tempPath), destination);
        if (listener != null) listener.onFileReceived(destination.getName());
        return text(Response.Status.OK, destination.getName());
    }

    private Response receiveLink(IHTTPSession session) throws Exception {
        parseForm(session);
        String url = session.getParms().get("url");
        if (url == null || url.trim().length() == 0) return text(Response.Status.BAD_REQUEST, "الرابط فارغ");
        url = url.trim();
        Writer writer = new OutputStreamWriter(new FileOutputStream(linksFile, true), "UTF-8");
        writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        writer.write("\t");
        writer.write(url);
        writer.write("\n");
        writer.close();
        if (listener != null) listener.onLinkReceived(url);
        return text(Response.Status.OK, "تم إرسال الرابط");
    }

    private Response download(IHTTPSession session) throws Exception {
        String name = session.getParms().get("name");
        if (name == null) return text(Response.Status.BAD_REQUEST, "اسم الملف غير موجود");
        File file = new File(inbox, safeName(name));
        if (!file.exists() || !file.isFile()) return text(Response.Status.NOT_FOUND, "الملف غير موجود");
        InputStream in = new FileInputStream(file);
        Response response = newChunkedResponse(Response.Status.OK, mime(file.getName()), in);
        response.addHeader("Content-Disposition", "attachment; filename=\"" + asciiFallback(file.getName()) + "\"");
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    private Response screenFrame() {
        byte[] frame = ScreenCaptureService.getLatestFrame();
        if (frame == null || frame.length == 0) return text(Response.Status.SERVICE_UNAVAILABLE, "عرض الشاشة غير مفعّل");
        Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", new ByteArrayInputStream(frame), frame.length);
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        return response;
    }

    private Response tap(IHTTPSession session) throws Exception {
        parseForm(session);
        float x = f(session, "x");
        float y = f(session, "y");
        return command(RemoteAccessibilityService.tap(x, y));
    }

    private Response longPress(IHTTPSession session) throws Exception {
        parseForm(session);
        return command(RemoteAccessibilityService.longPress(f(session, "x"), f(session, "y")));
    }

    private Response swipe(IHTTPSession session) throws Exception {
        parseForm(session);
        long duration = Math.round(f(session, "duration"));
        return command(RemoteAccessibilityService.swipe(f(session, "x1"), f(session, "y1"), f(session, "x2"), f(session, "y2"), duration));
    }

    private Response key(IHTTPSession session) throws Exception {
        parseForm(session);
        String action = session.getParms().get("action");
        if ("volup".equals(action) || "voldown".equals(action) || "mute".equals(action)) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return command(false);
            int direction = AudioManager.ADJUST_TOGGLE_MUTE;
            if ("volup".equals(action)) direction = AudioManager.ADJUST_RAISE;
            if ("voldown".equals(action)) direction = AudioManager.ADJUST_LOWER;
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
            return command(true);
        }
        return command(RemoteAccessibilityService.global(action));
    }

    private Response sendText(IHTTPSession session) throws Exception {
        parseForm(session);
        String value = session.getParms().get("text");
        return command(RemoteAccessibilityService.setFocusedText(value));
    }

    private Response launch(IHTTPSession session) throws Exception {
        parseForm(session);
        String pkg = session.getParms().get("package");
        if (pkg == null || pkg.length() == 0) return command(false);
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) return command(false);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return command(true);
    }

    private String statusJson() {
        byte[] frame = ScreenCaptureService.getLatestFrame();
        boolean screen = ScreenCaptureService.isRunning() && frame != null && frame.length > 0;
        return "{\"server\":true,\"screen\":" + screen + ",\"accessibility\":" + RemoteAccessibilityService.isReady() +
                ",\"width\":" + ScreenCaptureService.getCaptureWidth() + ",\"height\":" + ScreenCaptureService.getCaptureHeight() + "}";
    }

    private String filesJson() {
        StringBuilder out = new StringBuilder("[");
        File[] files = inbox.listFiles();
        boolean first = true;
        if (files != null) {
            java.util.Arrays.sort(files, new Comparator<File>() {
                @Override public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
            });
            for (File file : files) {
                if (!file.isFile()) continue;
                if (!first) out.append(',');
                first = false;
                out.append("{\"name\":\"").append(jsonEscape(file.getName())).append("\",\"size\":\"")
                        .append(formatBytes(file.length())).append("\"}");
            }
        }
        return out.append(']').toString();
    }

    private String appsJson() {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm)));
            }
        });
        StringBuilder out = new StringBuilder("[");
        boolean first = true;
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null || info.activityInfo.packageName == null) continue;
            if (!first) out.append(',');
            first = false;
            out.append("{\"name\":\"").append(jsonEscape(String.valueOf(info.loadLabel(pm)))).append("\",\"package\":\"")
                    .append(jsonEscape(info.activityInfo.packageName)).append("\"}");
        }
        return out.append(']').toString();
    }

    private static void parseForm(IHTTPSession session) throws Exception {
        session.parseBody(new HashMap<String, String>());
    }

    private static float f(IHTTPSession session, String key) {
        try { return Float.parseFloat(session.getParms().get(key)); } catch (Exception e) { return 0f; }
    }

    private static Response command(boolean ok) {
        return text(ok ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE, ok ? "OK" : "الخدمة غير مفعلة");
    }

    private static Response json(Response.Status status, String body) {
        Response r = newFixedLengthResponse(status, "application/json; charset=utf-8", body);
        r.addHeader("Cache-Control", "no-store");
        return r;
    }

    private static Response text(Response.Status status, String body) {
        Response r = newFixedLengthResponse(status, "text/plain; charset=utf-8", body == null ? "" : body);
        r.addHeader("Cache-Control", "no-store");
        return r;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static void copy(File source, File destination) throws IOException {
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(destination);
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        out.flush();
        out.close();
        in.close();
    }

    private static File uniqueFile(File dir, String fileName) {
        File f = new File(dir, fileName);
        if (!f.exists()) return f;
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        int i = 2;
        while (f.exists()) f = new File(dir, base + " (" + i++ + ")" + ext);
        return f;
    }

    private static String safeName(String name) {
        if (name == null) return "";
        name = name.replace('\\', '_').replace('/', '_').trim();
        while (name.startsWith(".")) name = name.substring(1);
        return name.replaceAll("[\\r\\n\\t]", "_");
    }

    private static String jsonEscape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String asciiFallback(String name) {
        return name.replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    private static String mime(String name) {
        String lower = name.toLowerCase(Locale.US);
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (lower.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

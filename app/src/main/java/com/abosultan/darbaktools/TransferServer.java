package com.abosultan.darbaktools;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class TransferServer extends NanoHTTPD {
    public interface Listener {
        void onFileReceived(String fileName);
        void onLinkReceived(String url);
    }

    private final File inbox;
    private final File linksFile;
    private final Listener listener;

    public TransferServer(int port, File inbox, File linksFile, Listener listener) {
        super(port);
        this.inbox = inbox;
        this.linksFile = linksFile;
        this.listener = listener;
        AppPaths.ensure(inbox);
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            if (Method.POST.equals(session.getMethod()) && "/upload".equals(uri)) {
                return receiveUpload(session);
            }
            if (Method.POST.equals(session.getMethod()) && "/link".equals(uri)) {
                return receiveLink(session);
            }
            if (Method.GET.equals(session.getMethod()) && "/download".equals(uri)) {
                return download(session);
            }
            if (Method.GET.equals(session.getMethod()) && "/health".equals(uri)) {
                return text(Response.Status.OK, "OK");
            }
            return html(Response.Status.OK, buildPage());
        } catch (Exception e) {
            return html(Response.Status.INTERNAL_ERROR,
                    "<html dir='rtl'><meta charset='utf-8'><body><h3>حدث خطأ</h3><pre>" + escape(e.getMessage()) + "</pre></body></html>");
        }
    }

    private Response receiveUpload(IHTTPSession session) throws Exception {
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
        Map<String, String> ignored = new HashMap<>();
        session.parseBody(ignored);
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
        return response;
    }

    private String buildPage() {
        StringBuilder filesHtml = new StringBuilder();
        File[] files = inbox.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (!file.isFile()) continue;
                String encoded = encode(file.getName());
                filesHtml.append("<a class='file' href='/download?name=").append(encoded).append("'>")
                        .append(escape(file.getName())).append("<small>")
                        .append(formatBytes(file.length())).append("</small></a>");
            }
        } else {
            filesHtml.append("<div class='empty'>لا توجد ملفات في الوارد حتى الآن</div>");
        }

        return "<!doctype html><html lang='ar' dir='rtl'><head><meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>" +
                "<title>دربك Tools</title><style>" +
                "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#071a16;color:#fff;margin:0;padding:18px}" +
                ".wrap{max-width:720px;margin:auto}.brand{color:#d4af37;font-size:30px;font-weight:800;margin:8px 0}.muted{color:#bdcac6}" +
                ".card{background:#0f3129;border:1px solid #6f6127;border-radius:18px;padding:18px;margin:14px 0}" +
                "button,.pick{display:block;width:100%;box-sizing:border-box;border:0;border-radius:14px;background:#d4af37;color:#071a16;padding:15px;font-size:18px;font-weight:800;text-align:center;margin-top:12px}" +
                "input[type=file]{display:none}input[type=url]{width:100%;box-sizing:border-box;background:#071a16;border:1px solid #596d66;border-radius:12px;color:#fff;padding:14px;font-size:16px}" +
                ".file{display:flex;justify-content:space-between;gap:15px;color:#fff;text-decoration:none;background:#0b251f;padding:13px;border-radius:12px;margin:8px 0;word-break:break-all}.file small{color:#d4af37;white-space:nowrap}.empty{color:#bdcac6;padding:10px 0}" +
                "#status{min-height:24px;color:#d4af37;margin-top:10px}</style></head><body><div class='wrap'>" +
                "<div class='brand'>دربك Tools</div><div class='muted'>إرسال الملفات والروابط إلى شاشة السيارة مباشرة</div>" +
                "<div class='card'><h2>إرسال ملفات للشاشة</h2><label class='pick' for='files'>اختيار الملفات</label><input id='files' type='file' multiple>" +
                "<button onclick='sendFiles()'>إرسال المحدد</button><div id='status'></div></div>" +
                "<div class='card'><h2>إرسال رابط</h2><input id='url' type='url' placeholder='https://...'><button onclick='sendLink()'>إرسال الرابط للشاشة</button></div>" +
                "<div class='card'><h2>ملفات الوارد</h2>" + filesHtml + "</div>" +
                "<script>async function sendFiles(){const fs=document.getElementById('files').files,s=document.getElementById('status');if(!fs.length){s.textContent='اختر ملفاً أولاً';return;}for(let i=0;i<fs.length;i++){s.textContent='جاري إرسال '+(i+1)+' من '+fs.length+'...';let f=new FormData();f.append('file',fs[i],fs[i].name);let r=await fetch('/upload',{method:'POST',body:f});if(!r.ok){s.textContent='تعذر إرسال '+fs[i].name;return;}}s.textContent='تم الإرسال بنجاح';setTimeout(()=>location.reload(),700)}" +
                "async function sendLink(){let u=document.getElementById('url').value.trim();if(!u)return;let f=new URLSearchParams();f.set('url',u);let r=await fetch('/link',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:f});alert(await r.text())}</script>" +
                "</div></body></html>";
    }

    private static Response html(Response.Status status, String body) {
        Response r = newFixedLengthResponse(status, "text/html; charset=utf-8", body);
        r.addHeader("Cache-Control", "no-store");
        return r;
    }

    private static Response text(Response.Status status, String body) {
        return newFixedLengthResponse(status, "text/plain; charset=utf-8", body);
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

    private static String encode(String text) {
        try { return URLEncoder.encode(text, "UTF-8"); } catch (Exception e) { return text; }
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
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

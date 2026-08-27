package com.abosultan.darbaktools;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DownloadActivity extends Activity {
    private EditText urlInput;
    private TextView status;
    private ArrayAdapter<String> adapter;
    private final List<File> files = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refreshFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFiles();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        Ui.rtl(root);

        TextView title = Ui.title(this, "مركز التنزيل", 28);
        title.setTextColor(Ui.GOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(bar);
        root.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        urlInput = new EditText(this);
        urlInput.setHint("الصق رابط الملف هنا https://...");
        urlInput.setSingleLine(true);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Ui.MUTED);
        urlInput.setTextSize(17);
        urlInput.setBackground(Ui.rounded(Ui.PANEL, Ui.GOLD, 12, this));
        urlInput.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        bar.addView(urlInput, new LinearLayout.LayoutParams(0, -1, 1));

        Button download = Ui.button(this, "تنزيل");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(this, 150), -1);
        bp.setMargins(Ui.dp(this, 10), 0, 0, 0);
        bar.addView(download, bp);

        status = Ui.title(this, "يحفظ داخل DarbakTools/Downloads", 15);
        status.setTextColor(Ui.MUTED);
        root.addView(status, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        ListView list = new ListView(this);
        list.setBackgroundColor(Ui.BG);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(Color.WHITE);
                v.setTextSize(18);
                v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                v.setMinHeight(Ui.dp(DownloadActivity.this, 56));
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> openFile(files.get(pos)));
        download.setOnClickListener(v -> enqueue());
        return root;
    }

    private void enqueue() {
        String url = urlInput.getText().toString().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "أدخل رابط HTTP أو HTTPS صحيح", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String fileName = URLUtil.guessFileName(url, null, null);
            File destination = uniqueFile(AppPaths.downloads(), fileName);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(destination.getName());
            request.setDescription("DarbakTools");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(destination));
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("DownloadManager unavailable");
            long id = manager.enqueue(request);
            status.setText("بدأ التنزيل • رقم المهمة " + id + " • " + destination.getName());
            urlInput.setText("");
        } catch (Exception e) {
            status.setText("تعذر بدء التنزيل: " + e.getMessage());
        }
    }

    private void refreshFiles() {
        File[] found = AppPaths.downloads().listFiles();
        files.clear();
        if (found != null) {
            Arrays.sort(found, new Comparator<File>() {
                @Override public int compare(File a, File b) {
                    return Long.compare(b.lastModified(), a.lastModified());
                }
            });
            for (File f : found) if (f.isFile()) files.add(f);
        }
        if (adapter == null) return;
        adapter.clear();
        for (File f : files) adapter.add("⬇  " + f.getName() + "     " + formatBytes(f.length()));
        adapter.notifyDataSetChanged();
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime(file));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح الملف", Toast.LENGTH_SHORT).show();
        }
    }

    private String mime(File file) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
        return type == null ? "*/*" : type;
    }

    private static File uniqueFile(File dir, String name) {
        if (name == null || name.trim().isEmpty()) name = "download.bin";
        name = name.replace('/', '_').replace('\\', '_');
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 2;
        while (f.exists()) f = new File(dir, base + " (" + i++ + ")" + ext);
        return f;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }
}

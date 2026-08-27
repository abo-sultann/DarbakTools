package com.abosultan.darbaktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ApkActivity extends Activity {
    private final List<ApkItem> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        scan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scan();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 14));
        Ui.rtl(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(top);
        root.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        TextView title = Ui.title(this, "مركز APK", 28);
        title.setTextColor(Ui.GOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        Button library = Ui.button(this, "مكتبة APK");
        top.addView(library, new LinearLayout.LayoutParams(Ui.dp(this, 180), -1));
        library.setOnClickListener(v -> {
            Intent i = new Intent(this, FileBrowserActivity.class);
            i.putExtra("path", AppPaths.apks().getAbsolutePath());
            startActivity(i);
        });

        TextView hint = Ui.title(this, "يفحص ملفات APK الموجودة في الوارد والتنزيلات ومكتبة APK ويقارن الحد الأدنى مع Android 7.1.", 15);
        hint.setTextColor(Ui.MUTED);
        root.addView(hint, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        ListView list = new ListView(this);
        list.setBackgroundColor(Ui.BG);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(Color.WHITE);
                v.setTextSize(17);
                v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                v.setMinHeight(Ui.dp(ApkActivity.this, 62));
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, position, id) -> details(items.get(position)));
        return root;
    }

    private void scan() {
        if (adapter == null) return;
        items.clear();
        addApks(AppPaths.inbox());
        addApks(AppPaths.downloads());
        addApks(AppPaths.apks());
        items.sort(new Comparator<ApkItem>() {
            @Override public int compare(ApkItem a, ApkItem b) {
                return Long.compare(b.file.lastModified(), a.file.lastModified());
            }
        });
        adapter.clear();
        for (ApkItem item : items) {
            String badge;
            if (item.info == null) badge = "⚠ غير قابل للقراءة";
            else if (item.minSdk <= Build.VERSION.SDK_INT) badge = "✓ مناسب";
            else badge = "✕ يحتاج Android " + apiName(item.minSdk) + "+";
            adapter.add(badge + "     " + item.displayName() + "     " + formatBytes(item.file.length()));
        }
        if (items.isEmpty()) adapter.add("لا توجد ملفات APK. أرسل APK من الآيفون إلى الوارد أو نزّله من مركز التنزيل.");
        adapter.notifyDataSetChanged();
    }

    private void addApks(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".apk")) {
                items.add(read(file));
            }
        }
    }

    private ApkItem read(File file) {
        PackageManager pm = getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(file.getAbsolutePath(), 0);
        int min = -1;
        String label = file.getName();
        if (info != null && info.applicationInfo != null) {
            ApplicationInfo ai = info.applicationInfo;
            ai.sourceDir = file.getAbsolutePath();
            ai.publicSourceDir = file.getAbsolutePath();
            try { label = pm.getApplicationLabel(ai).toString(); } catch (Exception ignored) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) min = ai.minSdkVersion;
        }
        return new ApkItem(file, info, label, min);
    }

    private void details(ApkItem item) {
        if (item.info == null) {
            new AlertDialog.Builder(this).setTitle(item.file.getName())
                    .setMessage("تعذر قراءة معلومات APK. قد يكون الملف تالفًا أو من نوع حزمة غير مدعوم.")
                    .setPositiveButton("حسنًا", null).show();
            return;
        }
        String version = item.info.versionName == null ? String.valueOf(item.info.versionCode) : item.info.versionName;
        String compatibility = item.minSdk <= Build.VERSION.SDK_INT ? "متوافق مبدئيًا مع هذه الشاشة" : "غير متوافق: يحتاج API " + item.minSdk + " أو أحدث";
        String message = "الاسم: " + item.label +
                "\nالحزمة: " + item.info.packageName +
                "\nالإصدار: " + version +
                "\nMin SDK: " + item.minSdk +
                "\nالحجم: " + formatBytes(item.file.length()) +
                "\n\n" + compatibility;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(item.file.getName())
                .setMessage(message)
                .setPositiveButton("تثبيت", (d, w) -> install(item))
                .setNeutralButton("حفظ في المكتبة", (d, w) -> saveToLibrary(item.file))
                .setNegativeButton("إلغاء", null).create();
        dialog.show();
    }

    private void install(ApkItem item) {
        if (item.minSdk > Build.VERSION.SDK_INT) {
            Toast.makeText(this, "هذا التطبيق يحتاج إصدار Android أحدث", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            Toast.makeText(this, "اسمح بالتثبيت من هذا المصدر ثم حاول مرة أخرى", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", item.file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح مثبت الحزم: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveToLibrary(File source) {
        try {
            File target = uniqueFile(AppPaths.apks(), source.getName());
            copy(source, target);
            Toast.makeText(this, "تم حفظ نسخة في مكتبة APK", Toast.LENGTH_SHORT).show();
            scan();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر حفظ النسخة", Toast.LENGTH_SHORT).show();
        }
    }

    private static void copy(File source, File target) throws Exception {
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(target);
        byte[] buffer = new byte[64 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        out.flush();
        out.close();
        in.close();
    }

    private static File uniqueFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 2;
        while (f.exists()) f = new File(dir, base + " (" + i++ + ")" + ext);
        return f;
    }

    private String apiName(int api) {
        if (api == 25) return "7.1";
        if (api == 26) return "8.0";
        if (api == 27) return "8.1";
        if (api == 28) return "9";
        if (api == 29) return "10";
        if (api == 30) return "11";
        if (api == 31 || api == 32) return "12";
        if (api == 33) return "13";
        if (api == 34) return "14";
        if (api == 35) return "15";
        return String.valueOf(api);
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    private static class ApkItem {
        final File file;
        final PackageInfo info;
        final String label;
        final int minSdk;

        ApkItem(File file, PackageInfo info, String label, int minSdk) {
            this.file = file;
            this.info = info;
            this.label = label;
            this.minSdk = minSdk;
        }

        String displayName() {
            if (info == null) return file.getName();
            String version = info.versionName == null ? String.valueOf(info.versionCode) : info.versionName;
            return label + "  •  " + version;
        }
    }
}

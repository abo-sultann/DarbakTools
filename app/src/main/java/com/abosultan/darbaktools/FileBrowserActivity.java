package com.abosultan.darbaktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.MimeTypeMap;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FileBrowserActivity extends Activity {
    private TextView pathView;
    private ListView listView;
    private final List<File> entries = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private File current;
    private boolean rootsMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        String requested = getIntent().getStringExtra("path");
        if (requested != null) showDirectory(new File(requested));
        else showRoots();
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 12), Ui.dp(this, 18), Ui.dp(this, 12));
        Ui.rtl(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(top);
        root.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 70)));

        Button storage = Ui.button(this, "التخزين");
        Button up = Ui.button(this, "رجوع ↑");
        Button newFolder = Ui.button(this, "+ مجلد");
        pathView = Ui.title(this, "الملفات", 20);
        pathView.setTextColor(Ui.GOLD);
        pathView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);

        top.addView(storage, new LinearLayout.LayoutParams(Ui.dp(this, 130), -1));
        top.addView(up, new LinearLayout.LayoutParams(Ui.dp(this, 125), -1));
        top.addView(newFolder, new LinearLayout.LayoutParams(Ui.dp(this, 125), -1));
        top.addView(pathView, new LinearLayout.LayoutParams(0, -1, 1));

        listView = new ListView(this);
        listView.setDividerHeight(1);
        listView.setBackgroundColor(Ui.BG);
        listView.setCacheColorHint(Ui.BG);
        root.addView(listView, new LinearLayout.LayoutParams(-1, 0, 1));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(Color.WHITE);
                v.setTextSize(18);
                v.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                v.setPadding(Ui.dp(FileBrowserActivity.this, 16), 0, Ui.dp(FileBrowserActivity.this, 16), 0);
                v.setMinHeight(Ui.dp(FileBrowserActivity.this, 58));
                return v;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> open(entries.get(position)));
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            fileMenu(entries.get(position));
            return true;
        });

        storage.setOnClickListener(v -> showRoots());
        up.setOnClickListener(v -> goUp());
        newFolder.setOnClickListener(v -> createFolder());
        return root;
    }

    private void showRoots() {
        rootsMode = true;
        current = null;
        pathView.setText("أماكن التخزين");
        entries.clear();
        Set<String> seen = new LinkedHashSet<>();
        addRoot(new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath()), seen);
        File storage = new File("/storage");
        File[] children = storage.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && child.canRead() && !"emulated".equals(child.getName()) && !"self".equals(child.getName())) {
                    addRoot(child, seen);
                }
            }
        }
        refreshAdapter();
    }

    private void addRoot(File file, Set<String> seen) {
        try {
            String path = file.getCanonicalPath();
            if (seen.add(path)) entries.add(file);
        } catch (Exception ignored) {
        }
    }

    private void showDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            Toast.makeText(this, "لا يمكن فتح هذا المجلد", Toast.LENGTH_SHORT).show();
            return;
        }
        rootsMode = false;
        current = dir;
        pathView.setText(dir.getAbsolutePath());
        entries.clear();
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> list = Arrays.asList(files);
            Collections.sort(list, new Comparator<File>() {
                @Override public int compare(File a, File b) {
                    if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            entries.addAll(list);
        }
        refreshAdapter();
    }

    private void refreshAdapter() {
        adapter.clear();
        for (File f : entries) {
            String prefix = f.isDirectory() ? "📁  " : iconFor(f) + "  ";
            String extra = f.isFile() ? "     " + formatBytes(f.length()) : "";
            adapter.add(prefix + f.getName() + extra);
        }
        adapter.notifyDataSetChanged();
    }

    private void goUp() {
        if (rootsMode || current == null) {
            finish();
            return;
        }
        File parent = current.getParentFile();
        if (parent == null || "/".equals(current.getAbsolutePath())) showRoots();
        else showDirectory(parent);
    }

    @Override
    public void onBackPressed() {
        if (rootsMode) super.onBackPressed();
        else goUp();
    }

    private void open(File file) {
        if (file.isDirectory()) {
            showDirectory(file);
            return;
        }
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

    private void fileMenu(File file) {
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setItems(new String[]{"فتح", "إعادة تسمية", "حذف"}, (dialog, which) -> {
                    if (which == 0) open(file);
                    else if (which == 1) rename(file);
                    else confirmDelete(file);
                }).show();
    }

    private void rename(File file) {
        EditText input = new EditText(this);
        input.setText(file.getName());
        input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("إعادة تسمية").setView(input)
                .setPositiveButton("حفظ", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0 || name.contains("/") || name.contains("\\")) return;
                    File target = new File(file.getParentFile(), name);
                    if (target.exists()) {
                        Toast.makeText(this, "يوجد ملف بهذا الاسم", Toast.LENGTH_SHORT).show();
                    } else if (!file.renameTo(target)) {
                        Toast.makeText(this, "تعذر إعادة التسمية", Toast.LENGTH_SHORT).show();
                    }
                    if (current != null) showDirectory(current);
                }).setNegativeButton("إلغاء", null).show();
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this).setTitle("حذف")
                .setMessage("هل تريد حذف \"" + file.getName() + "\"؟")
                .setPositiveButton("حذف", (d, w) -> {
                    boolean ok = deleteRecursive(file);
                    Toast.makeText(this, ok ? "تم الحذف" : "تعذر الحذف", Toast.LENGTH_SHORT).show();
                    if (current != null) showDirectory(current); else showRoots();
                }).setNegativeButton("إلغاء", null).show();
    }

    private void createFolder() {
        if (current == null) {
            Toast.makeText(this, "افتح مكان تخزين أولاً", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText input = new EditText(this);
        input.setHint("اسم المجلد");
        new AlertDialog.Builder(this).setTitle("مجلد جديد").setView(input)
                .setPositiveButton("إنشاء", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0 || name.contains("/") || name.contains("\\")) return;
                    File folder = new File(current, name);
                    boolean ok = folder.mkdir();
                    Toast.makeText(this, ok ? "تم إنشاء المجلد" : "تعذر إنشاء المجلد", Toast.LENGTH_SHORT).show();
                    showDirectory(current);
                }).setNegativeButton("إلغاء", null).show();
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) if (!deleteRecursive(child)) return false;
        }
        return file.delete();
    }

    private String mime(File file) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.US));
        return type == null ? "*/*" : type;
    }

    private String iconFor(File f) {
        String n = f.getName().toLowerCase(Locale.US);
        if (n.endsWith(".apk")) return "APK";
        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".m4a")) return "♫";
        if (n.endsWith(".mp4") || n.endsWith(".mkv")) return "▶";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) return "▣";
        if (n.endsWith(".zip")) return "ZIP";
        return "•";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.1f MB", bytes / 1048576.0);
        return String.format(Locale.US, "%.1f GB", bytes / 1073741824.0);
    }
}

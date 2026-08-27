package com.abosultan.darbaktools;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    private static final int REQ_STORAGE = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPaths.root();
        requestStorageIfNeeded();
        setContentView(buildUi());
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 18), Ui.dp(this, 24), Ui.dp(this, 18));
        Ui.rtl(root);

        TextView title = Ui.title(this, "دربك Tools", 30);
        title.setTextColor(Ui.GOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, Ui.dp(this, 62)));

        TextView sub = Ui.title(this, "أدوات الشاشة • نقل • ملفات • تنزيل • APK", 16);
        sub.setTextColor(Ui.MUTED);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(row);
        root.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));

        Button transfer = Ui.button(this, "📱\nاتصال الآيفون");
        Button files = Ui.button(this, "📁\nالملفات");
        Button downloads = Ui.button(this, "⬇\nالتنزيلات");
        Button apk = Ui.button(this, "APK\nالتطبيقات");

        row.addView(transfer, Ui.weighted(1, 7, this));
        row.addView(files, Ui.weighted(1, 7, this));
        row.addView(downloads, Ui.weighted(1, 7, this));
        row.addView(apk, Ui.weighted(1, 7, this));

        transfer.setOnClickListener(v -> startActivity(new Intent(this, TransferActivity.class)));
        files.setOnClickListener(v -> startActivity(new Intent(this, FileBrowserActivity.class)));
        downloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));
        apk.setOnClickListener(v -> startActivity(new Intent(this, ApkActivity.class)));

        TextView footer = Ui.title(this, "مصمم للاستخدام الخاص على Android 7.1 • 1024×600", 13);
        footer.setTextColor(Ui.MUTED);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, new LinearLayout.LayoutParams(-1, Ui.dp(this, 40)));
        return root;
    }

    private void requestStorageIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQ_STORAGE);
        }
    }
}

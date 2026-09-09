package com.abosultan.darbaktools;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.abosultan.darbakcore.DarbakAboutActivity;
import com.abosultan.darbakcore.DarbakCore;

public class MainActivity extends Activity {
    private static final int REQ_STORAGE = 40;
    private final Handler handler = new Handler();
    private TextView serverPill;
    private TextView phoneSubtitle;
    private TextView footer;

    private final Runnable updater = new Runnable() {
        @Override public void run() {
            refreshStatus();
            handler.postDelayed(this, 1800);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarbakCore.install(this);
        DarbakCore.prepareCarScreen(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestStorageIfNeeded();
        DarbakServerService.ensureStarted(this);
        setContentView(buildUi());
        handler.post(updater);
    }

    private LinearLayout buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(Ui.verticalGradient(this));
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 14), Ui.dp(this, 24), Ui.dp(this, 12));
        Ui.rtl(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Ui.rtl(header);
        root.addView(header, new LinearLayout.LayoutParams(-1, Ui.dp(this, 82)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        header.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = Ui.title(this, "دربك Tools", 30);
        title.setTextColor(Ui.TEXT);
        title.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        brand.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView sub = Ui.title(this, "مركز إدارة وتحكم شاشة السيارة", 14);
        sub.setTextColor(Ui.MUTED);
        sub.setGravity(Gravity.RIGHT | Gravity.TOP);
        brand.addView(sub, new LinearLayout.LayoutParams(-1, Ui.dp(this, 31)));

        serverPill = Ui.pill(this, "● جاري تشغيل الاتصال", Ui.MUTED);
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(Ui.dp(this, 220), Ui.dp(this, 44));
        pillParams.setMargins(Ui.dp(this, 12), 0, 0, 0);
        header.addView(serverPill, pillParams);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(row1);
        grid.addView(row1, new LinearLayout.LayoutParams(-1, 0, 1));

        phoneSubtitle = new TextView(this);
        LinearLayout phone = moduleCard("↔", "الآيفون والتحكم", "جاري اكتشاف عنوان الاتصال…", phoneSubtitle, Ui.BLUE);
        LinearLayout files = moduleCard("▣", "الملفات", "الذاكرة الداخلية • USB • SD • الوارد", null, Ui.BLUE);
        row1.addView(phone, Ui.weighted(1, 6, this));
        row1.addView(files, Ui.weighted(1, 6, this));

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(row2);
        grid.addView(row2, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout downloads = moduleCard("↓", "التنزيلات", "تنزيل رابط إلى الشاشة", null, Ui.BLUE);
        LinearLayout apk = moduleCard("APK", "مركز التطبيقات", "فحص • تثبيت • تحديث", null, Ui.BLUE);
        row2.addView(downloads, Ui.weighted(1, 6, this));
        row2.addView(apk, Ui.weighted(1, 6, this));

        phone.setOnClickListener(v -> startActivity(new Intent(this, TransferActivity.class)));
        files.setOnClickListener(v -> startActivity(new Intent(this, FileBrowserActivity.class)));
        downloads.setOnClickListener(v -> startActivity(new Intent(this, DownloadActivity.class)));
        apk.setOnClickListener(v -> startActivity(new Intent(this, ApkActivity.class)));

        footer = Ui.title(this, "حول دربك", 16);
        footer.setTextColor(Ui.MUTED);
        footer.setGravity(Gravity.CENTER);
        footer.setClickable(true);
        footer.setOnClickListener(v -> startActivity(new Intent(this, DarbakAboutActivity.class)));
        root.addView(footer, new LinearLayout.LayoutParams(-1, Ui.dp(this, 52)));
        return root;
    }

    private LinearLayout moduleCard(String icon, String titleText, String subtitleText, TextView externalSubtitle, int accent) {
        LinearLayout card = Ui.card(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        Ui.rtl(line);
        card.addView(line, new LinearLayout.LayoutParams(-1, -1));

        TextView iconView = Ui.title(this, icon, "APK".equals(icon) ? 20 : 34);
        iconView.setTextColor(accent);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(Ui.rounded(Ui.PANEL_2, Ui.LINE, 18, this));
        line.addView(iconView, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 76)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        line.addView(text, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = Ui.title(this, titleText, 22);
        title.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        text.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView subtitle = externalSubtitle != null ? externalSubtitle : Ui.title(this, subtitleText, 13);
        if (externalSubtitle != null) {
            externalSubtitle.setText(subtitleText);
            externalSubtitle.setTextSize(13);
            externalSubtitle.setTextColor(Ui.MUTED);
            externalSubtitle.setGravity(Gravity.RIGHT | Gravity.TOP);
            externalSubtitle.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        } else {
            subtitle.setTextColor(Ui.MUTED);
            subtitle.setGravity(Gravity.RIGHT | Gravity.TOP);
        }
        text.addView(subtitle, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        card.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) v.setAlpha(.82f);
            else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) v.setAlpha(1f);
            return false;
        });
        return card;
    }

    private void refreshStatus() {
        if (serverPill == null) return;
        String ip = NetworkUtils.localIpv4(this);
        boolean running = DarbakServerService.isRunning();
        if (running && ip != null) {
            int port = DarbakServerService.getPort();
            serverPill.setText("● الاتصال جاهز");
            serverPill.setTextColor(Ui.GREEN);
            phoneSubtitle.setText("http://" + ip + ":" + port + " • Safari");
            footer.setText("حول دربك");
        } else if (running) {
            serverPill.setText("● بانتظار الشبكة");
            serverPill.setTextColor(Ui.GOLD);
            phoneSubtitle.setText("الخادم يعمل • اتصل بالشبكة لظهور العنوان");
        } else {
            serverPill.setText("● الخادم متوقف");
            serverPill.setTextColor(Ui.RED);
            phoneSubtitle.setText("اضغط هنا لإعادة تشغيل مركز الاتصال");
        }
    }

    private void requestStorageIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
        } else {
            AppPaths.root();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AppPaths.root();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

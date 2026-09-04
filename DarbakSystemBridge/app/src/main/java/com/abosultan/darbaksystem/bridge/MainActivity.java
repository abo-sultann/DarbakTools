package com.abosultan.darbaksystem.bridge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int STORAGE_REQUEST = 41;
    private static final int GOLD = Color.rgb(210, 166, 70);
    private static final int GREEN = Color.rgb(16, 32, 27);
    private static final int PANEL = Color.rgb(25, 45, 39);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private TextView stateView;
    private TextView reportView;
    private String currentReport = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 18, 16));
        getWindow().setNavigationBarColor(Color.rgb(11, 18, 16));
        setContentView(buildScreen());
        refreshReport("فحص أولي فقط — لم يتم تعديل النظام");
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(24), dp(18), dp(24), dp(18));
        root.setBackgroundColor(Color.rgb(11, 18, 16));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setGravity(Gravity.TOP);
        controls.setPadding(dp(18), dp(10), dp(18), dp(10));
        controls.setBackground(rounded(PANEL, dp(16), GOLD, dp(1)));
        root.addView(controls, new LinearLayout.LayoutParams(dp(330), ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = text("Darbak System", 30, GOLD, true);
        title.setGravity(Gravity.CENTER);
        controls.addView(title, matchWrap(dp(4)));

        TextView subtitle = text("جسر الفحص والاتصال الآمن", 17, Color.WHITE, false);
        subtitle.setGravity(Gravity.CENTER);
        controls.addView(subtitle, matchWrap(dp(18)));

        stateView = text("جارٍ الفحص…", 17, Color.WHITE, true);
        stateView.setGravity(Gravity.CENTER);
        stateView.setPadding(dp(12), dp(14), dp(12), dp(14));
        stateView.setBackground(rounded(GREEN, dp(12), Color.rgb(64, 91, 81), dp(1)));
        controls.addView(stateView, matchWrap(dp(14)));

        Button refresh = button("تحديث الفحص");
        refresh.setOnClickListener(view -> refreshReport("تم تحديث معلومات الشاشة"));
        controls.addView(refresh, matchWrap(dp(10)));

        Button rootCheck = button("فحص صلاحية Root");
        rootCheck.setOnClickListener(view -> checkRoot());
        controls.addView(rootCheck, matchWrap(dp(10)));

        Button enable = button("تشغيل ADB عبر Wi-Fi — ضغط مطوّل");
        enable.setOnClickListener(view -> toast("اضغط الزر مطولًا للحماية"));
        enable.setOnLongClickListener(view -> {
            confirmEnable();
            return true;
        });
        controls.addView(enable, matchWrap(dp(10)));

        Button disable = button("إيقاف ADB عبر Wi-Fi — ضغط مطوّل");
        disable.setOnClickListener(view -> toast("اضغط الزر مطولًا للحماية"));
        disable.setOnLongClickListener(view -> {
            confirmDisable();
            return true;
        });
        controls.addView(disable, matchWrap(dp(10)));

        Button save = button("حفظ تقرير الفحص");
        save.setOnClickListener(view -> saveReportWithPermission());
        controls.addView(save, matchWrap(0));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(18), 0, 0, 0);
        root.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        reportView = text("", 16, Color.rgb(229, 235, 232), false);
        reportView.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        reportView.setGravity(Gravity.RIGHT);
        reportView.setLineSpacing(0f, 1.12f);
        reportView.setPadding(dp(22), dp(18), dp(22), dp(18));
        reportView.setBackground(rounded(Color.rgb(17, 29, 25), dp(16), Color.rgb(49, 75, 65), dp(1)));
        scroll.addView(reportView, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private void refreshReport(String state) {
        setBusy(state);
        worker.execute(() -> {
            String report = DeviceDiagnostics.buildReport(this);
            main.post(() -> {
                currentReport = report;
                reportView.setText(report);
                stateView.setText(state + "\nIP: " + DeviceDiagnostics.localIp(this));
            });
        });
    }

    private void checkRoot() {
        setBusy("جارٍ طلب صلاحية Root…");
        worker.execute(() -> {
            ShellResult result = RootShell.checkRoot();
            main.post(() -> {
                if (result.isSuccess() && result.output.contains("uid=0")) {
                    stateView.setText("Root متوفر وجاهز");
                } else if (result.output.contains("not allowed")) {
                    stateView.setText("su موجود لكنه يمنع التطبيقات العادية");
                } else {
                    stateView.setText("Root غير متاح أو لم تتم الموافقة");
                }
                toast(shortOutput(result));
                refreshReport(stateView.getText().toString());
            });
        });
    }

    private void confirmEnable() {
        new AlertDialog.Builder(this)
                .setTitle("تشغيل ADB عبر Wi-Fi مؤقتًا؟")
                .setMessage("سيتم ضبط المنفذ 5555 وإعادة تشغيل خدمة ADB فقط. لا تُعدّل ملفات النظام، ويُلغى الاتصال بإعادة تشغيل الشاشة أو من زر الإيقاف.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("تشغيل", (dialog, which) -> enableAdb())
                .show();
    }

    private void confirmDisable() {
        new AlertDialog.Builder(this)
                .setTitle("إيقاف ADB عبر Wi-Fi؟")
                .setMessage("سيتم إغلاق المنفذ اللاسلكي مع إبقاء خيار تصحيح USB كما هو.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إيقاف", (dialog, which) -> disableAdb())
                .show();
    }

    private void enableAdb() {
        setBusy("جارٍ تشغيل ADB…");
        worker.execute(() -> {
            ShellResult result = RootShell.enableTemporaryAdb();
            String port = RootShell.getProperty("service.adb.tcp.port");
            boolean propertySet = "5555".equals(port.trim());
            boolean reachable = DeviceDiagnostics.isAdbTcpReachable(this);
            main.post(() -> {
                String ip = DeviceDiagnostics.localIp(this);
                if (propertySet && reachable) {
                    stateView.setText("ADB يعمل مؤقتًا\n" + ip + ":5555");
                    toast("من اللابتوب: adb connect " + ip + ":5555");
                } else if (propertySet) {
                    stateView.setText("تم ضبط المنفذ لكن الخدمة لم تفتحه بعد");
                    toast("أوقف تصحيح USB وشغّله مرة واحدة ثم حدّث الفحص");
                } else {
                    stateView.setText("النظام منع تغيير منفذ ADB");
                    toast(shortOutput(result));
                }
                refreshReport(stateView.getText().toString());
            });
        });
    }

    private void disableAdb() {
        setBusy("جارٍ إيقاف ADB…");
        worker.execute(() -> {
            ShellResult result = RootShell.disableTemporaryAdb();
            boolean stopped = !DeviceDiagnostics.isAdbTcpReachable(this);
            main.post(() -> {
                stateView.setText(stopped ? "تم إيقاف ADB اللاسلكي" : "تعذر تأكيد إيقاف ADB اللاسلكي");
                toast(shortOutput(result));
                refreshReport(stateView.getText().toString());
            });
        });
    }

    private void saveReportWithPermission() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            return;
        }
        saveReport();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            saveReport();
        } else if (requestCode == STORAGE_REQUEST) {
            toast("لم تُمنح صلاحية حفظ التقرير");
        }
    }

    private void saveReport() {
        worker.execute(() -> {
            try {
                File directory = new File(Environment.getExternalStorageDirectory(), "DarbakSystem");
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("تعذر إنشاء المجلد");
                }
                File report = new File(directory, "DarbakSystem-report.txt");
                FileOutputStream stream = new FileOutputStream(report, false);
                stream.write(currentReport.getBytes(Charset.forName("UTF-8")));
                stream.flush();
                stream.close();
                main.post(() -> toast("تم الحفظ: /DarbakSystem/DarbakSystem-report.txt"));
            } catch (Exception error) {
                main.post(() -> toast("فشل الحفظ: " + error.getMessage()));
            }
        });
    }

    private void setBusy(String message) {
        stateView.setText(message);
    }

    private String shortOutput(ShellResult result) {
        if (result.timedOut) {
            return "انتهت المهلة؛ ربما ظهرت نافذة Root على الشاشة";
        }
        if (result.output.isEmpty()) {
            return result.isSuccess() ? "تم" : "لم ينفذ الأمر";
        }
        return result.output.length() > 180 ? result.output.substring(0, 180) : result.output;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(54));
        button.setPadding(dp(8), dp(6), dp(8), dp(6));
        button.setBackground(rounded(Color.rgb(36, 63, 53), dp(10), GOLD, dp(1)));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomMargin;
        return params;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}

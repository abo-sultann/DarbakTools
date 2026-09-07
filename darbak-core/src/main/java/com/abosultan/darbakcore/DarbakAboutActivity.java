package com.abosultan.darbakcore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/** Standard About screen for every Darbak application. */
public class DarbakAboutActivity extends Activity {
    public static final String META_UPDATE_MANIFEST = "com.abosultan.darbak.UPDATE_MANIFEST_URL";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarbakCore.install(this);
        DarbakCore.prepareCarScreen(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = DarbakUi.page(this);
        scroll.addView(page);

        page.addView(DarbakUi.title(this, "حول التطبيق"));
        TextView sub = DarbakUi.subtitle(this, "معلومات التطبيق والإصدار وحقوق الاستخدام");
        page.addView(sub);

        String appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
        DarbakDiagnostics.Snapshot snapshot = DarbakDiagnostics.inspect(this);

        LinearLayout identity = DarbakUi.card(this, appName, "من تطبيقات دربك");
        page.addView(identity, DarbakUi.cardParams(this));

        LinearLayout signature = DarbakUi.card(this, "دربك", "تصميم وتطوير  •  أبوسلطان");
        TextView mark = new TextView(this);
        mark.setText("دربك\nتصميم وتطوير\nأبوسلطان");
        mark.setTextColor(getResources().getColor(R.color.darbak_gold));
        mark.setTextSize(25);
        mark.setGravity(Gravity.CENTER);
        mark.setPadding(0, DarbakUi.dp(this, 10), 0, DarbakUi.dp(this, 4));
        signature.addView(mark, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(signature, DarbakUi.cardParams(this));

        LinearLayout version = DarbakUi.card(this, "النسخة الحالية", snapshot.versionName + "  (" + snapshot.versionCode + ")");
        version.setOnLongClickListener(v -> {
            startActivity(new Intent(this, DarbakDiagnosticsActivity.class));
            return true;
        });
        page.addView(version, DarbakUi.cardParams(this));

        LinearLayout update = DarbakUi.card(this, "التحقق من التحديثات", "فحص إصدار جديد والتحقق من سلامة ملف APK قبل التثبيت");
        update.setOnClickListener(v -> checkForUpdate());
        page.addView(update, DarbakUi.cardParams(this));

        LinearLayout rights = DarbakUi.card(this, "الملكية", "دربك • تصميم وتطوير أبوسلطان");
        page.addView(rights, DarbakUi.cardParams(this));

        setContentView(scroll);
    }

    private void checkForUpdate() {
        final String manifestUrl = readMeta(META_UPDATE_MANIFEST);
        if (manifestUrl == null || manifestUrl.trim().isEmpty()) {
            Toast.makeText(this, "مصدر التحديث غير مهيأ لهذا التطبيق", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "جارٍ التحقق من التحديث...", Toast.LENGTH_SHORT).show();
        DarbakUpdater.check(this, manifestUrl, new DarbakUpdater.CheckListener() {
            @Override public void onResult(DarbakUpdater.UpdateInfo info, boolean newer) {
                runOnUiThread(() -> {
                    if (!newer) {
                        Toast.makeText(DarbakAboutActivity.this, "أنت على أحدث إصدار", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String message = "الإصدار " + info.versionName;
                    if (info.notes != null && !info.notes.isEmpty()) message += "\n\n" + info.notes;
                    new AlertDialog.Builder(DarbakAboutActivity.this)
                            .setTitle("تحديث متوفر")
                            .setMessage(message)
                            .setNegativeButton("لاحقًا", null)
                            .setPositiveButton("تنزيل", (d, which) -> download(info))
                            .show();
                });
            }

            @Override public void onError(String message, Throwable error) {
                runOnUiThread(() -> Toast.makeText(DarbakAboutActivity.this, message, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void download(DarbakUpdater.UpdateInfo info) {
        DarbakUpdater.download(this, info, new DarbakUpdater.Listener() {
            private int lastShown = -20;
            @Override public void onProgress(int percent) {
                if (percent - lastShown >= 20 || percent == 100) {
                    lastShown = percent;
                    runOnUiThread(() -> Toast.makeText(DarbakAboutActivity.this,
                            "تنزيل التحديث " + percent + "%", Toast.LENGTH_SHORT).show());
                }
            }

            @Override public void onReady(DarbakUpdater.UpdateInfo readyInfo, File apk) {
                runOnUiThread(() -> DarbakUpdater.install(DarbakAboutActivity.this, apk));
            }

            @Override public void onError(String message, Throwable error) {
                runOnUiThread(() -> Toast.makeText(DarbakAboutActivity.this, message, Toast.LENGTH_LONG).show());
            }
        });
    }

    private String readMeta(String key) {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(),
                    getPackageManager().GET_META_DATA);
            return ai.metaData == null ? null : ai.metaData.getString(key);
        } catch (Exception ignored) {
            return null;
        }
    }
}

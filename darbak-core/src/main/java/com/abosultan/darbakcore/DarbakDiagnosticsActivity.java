package com.abosultan.darbakcore;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Hidden service/maintenance screen opened by long-pressing the version card in About. */
public class DarbakDiagnosticsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarbakCore.install(this);
        DarbakCore.prepareCarScreen(this);

        DarbakDiagnostics.Snapshot s = DarbakDiagnostics.inspect(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = DarbakUi.page(this);
        scroll.addView(page);

        page.addView(DarbakUi.title(this, "فحص التطبيق"));
        page.addView(DarbakUi.subtitle(this, "معلومات فنية للصيانة والتشخيص"));

        LinearLayout health = DarbakUi.card(this, "الحالة", healthArabic(s.health));
        health.addView(DarbakUi.statusPill(this, healthArabic(s.health), s.health == DarbakDiagnostics.Health.HEALTHY));
        page.addView(health, DarbakUi.cardParams(this));

        LinearLayout resources = DarbakUi.card(this, "الموارد",
                "التخزين المتاح: " + s.freeStorageMb + " MB   •   الذاكرة المتاحة للتطبيق: " + s.freeHeapMb + " MB");
        page.addView(resources, DarbakUi.cardParams(this));

        LinearLayout details = DarbakUi.card(this, "التقرير الفني", "اضغط لإنشاء نسخة محفوظة داخل مجلد تقارير دربك");
        TextView report = new TextView(this);
        report.setText(s.report);
        report.setTextColor(getResources().getColor(R.color.darbak_text_secondary));
        report.setTextSize(14);
        report.setGravity(Gravity.START);
        report.setTextDirection(TextView.TEXT_DIRECTION_LTR);
        details.addView(report);
        details.setOnClickListener(v -> {
            try {
                java.io.File file = DarbakDiagnostics.export(this);
                Toast.makeText(this, "تم حفظ التقرير: " + file.getName(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "تعذر حفظ التقرير", Toast.LENGTH_LONG).show();
            }
        });
        page.addView(details, DarbakUi.cardParams(this));

        LinearLayout crash = DarbakUi.card(this, "آخر انهيار", lastCrashSummary());
        page.addView(crash, DarbakUi.cardParams(this));

        setContentView(scroll);
    }

    private String healthArabic(DarbakDiagnostics.Health health) {
        switch (health) {
            case CRITICAL: return "يحتاج تدخل";
            case WARNING: return "تنبيه";
            default: return "سليم";
        }
    }

    private String lastCrashSummary() {
        java.io.File file = new java.io.File(DarbakCore.reportsDir(this), "last_crash.txt");
        if (!file.exists()) return "لا يوجد انهيار مسجل";
        return "يوجد تقرير انهيار محفوظ • " + file.length() + " بايت";
    }
}

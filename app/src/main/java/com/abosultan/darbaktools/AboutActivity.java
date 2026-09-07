package com.abosultan.darbaktools;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Darbak unified About page reference.
 *
 * Long-press the version pill to open the hidden technical diagnostics.
 */
public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(buildUi());
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.verticalGradient(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 26), Ui.dp(this, 18), Ui.dp(this, 26), Ui.dp(this, 18));
        Ui.rtl(root);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Ui.rtl(header);
        root.addView(header, new LinearLayout.LayoutParams(-1, Ui.dp(this, 64)));

        TextView back = Ui.pill(this, "‹ رجوع", Ui.BLUE);
        back.setClickable(true);
        back.setFocusable(true);
        Ui.applyPressFeedback(back);
        back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(Ui.dp(this, 120), Ui.dp(this, 44)));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        header.addView(heading, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = Ui.title(this, "حول التطبيق", 28);
        title.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        heading.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView subtitle = Ui.title(this, "معلومات التطبيق والإصدار والهوية", 13);
        subtitle.setTextColor(Ui.MUTED);
        subtitle.setGravity(Gravity.RIGHT | Gravity.TOP);
        heading.addView(subtitle, new LinearLayout.LayoutParams(-1, Ui.dp(this, 28)));

        LinearLayout identity = Ui.card(this);
        identity.setGravity(Gravity.CENTER);
        root.addView(identity, margin(-1, Ui.dp(this, 238), 6));

        int signatureId = getResources().getIdentifier("darbak_signature_about", "drawable", getPackageName());
        if (signatureId != 0) {
            ImageView signature = new ImageView(this);
            signature.setImageResource(signatureId);
            signature.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            signature.setContentDescription("دربك • تصميم وتطوير • أبوسلطان");
            identity.addView(signature, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            TextView brand = Ui.title(this, "دربك", 42);
            brand.setTextColor(Ui.BLUE);
            brand.setGravity(Gravity.CENTER);
            identity.addView(brand, new LinearLayout.LayoutParams(-1, 0, 1));

            TextView owner = Ui.title(this, "تصميم وتطوير • أبوسلطان", 24);
            owner.setTextColor(Ui.TEXT);
            owner.setGravity(Gravity.CENTER);
            identity.addView(owner, new LinearLayout.LayoutParams(-1, 0, 1));
        }

        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        Ui.rtl(infoRow);
        root.addView(infoRow, new LinearLayout.LayoutParams(-1, Ui.dp(this, 112)));

        TextView version = infoCard("النسخة الحالية", "v" + DarbakDiagnostics.versionName(this), Ui.GREEN);
        version.setClickable(true);
        version.setFocusable(true);
        version.setOnLongClickListener(v -> {
            showDiagnostics();
            return true;
        });
        Ui.applyPressFeedback(version);

        TextView platform = infoCard("المنصة", "Android " + android.os.Build.VERSION.RELEASE, Ui.BLUE);
        TextView identityInfo = infoCard("الهوية", "Darbak UI V1", Ui.CYAN);

        infoRow.addView(version, Ui.weighted(1, 5, this));
        infoRow.addView(platform, Ui.weighted(1, 5, this));
        infoRow.addView(identityInfo, Ui.weighted(1, 5, this));

        TextView note = Ui.title(this,
                "هوية موحدة لتطبيقات دربك • مصممة لشاشة السيارة 1024×600 • Android 7.1 / API 25",
                13);
        note.setTextColor(Ui.MUTED);
        note.setGravity(Gravity.CENTER);
        root.addView(note, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        return scroll;
    }

    private TextView infoCard(String label, String value, int accent) {
        TextView v = Ui.title(this, label + "\n" + value, 15);
        v.setTextColor(Ui.TEXT);
        v.setGravity(Gravity.CENTER);
        v.setBackground(Ui.rounded(Ui.PANEL, Ui.LINE, 16, this));
        v.setCompoundDrawablePadding(Ui.dp(this, 4));
        return v;
    }

    private LinearLayout.LayoutParams margin(int width, int height, int dp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.setMargins(Ui.dp(this, dp), Ui.dp(this, dp), Ui.dp(this, dp), Ui.dp(this, dp));
        return p;
    }

    private void showDiagnostics() {
        TextView text = Ui.title(this, DarbakDiagnostics.buildSummary(this), 14);
        text.setTextColor(Ui.TEXT);
        text.setTextDirection(View.TEXT_DIRECTION_RTL);
        text.setPadding(Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("تشخيص دربك")
                .setView(text)
                .setPositiveButton("إغلاق", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.BLUE));
        dialog.show();
    }
}

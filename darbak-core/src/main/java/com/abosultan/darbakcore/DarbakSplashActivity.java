package com.abosultan.darbakcore;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Optional lightweight splash for Android 7.1. No animation framework or heavy asset required. */
public class DarbakSplashActivity extends Activity {
    public static final String META_TARGET = "com.abosultan.darbak.SPLASH_TARGET";
    public static final long DEFAULT_DELAY_MS = 650L;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarbakCore.install(this);
        DarbakCore.prepareCarScreen(this);

        LinearLayout page = DarbakUi.page(this);
        page.setGravity(Gravity.CENTER);
        String appName = getApplicationInfo().loadLabel(getPackageManager()).toString();

        TextView name = DarbakUi.title(this, appName);
        name.setGravity(Gravity.CENTER);
        page.addView(name);

        TextView family = DarbakUi.subtitle(this, "دربك");
        family.setGravity(Gravity.CENTER);
        family.setTextSize(24);
        family.setTextColor(getResources().getColor(R.color.darbak_gold));
        page.addView(family);

        TextView owner = DarbakUi.subtitle(this, "تصميم وتطوير • أبوسلطان");
        owner.setGravity(Gravity.CENTER);
        page.addView(owner);

        setContentView(page);
        new Handler(Looper.getMainLooper()).postDelayed(this::openTarget, DEFAULT_DELAY_MS);
    }

    private void openTarget() {
        String className = readTarget();
        if (className != null && !className.trim().isEmpty()) {
            try {
                Intent i = new Intent();
                i.setClassName(this, className);
                startActivity(i);
            } catch (Throwable ignored) { }
        }
        finish();
    }

    private String readTarget() {
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(),
                    getPackageManager().GET_META_DATA);
            return ai.metaData == null ? null : ai.metaData.getString(META_TARGET);
        } catch (Exception ignored) {
            return null;
        }
    }
}

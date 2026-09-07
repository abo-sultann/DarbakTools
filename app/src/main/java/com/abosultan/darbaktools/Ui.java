package com.abosultan.darbaktools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Darbak Settings UI — V1
 *
 * المرجع البصري الموحد لتطبيقات دربك على شاشة السيارة.
 * Android 7.1 / API 25 / 1024x600 / RTL.
 *
 * ملاحظة: GOLD محفوظ كاسم قديم للتوافق مع الشاشات الحالية، لكنه يشير
 * إلى اللون الأزرق الأساسي في الهوية الجديدة.
 */
public final class Ui {
    public static final int BG = Color.rgb(3, 14, 27);
    public static final int BG_2 = Color.rgb(5, 23, 42);
    public static final int PANEL = Color.rgb(8, 29, 49);
    public static final int PANEL_2 = Color.rgb(10, 43, 72);
    public static final int BLUE = Color.rgb(55, 181, 255);
    public static final int BLUE_DEEP = Color.rgb(30, 119, 205);
    public static final int GOLD = BLUE; // Backward-compatible alias.
    public static final int TEXT = Color.rgb(247, 250, 255);
    public static final int MUTED = Color.rgb(168, 190, 212);
    public static final int GREEN = Color.rgb(49, 230, 161);
    public static final int YELLOW = Color.rgb(255, 201, 77);
    public static final int RED = Color.rgb(255, 93, 108);
    public static final int PURPLE = Color.rgb(169, 124, 255);
    public static final int CYAN = Color.rgb(55, 230, 241);
    public static final int LINE = Color.rgb(44, 105, 157);

    private Ui() {}

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, int strokeColor, int radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeColor != 0) d.setStroke(dp(c, 1), strokeColor);
        return d;
    }

    public static GradientDrawable verticalGradient(Context c) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG_2, BG});
        d.setCornerRadius(0);
        return d;
    }

    public static TextView title(Context c, String text, float sp) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(sp);
        v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        v.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), dp(c, 6));
        return v;
    }

    public static TextView pill(Context c, String text, int color) {
        TextView v = title(c, text, 13);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setBackground(rounded(Color.rgb(6, 35, 58), LINE, 24, c));
        v.setPadding(dp(c, 14), dp(c, 7), dp(c, 14), dp(c, 7));
        return v;
    }

    public static TextView statusPill(Context c, String text, Status status) {
        return pill(c, text, statusColor(status));
    }

    public static int statusColor(Status status) {
        if (status == null) return MUTED;
        switch (status) {
            case OK: return GREEN;
            case WARNING: return YELLOW;
            case ERROR: return RED;
            case INFO: return BLUE;
            default: return MUTED;
        }
    }

    public static Button button(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(PANEL_2, LINE, 14, c));
        b.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        return b;
    }

    public static Button primaryButton(Context c, String text) {
        Button b = button(c, text);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(BLUE_DEEP, BLUE, 14, c));
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        card.setBackground(rounded(PANEL, LINE, 18, c));
        card.setElevation(dp(c, 2));
        rtl(card);
        return card;
    }

    public static void applyPressFeedback(View v) {
        v.setOnTouchListener((view, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                view.setAlpha(.84f);
                view.setScaleX(.992f);
                view.setScaleY(.992f);
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                view.setAlpha(1f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
            return false;
        });
    }

    public static LinearLayout.LayoutParams weighted(int weight, int marginDp, Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(dp(c, marginDp), dp(c, marginDp), dp(c, marginDp), dp(c, marginDp));
        return p;
    }

    public static LinearLayout.LayoutParams weightedWrap(int weight, int marginDp, Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        p.setMargins(dp(c, marginDp), dp(c, marginDp), dp(c, marginDp), dp(c, marginDp));
        return p;
    }

    public static void rtl(View v) {
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }

    public enum Status {
        OK,
        WARNING,
        ERROR,
        INFO,
        NEUTRAL
    }
}

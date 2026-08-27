package com.abosultan.darbaktools;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(7, 26, 22);
    public static final int PANEL = Color.rgb(15, 49, 41);
    public static final int GOLD = Color.rgb(212, 175, 55);
    public static final int TEXT = Color.WHITE;
    public static final int MUTED = Color.rgb(190, 202, 198);

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

    public static TextView title(Context c, String text, float sp) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(sp);
        v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        v.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), dp(c, 6));
        return v;
    }

    public static Button button(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setTextSize(19);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(rounded(PANEL, GOLD, 14, c));
        b.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return b;
    }

    public static LinearLayout.LayoutParams weighted(int weight, int marginDp, Context c) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(dp(c, marginDp), dp(c, marginDp), dp(c, marginDp), dp(c, marginDp));
        return p;
    }

    public static void rtl(View v) {
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }
}

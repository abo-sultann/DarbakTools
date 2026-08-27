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
    public static final int BG = Color.rgb(5, 20, 16);
    public static final int PANEL = Color.rgb(13, 40, 33);
    public static final int PANEL_2 = Color.rgb(18, 54, 45);
    public static final int GOLD = Color.rgb(216, 180, 90);
    public static final int TEXT = Color.rgb(247, 249, 248);
    public static final int MUTED = Color.rgb(159, 179, 172);
    public static final int GREEN = Color.rgb(66, 211, 146);
    public static final int RED = Color.rgb(255, 117, 109);
    public static final int LINE = Color.rgb(38, 75, 64);

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
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(7, 27, 22), BG});
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
        v.setBackground(rounded(Color.rgb(8, 31, 25), LINE, 24, c));
        v.setPadding(dp(c, 14), dp(c, 7), dp(c, 14), dp(c, 7));
        return v;
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
        b.setTextColor(Color.rgb(16, 34, 28));
        b.setBackground(rounded(GOLD, GOLD, 14, c));
        return b;
    }

    public static LinearLayout card(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16));
        card.setBackground(rounded(PANEL, LINE, 18, c));
        rtl(card);
        return card;
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
}

package com.abosultan.darbakcore;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small UI factory that keeps Darbak screens visually consistent without heavy frameworks. */
public final class DarbakUi {
    private DarbakUi() {}

    public static LinearLayout page(Activity activity) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.setGravity(Gravity.TOP | Gravity.END);
        root.setPadding(dp(activity, 28), dp(activity, 22), dp(activity, 28), dp(activity, 22));
        root.setBackgroundColor(activity.getResources().getColor(R.color.darbak_bg));
        return root;
    }

    public static TextView title(Activity activity, String text) {
        TextView v = new TextView(activity);
        v.setText(text);
        v.setTextColor(activity.getResources().getColor(R.color.darbak_text));
        v.setTextSize(34);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.END);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    public static TextView subtitle(Activity activity, String text) {
        TextView v = new TextView(activity);
        v.setText(text);
        v.setTextColor(activity.getResources().getColor(R.color.darbak_text_secondary));
        v.setTextSize(18);
        v.setGravity(Gravity.END);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    public static LinearLayout card(Activity activity, String heading, String description) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setMinimumHeight(dp(activity, 96));
        card.setPadding(dp(activity, 22), dp(activity, 14), dp(activity, 22), dp(activity, 14));
        card.setBackgroundResource(R.drawable.darbak_glass_card);

        TextView h = new TextView(activity);
        h.setText(heading);
        h.setTextColor(activity.getResources().getColor(R.color.darbak_text));
        h.setTextSize(23);
        h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        h.setGravity(Gravity.END);

        TextView d = new TextView(activity);
        d.setText(description == null ? "" : description);
        d.setTextColor(activity.getResources().getColor(R.color.darbak_text_secondary));
        d.setTextSize(16);
        d.setGravity(Gravity.END);

        card.addView(h, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        card.addView(d, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    public static TextView statusPill(Activity activity, String text, boolean success) {
        TextView v = new TextView(activity);
        v.setText(text);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(15);
        v.setTextColor(activity.getResources().getColor(success ? R.color.darbak_success : R.color.darbak_text_secondary));
        if (success) v.setBackgroundResource(R.drawable.darbak_status_success);
        v.setMinHeight(dp(activity, 40));
        v.setMinWidth(dp(activity, 110));
        return v;
    }

    public static LinearLayout.LayoutParams cardParams(Activity activity) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(activity, 10);
        return p;
    }

    public static int dp(Activity activity, int dp) {
        return Math.round(dp * activity.getResources().getDisplayMetrics().density);
    }
}

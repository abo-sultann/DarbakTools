package com.abosultan.darbakcore;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Standard visual states: loading, success, empty, error, permission and storage warnings. */
public final class DarbakStateView {
    public enum State { LOADING, SUCCESS, EMPTY, ERROR, PERMISSION, STORAGE }

    private DarbakStateView() {}

    public static View create(Activity activity, State state, String detail) {
        LinearLayout box = DarbakUi.card(activity, title(state), detail == null ? defaultDetail(state) : detail);
        TextView symbol = new TextView(activity);
        symbol.setText(symbol(state));
        symbol.setTextSize(32);
        symbol.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        symbol.setGravity(Gravity.CENTER);
        symbol.setTextColor(activity.getResources().getColor(color(state)));
        box.addView(symbol, 0);
        return box;
    }

    private static String title(State s) {
        switch (s) {
            case LOADING: return "جارٍ التحميل";
            case SUCCESS: return "تم بنجاح";
            case EMPTY: return "لا توجد بيانات";
            case ERROR: return "حدثت مشكلة";
            case PERMISSION: return "صلاحية مطلوبة";
            case STORAGE: return "مساحة التخزين منخفضة";
            default: return "الحالة";
        }
    }

    private static String defaultDetail(State s) {
        switch (s) {
            case LOADING: return "يرجى الانتظار قليلًا";
            case SUCCESS: return "اكتملت العملية";
            case EMPTY: return "لا يوجد محتوى لعرضه الآن";
            case ERROR: return "تعذر إكمال العملية، حاول مرة أخرى";
            case PERMISSION: return "يلزم السماح بهذه الصلاحية لإكمال الوظيفة";
            case STORAGE: return "حرر مساحة ثم حاول مرة أخرى";
            default: return "";
        }
    }

    private static String symbol(State s) {
        switch (s) {
            case SUCCESS: return "✓";
            case ERROR: return "!";
            case PERMISSION: return "◈";
            case STORAGE: return "▰";
            case EMPTY: return "—";
            default: return "…";
        }
    }

    private static int color(State s) {
        switch (s) {
            case SUCCESS: return R.color.darbak_success;
            case ERROR: return R.color.darbak_error;
            case STORAGE: return R.color.darbak_warning;
            default: return R.color.darbak_primary;
        }
    }
}

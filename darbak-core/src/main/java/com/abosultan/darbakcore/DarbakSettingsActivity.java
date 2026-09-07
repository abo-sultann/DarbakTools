package com.abosultan.darbakcore;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Approved Darbak Settings UI V1 shell for classic Android View apps.
 * Host apps provide only the real sections they actually support.
 */
public abstract class DarbakSettingsActivity extends Activity {
    public static final class Section {
        public final String id;
        public final String title;
        public final String description;
        public final String status;
        public final boolean statusGood;

        public Section(String id, String title, String description) {
            this(id, title, description, null, false);
        }

        public Section(String id, String title, String description, String status, boolean statusGood) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.status = status;
            this.statusGood = statusGood;
        }
    }

    private final List<Section> allSections = new ArrayList<>();
    private GridLayout grid;

    protected abstract List<Section> provideSections();
    protected abstract void onSectionSelected(Section section);

    protected String pageSubtitle() {
        return "كل ما تحتاجه في مكان واحد";
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarbakCore.install(this);
        DarbakCore.prepareCarScreen(this);

        List<Section> supplied = provideSections();
        if (supplied != null) allSections.addAll(supplied);

        LinearLayout page = DarbakUi.page(this);
        page.setPadding(DarbakUi.dp(this, 26), DarbakUi.dp(this, 14), DarbakUi.dp(this, 26), DarbakUi.dp(this, 14));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        TextView title = DarbakUi.title(this, "الإعدادات");
        titleBox.addView(title);
        titleBox.addView(DarbakUi.subtitle(this, pageSubtitle()));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        EditText search = new EditText(this);
        search.setHint("بحث في الإعدادات ...");
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setTextColor(getResources().getColor(R.color.darbak_text));
        search.setHintTextColor(getResources().getColor(R.color.darbak_text_secondary));
        search.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        search.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        search.setBackgroundResource(R.drawable.darbak_glass_card);
        search.setPadding(DarbakUi.dp(this, 18), 0, DarbakUi.dp(this, 18), 0);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(DarbakUi.dp(this, 430), DarbakUi.dp(this, 62));
        searchParams.setMarginEnd(DarbakUi.dp(this, 18));
        header.addView(search, searchParams);
        page.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DarbakUi.dp(this, 92)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(grid, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(s == null ? "" : s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        render("");
        setContentView(page);
    }

    private void render(String query) {
        grid.removeAllViews();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (Section section : allSections) {
            if (!q.isEmpty()) {
                String haystack = (section.title + " " + section.description).toLowerCase(Locale.ROOT);
                if (!haystack.contains(q)) continue;
            }

            LinearLayout card = DarbakUi.card(this, section.title, section.description);
            if (section.status != null && !section.status.trim().isEmpty()) {
                TextView status = DarbakUi.statusPill(this, section.status, section.statusGood);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                sp.topMargin = DarbakUi.dp(this, 8);
                card.addView(status, sp);
            }
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> onSectionSelected(section));
            card.setOnTouchListener((v, e) -> {
                switch (e.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN: v.setAlpha(.82f); break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL: v.setAlpha(1f); break;
                }
                return false;
            });

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = DarbakUi.dp(this, 224);
            p.height = DarbakUi.dp(this, 132);
            p.setMargins(DarbakUi.dp(this, 6), DarbakUi.dp(this, 6), DarbakUi.dp(this, 6), DarbakUi.dp(this, 6));
            grid.addView(card, p);
        }
    }
}

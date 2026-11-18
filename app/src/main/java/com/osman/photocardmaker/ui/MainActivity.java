package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdsManager;
import com.osman.photocardmaker.core.AppModule;
import com.osman.photocardmaker.core.ModuleRegistry;
import com.osman.photocardmaker.storage.LocalStorage;
import com.osman.photocardmaker.templates.LibraryLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Home dashboard.
 * - "Template library": one card per folder found in assets/templates.
 * - "Your space": the feature modules registered in ModuleRegistry.
 */
public class MainActivity extends Activity {

    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure the local folders exist.
        new LocalStorage(this);
        inflater = LayoutInflater.from(this);

        // Ask for ad consent where required, then start the ads SDK.
        AdsManager.start(this);

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        LinearLayout root = (LinearLayout) findViewById(R.id.contentRoot);
        addHero(root);

        // Section 1: template categories (read from the assets folder)
        List<View> categoryCards = new ArrayList<View>();
        for (final LibraryLoader.Category c : LibraryLoader.load(this)) {
            String desc = c.isEmpty()
                    ? c.description
                    : getString(R.string.n_templates, c.templates.size());
            categoryCards.add(makeCard(c.letter, c.color, c.title, desc,
                    c.isEmpty(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ModuleLauncher.openCategory(MainActivity.this, c.folder);
                        }
                    }));
        }
        addSection(root, R.string.home_sec_library, categoryCards);

        // Native ad between the two sections (hidden until an ad is loaded)
        FrameLayout nativeSlot = new FrameLayout(this);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.setMargins(dp(this, 6), dp(this, 14), dp(this, 6), 0);
        root.addView(nativeSlot, nlp);
        AdsManager.loadNative(this, nativeSlot);

        // Section 2: feature modules
        List<View> moduleCards = new ArrayList<View>();
        for (final AppModule m : ModuleRegistry.getAll()) {
            moduleCards.add(makeCard(m.getIconText(),
                    getResources().getColor(m.getColorRes()),
                    getString(m.getTitleRes()), getString(m.getDescRes()),
                    !m.isReady(), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ModuleLauncher.open(MainActivity.this, m);
                        }
                    }));
        }
        addSection(root, R.string.home_sec_space, moduleCards);

        TextView footer = new TextView(this);
        footer.setText(R.string.home_footer);
        footer.setTextSize(11);
        footer.setTextColor(getResources().getColor(R.color.text_sub));
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(this, 22), 0, 0);
        root.addView(footer);
    }

    private void addHero(LinearLayout root) {
        View hero = inflater.inflate(R.layout.item_hero, root, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(this, 6), 0, dp(this, 6), dp(this, 6));
        root.addView(hero, lp);

        hero.findViewById(R.id.btnHeroBlank).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openModule("custom");
            }
        });
        hero.findViewById(R.id.btnHeroTemplates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openModule("projects");
            }
        });
    }

    private void openModule(String id) {
        AppModule m = ModuleRegistry.findById(id);
        if (m != null) {
            ModuleLauncher.open(this, m);
        }
    }

    private void addSection(LinearLayout root, int titleRes, List<View> cards) {
        if (cards.isEmpty()) {
            return;
        }
        TextView title = new TextView(this);
        title.setText(titleRes);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_main));
        title.setPadding(dp(this, 8), dp(this, 18), dp(this, 8), dp(this, 6));
        root.addView(title);

        for (int i = 0; i < cards.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(cards.get(i), cellParams());
            if (i + 1 < cards.size()) {
                row.addView(cards.get(i + 1), cellParams());
            } else {
                row.addView(new View(this), cellParams());
            }
            root.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private LinearLayout.LayoutParams cellParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        int m = dp(this, 6);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    private View makeCard(String iconText, int color, String titleText, String descText,
                          boolean comingSoon, View.OnClickListener click) {
        View v = inflater.inflate(R.layout.item_module, null, false);

        TextView icon = (TextView) v.findViewById(R.id.tvIcon);
        TextView title = (TextView) v.findViewById(R.id.tvTitle);
        TextView desc = (TextView) v.findViewById(R.id.tvDesc);
        TextView status = (TextView) v.findViewById(R.id.tvStatus);

        GradientDrawable badge = new GradientDrawable();
        badge.setShape(GradientDrawable.RECTANGLE);
        badge.setCornerRadius(dp(this, 13));
        badge.setColor(color);
        icon.setBackgroundDrawable(badge);
        icon.setText(iconText);

        title.setText(titleText);
        desc.setText(descText);

        if (comingSoon) {
            status.setVisibility(View.VISIBLE);
            status.setText(R.string.coming_soon);
            status.setTextColor(getResources().getColor(R.color.badge_soon));
        } else {
            status.setVisibility(View.GONE);
        }
        v.setOnClickListener(click);
        return v;
    }

    @Override
    protected void onDestroy() {
        AdsManager.destroyNative();
        super.onDestroy();
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }
}

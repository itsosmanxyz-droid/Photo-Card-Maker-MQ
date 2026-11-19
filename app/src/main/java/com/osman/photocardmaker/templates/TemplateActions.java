package com.osman.photocardmaker.templates;

import android.app.Activity;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.activities.EditorActivity;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.utils.Prefs;
import com.osman.photocardmaker.utils.Util;

public class TemplateActions {

    public static void open(Activity a, Template t) {
        if (t.premium && !Prefs.isUnlocked(a, t.id)) {
            new AlertDialog.Builder(a)
                    .setTitle(R.string.premium_title)
                    .setMessage(R.string.premium_msg)
                    .setPositiveButton(R.string.watch_ad, (d, w) ->
                            AdManager.showRewarded(a,
                                    () -> {
                                        Prefs.unlock(a, t.id);
                                        Util.toast(a, R.string.unlocked);
                                        start(a, t);
                                    },
                                    () -> Util.toast(a, R.string.ad_not_ready)))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }
        start(a, t);
    }

    private static void start(Activity a, Template t) {
        if (t.custom) askSize(a, t); else launch(a, t, 0, 0);
    }

    private static void askSize(Activity a, Template t) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int p = Util.dp(a, 20);
        row.setPadding(p, p / 2, p, 0);
        EditText w = new EditText(a), h = new EditText(a);
        w.setInputType(InputType.TYPE_CLASS_NUMBER);
        h.setInputType(InputType.TYPE_CLASS_NUMBER);
        w.setHint(R.string.width);
        h.setHint(R.string.height);
        w.setText(String.valueOf(t.width));
        h.setText(String.valueOf(t.height));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(w, lp);
        row.addView(h, lp);
        new AlertDialog.Builder(a)
                .setTitle(R.string.custom_size_title)
                .setView(row)
                .setPositiveButton(R.string.create, (d, x) -> {
                    try {
                        int ww = Integer.parseInt(w.getText().toString().trim());
                        int hh = Integer.parseInt(h.getText().toString().trim());
                        if (ww < 100 || hh < 100 || ww > 4000 || hh > 4000) throw new NumberFormatException();
                        launch(a, t, ww, hh);
                    } catch (NumberFormatException e) {
                        Util.toast(a, R.string.invalid_size);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void launch(Activity a, Template t, int w, int h) {
        AdManager.maybeShowInterstitial(a, () -> {
            Intent i = new Intent(a, EditorActivity.class);
            i.putExtra(EditorActivity.EXTRA_TEMPLATE_ID, t.id);
            if (w > 0) { i.putExtra(EditorActivity.EXTRA_W, w); i.putExtra(EditorActivity.EXTRA_H, h); }
            a.startActivity(i);
        });
    }
}

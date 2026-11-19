package com.osman.photocardmaker.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import com.osman.photocardmaker.R;

import java.util.function.Consumer;

public class ColorPicker {
    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFF000000, 0xFF9E9E9E, 0xFF424242, 0xFFF44336, 0xFFE91E63,
            0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4,
            0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107,
            0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF006A4E, 0xFFF42A41, 0xFF1A237E};

    public static void show(Context c, String title, boolean allowNone, Consumer<Integer> cb) {
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Util.dp(c, 16);
        root.setPadding(pad, pad / 2, pad, 0);
        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(6);
        final AlertDialog[] dlg = new AlertDialog[1];
        for (final int col : PALETTE) {
            View v = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            g.setStroke(Util.dp(c, 1), 0x66888888);
            v.setBackground(g);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Util.dp(c, 40);
            lp.height = Util.dp(c, 40);
            lp.setMargins(Util.dp(c, 4), Util.dp(c, 4), Util.dp(c, 4), Util.dp(c, 4));
            v.setLayoutParams(lp);
            v.setOnClickListener(x -> { cb.accept(col); if (dlg[0] != null) dlg[0].dismiss(); });
            grid.addView(v);
        }
        root.addView(grid);
        final EditText hex = new EditText(c);
        hex.setHint(R.string.hex_hint);
        hex.setSingleLine(true);
        root.addView(hex);
        ScrollView sv = new ScrollView(c);
        sv.addView(root);
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String s = hex.getText().toString().trim();
                    if (s.isEmpty()) return;
                    if (!s.startsWith("#")) s = "#" + s;
                    try { cb.accept(Color.parseColor(s)); }
                    catch (Exception e) { Util.toast(c, R.string.invalid_color); }
                })
                .setNegativeButton(R.string.cancel, null);
        if (allowNone) b.setNeutralButton(R.string.none, (d, w) -> cb.accept(0));
        dlg[0] = b.show();
    }
}

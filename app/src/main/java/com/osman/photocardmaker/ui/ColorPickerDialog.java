package com.osman.photocardmaker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Simple colour picker: preset swatches plus a hex field (#RRGGBB or #AARRGGBB). */
public final class ColorPickerDialog {

    public interface OnColor {
        void onColor(int color);
    }

    private static final int[] PRESETS = {
            0x00000000, 0xFFFFFFFF, 0xFF000000, 0xFF212121, 0xFF616161, 0xFFBDBDBD,
            0xFFC62828, 0xFFD50000, 0xFFFF1744, 0xFFAD1457, 0xFFD81B60, 0xFFFF80AB,
            0xFFEF6C00, 0xFFFF6F00, 0xFFFFC107, 0xFFFFD600, 0xFFFFEB3B, 0xFFFFD54F,
            0xFF2E7D32, 0xFF006A4E, 0xFF00695C, 0xFF00897B, 0xFF66BB6A, 0xFF69F0AE,
            0xFF1565C0, 0xFF0D1B3E, 0xFF0277BD, 0xFF00E5FF, 0xFF4FC3F7, 0xFF283593,
            0xFF311B92, 0xFF6A1B9A, 0xFF7B1FA2, 0xFFE040FB, 0xFF4E342E, 0xFF263238
    };

    private ColorPickerDialog() {
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static String toHex(int color) {
        return String.format("#%08X", color);
    }

    public static void show(final Context ctx, int initial, final OnColor listener) {
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 16);
        root.setPadding(pad, pad, pad, pad);

        final View preview = new View(ctx);
        root.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(ctx, 44)));

        final EditText hex = new EditText(ctx);
        hex.setSingleLine(true);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setText(toHex(initial));
        hex.setHint("#RRGGBB or #AARRGGBB");
        root.addView(hex);

        setPreview(preview, initial);

        hex.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(android.text.Editable e) {
                Integer v = parse(e.toString());
                if (v != null) {
                    setPreview(preview, v.intValue());
                }
            }
        });

        LinearLayout row = null;
        for (int i = 0; i < PRESETS.length; i++) {
            if (i % 6 == 0) {
                row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                root.addView(row);
            }
            final int color = PRESETS[i];
            TextView sw = new TextView(ctx);
            sw.setGravity(Gravity.CENTER);
            sw.setTextSize(9);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setStroke(dp(ctx, 1), 0xFF9E9E9E);
            if (color == 0) {
                g.setColor(0xFFF5F5F5);
                sw.setText("none");
                sw.setTextColor(0xFF757575);
            } else {
                g.setColor(color);
            }
            sw.setBackgroundDrawable(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 38), dp(ctx, 38));
            lp.setMargins(dp(ctx, 4), dp(ctx, 4), dp(ctx, 4), dp(ctx, 4));
            row.addView(sw, lp);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hex.setText(toHex(color));
                }
            });
        }

        new AlertDialog.Builder(ctx)
                .setTitle("Choose colour")
                .setView(root)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Integer v = parse(hex.getText().toString());
                        if (v == null) {
                            Toast.makeText(ctx, "Invalid colour code", Toast.LENGTH_SHORT).show();
                        } else {
                            listener.onColor(v.intValue());
                        }
                    }
                })
                .show();
    }

    private static void setPreview(View v, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setStroke(2, 0xFF9E9E9E);
        v.setBackgroundDrawable(g);
    }

    private static Integer parse(String s) {
        String t = s.trim();
        if (t.length() == 0) {
            return null;
        }
        if (t.charAt(0) != '#') {
            t = "#" + t;
        }
        if (t.length() != 7 && t.length() != 9) {
            return null;
        }
        try {
            return Integer.valueOf(Color.parseColor(t));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.osman.photocardmaker.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/**
 * Builds the editing controls (text boxes, sliders, colour buttons ...) inside a
 * LinearLayout. Every control calls onChange after it modifies the template.
 */
public class PanelBuilder {

    public interface IntSetter {
        void set(int value);
    }

    public interface FloatSetter {
        void set(float value);
    }

    public interface BoolSetter {
        void set(boolean value);
    }

    public interface StringSetter {
        void set(String value);
    }

    private final Context ctx;
    private final LinearLayout root;
    private final Runnable onChange;
    private final float density;

    public PanelBuilder(Context ctx, LinearLayout root, Runnable onChange) {
        this.ctx = ctx;
        this.root = root;
        this.onChange = onChange;
        this.density = ctx.getResources().getDisplayMetrics().density;
    }

    private int dp(int v) {
        return (int) (v * density + 0.5f);
    }

    public void clear() {
        root.removeAllViews();
    }

    public void add(View v) {
        root.addView(v);
    }

    public void section(String title) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(14);
        tv.setTextColor(0xFFC62828);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, dp(14), 0, dp(4));
        root.addView(tv);
    }

    private TextView label(String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF1B1B1F);
        return tv;
    }

    public void text(String labelText, String value, boolean multiline, final StringSetter setter) {
        root.addView(label(labelText));
        EditText et = new EditText(ctx);
        et.setText(value == null ? "" : value);
        et.setTextSize(15);
        if (multiline) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            et.setMinLines(2);
            et.setGravity(Gravity.TOP);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT);
            et.setSingleLine(true);
        }
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override
            public void afterTextChanged(Editable e) {
                setter.set(e.toString());
                onChange.run();
            }
        });
        root.addView(et);
    }

    public void color(String labelText, int value, final IntSetter setter) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView tv = label(labelText);
        row.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final View swatch = new View(ctx);
        final int[] current = {value};
        paintSwatch(swatch, value);
        row.addView(swatch, new LinearLayout.LayoutParams(dp(56), dp(32)));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ColorPickerDialog.show(ctx, current[0], new ColorPickerDialog.OnColor() {
                    @Override
                    public void onColor(int c) {
                        current[0] = c;
                        paintSwatch(swatch, c);
                        setter.set(c);
                        onChange.run();
                    }
                });
            }
        });
        root.addView(row);
    }

    private void paintSwatch(View v, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(6));
        g.setStroke(dp(1), 0xFF9E9E9E);
        v.setBackgroundDrawable(g);
    }

    public void slider(String labelText, final float min, final float max, float value,
                       final FloatSetter setter) {
        final TextView tv = label("");
        root.addView(tv);
        final String name = labelText;
        final float range = max - min;
        SeekBar sb = new SeekBar(ctx);
        sb.setMax(1000);
        float clamped = Math.max(min, Math.min(max, value));
        sb.setProgress(Math.round((clamped - min) / range * 1000f));
        tv.setText(format(name, clamped));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                float v = min + range * progress / 1000f;
                tv.setText(format(name, v));
                if (fromUser) {
                    setter.set(v);
                    onChange.run();
                }
            }

            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        root.addView(sb);
    }

    private String format(String name, float v) {
        if (Math.abs(v) >= 10) {
            return name + ": " + Math.round(v);
        }
        return name + ": " + String.format(Locale.US, "%.2f", v);
    }

    public void choice(String labelText, String[] options, int current, final IntSetter setter) {
        root.addView(label(labelText));
        Spinner sp = new Spinner(ctx);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_spinner_item, options);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        final int[] last = {current};
        sp.setSelection(Math.max(0, current), false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos != last[0]) {
                    last[0] = pos;
                    setter.set(pos);
                    onChange.run();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        root.addView(sp);
    }

    public void toggle(String labelText, boolean checked, final BoolSetter setter) {
        CheckBox cb = new CheckBox(ctx);
        cb.setText(labelText);
        cb.setTextSize(13);
        cb.setChecked(checked);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                setter.set(isChecked);
                onChange.run();
            }
        });
        root.addView(cb);
    }

    public void button(String text, final Runnable action) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        root.addView(b, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}

package com.osman.photocardmaker.utils;

import android.graphics.Color;

import org.json.JSONObject;

import java.util.Locale;

public class JsonUtil {
    public static int color(JSONObject o, String k, int def) {
        if (o == null || !o.has(k)) return def;
        try {
            Object v = o.get(k);
            if (v instanceof String) return Color.parseColor((String) v);
            if (v instanceof Number) return ((Number) v).intValue();
        } catch (Exception ignored) { }
        return def;
    }

    public static String hex(int c) {
        return String.format(Locale.US, "#%08X", c);
    }

    public static float clamp(float v, float lo, float hi) {
        if (Float.isNaN(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }
}

package com.osman.photocardmaker.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A decorative shape on a card. Position values are fractions (0..1)
 * of the card width/height, so a template scales to any export size.
 */
public class Decoration {

    public static final int RECT = 0;
    public static final int CIRCLE = 1;
    public static final int SLANT = 2;

    public int type = RECT;
    public float l;
    public float t;
    public float r;
    public float b;
    /** ARGB color (alpha is part of the color). */
    public int color = 0xFFFFFFFF;
    /** True = drawn above photo, header, footer and logo. */
    public boolean onTop = false;

    public Decoration() {
    }

    public Decoration(int type, float l, float t, float r, float b, int color, boolean onTop) {
        this.type = type;
        this.l = l;
        this.t = t;
        this.r = r;
        this.b = b;
        this.color = color;
        this.onTop = onTop;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type);
        o.put("l", CardTemplate.d(l));
        o.put("t", CardTemplate.d(t));
        o.put("r", CardTemplate.d(r));
        o.put("b", CardTemplate.d(b));
        CardTemplate.putColor(o, "color", color);
        o.put("onTop", onTop);
        return o;
    }

    public static Decoration fromJson(JSONObject o) {
        Decoration d = new Decoration();
        d.type = o.optInt("type", RECT);
        d.l = (float) o.optDouble("l", 0);
        d.t = (float) o.optDouble("t", 0);
        d.r = (float) o.optDouble("r", 0);
        d.b = (float) o.optDouble("b", 0);
        d.color = CardTemplate.color(o, "color", 0xFFFFFFFF);
        d.onTop = o.optBoolean("onTop", false);
        return d;
    }
}

package com.osman.photocardmaker.models;

import com.osman.photocardmaker.utils.JsonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Element {
    public static final int TEXT = 0, IMAGE = 1, SHAPE = 2;
    public static final int S_RECT = 0, S_ROUND = 1, S_CIRCLE = 2, S_TRIANGLE = 3, S_STAR = 4, S_HEART = 5;
    public static final String[] FONTS = {"sans-serif", "sans-serif-condensed", "sans-serif-medium",
            "serif", "monospace", "cursive", "casual"};

    public int type = TEXT;
    public String id = newId();
    public float x, y, w = 400f, h = 400f, rotation = 0f, scale = 1f;
    public boolean flipH = false;
    public int alpha = 255;

    public String text = "";
    public float textSize = 64f;
    public int color = 0xFFFFFFFF;
    public boolean bold, italic, shadow, outline;
    public int align = 1;                 // 0 left, 1 center, 2 right
    public String font = "sans-serif";
    public int outlineColor = 0xFF000000;
    public int textBg = 0;

    public int shape = 0;                 // SHAPE: kind. IMAGE: mask (0 none, 1 rounded, 2 circle)
    public int color2 = 0;                // gradient end colour for shapes (0 = no gradient)
    public String imagePath = "";

    public static String newId() { return UUID.randomUUID().toString().substring(0, 8); }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type == IMAGE ? "image" : type == SHAPE ? "shape" : "text");
        o.put("id", id);
        o.put("x", (double) x);
        o.put("y", (double) y);
        o.put("w", (double) w);
        o.put("h", (double) h);
        o.put("rot", (double) rotation);
        o.put("scale", (double) scale);
        o.put("flip", flipH);
        o.put("alpha", alpha);
        o.put("text", text);
        o.put("size", (double) textSize);
        o.put("color", JsonUtil.hex(color));
        o.put("bold", bold);
        o.put("italic", italic);
        o.put("shadow", shadow);
        o.put("outline", outline);
        o.put("align", align);
        o.put("font", font);
        o.put("outlineColor", JsonUtil.hex(outlineColor));
        o.put("textBg", JsonUtil.hex(textBg));
        o.put("shape", shape);
        o.put("color2", JsonUtil.hex(color2));
        if (imagePath != null && !imagePath.isEmpty()) o.put("image", imagePath);
        return o;
    }

    public static Element fromJson(JSONObject o, boolean strict) throws JSONException {
        Element e = new Element();
        String t = o.optString("type", "text");
        e.type = t.equals("image") ? IMAGE : t.equals("shape") ? SHAPE : TEXT;
        String id = o.optString("id", "");
        e.id = id.isEmpty() || id.length() > 40 ? newId() : id;
        e.x = (float) o.optDouble("x", 0);
        e.y = (float) o.optDouble("y", 0);
        e.w = JsonUtil.clamp((float) o.optDouble("w", 400), 1f, 12000f);
        e.h = JsonUtil.clamp((float) o.optDouble("h", e.type == TEXT ? 0 : 400), 0f, 12000f);
        e.rotation = (float) o.optDouble("rot", 0);
        e.scale = JsonUtil.clamp((float) o.optDouble("scale", 1), 0.05f, 6f);
        e.flipH = o.optBoolean("flip", false);
        e.alpha = Math.max(0, Math.min(255, o.optInt("alpha", 255)));
        String s = o.optString("text", "");
        if (strict && s.length() > 500) s = s.substring(0, 500);
        e.text = s;
        e.textSize = JsonUtil.clamp((float) o.optDouble("size", 64), 4f, 1500f);
        e.color = JsonUtil.color(o, "color", 0xFFFFFFFF);
        e.bold = o.optBoolean("bold", false);
        e.italic = o.optBoolean("italic", false);
        e.shadow = o.optBoolean("shadow", false);
        e.outline = o.optBoolean("outline", false);
        e.align = Math.max(0, Math.min(2, o.optInt("align", 1)));
        String f = o.optString("font", "sans-serif");
        e.font = "sans-serif";
        for (String k : FONTS) if (k.equals(f)) e.font = k;
        e.outlineColor = JsonUtil.color(o, "outlineColor", 0xFF000000);
        e.textBg = JsonUtil.color(o, "textBg", 0);
        e.shape = Math.max(0, Math.min(5, o.optInt("shape", 0)));
        e.color2 = JsonUtil.color(o, "color2", 0);
        e.imagePath = strict ? "" : o.optString("image", "");
        return e;
    }
}

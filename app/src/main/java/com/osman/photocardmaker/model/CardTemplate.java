package com.osman.photocardmaker.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete description of a photo card design.
 * Every visual property is a plain field, so each template can be
 * customised independently. Positions are fractions (0..1) of the card
 * size; text sizes and border widths are in template pixels
 * (relative to width x height).
 */
public class CardTemplate {

    public static final int SHAPE_RECT = 0;
    public static final int SHAPE_CIRCLE = 1;

    public static final int FONT_SANS = 0;
    public static final int FONT_SERIF = 1;
    public static final int FONT_MONO = 2;

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    // ---- identity ----
    public String id = "";
    public String name = "";
    public String category = "Custom";

    // ---- canvas ----
    public int width = 1080;
    public int height = 1080;

    // ---- background ----
    public int bgColor1 = 0xFF101820;
    public int bgColor2 = 0xFF101820;
    /** 0 = left to right, 90 = top to bottom. */
    public int gradientAngle = 90;

    // ---- header ----
    public boolean showHeader = true;
    public float headerTopFrac = 0f;
    public float headerHeightFrac = 0.09f;
    public float headerWidthFrac = 1f;
    public int headerColor = 0xFFC62828;
    public String headerText = "";
    public int headerTextColor = 0xFFFFFFFF;
    public float headerTextSize = 44f;
    public int headerFamily = FONT_SANS;
    public boolean headerBold = true;
    public boolean headerItalic = false;
    public int headerAlign = ALIGN_LEFT;
    /** Optional custom font file (e.g. a Bangla .ttf) for the header. */
    public String headerFontPath = "";

    // ---- footer ----
    public float footerTopFrac = 0.72f;
    public int footerColor = 0xFF1A1A1A;
    public int footerAccentColor = 0xFFC62828;
    public float footerAccentHeightFrac = 0.012f;
    public float footerPadFrac = 0.05f;
    public float footerWidthFrac = 1f;

    // ---- headline and sub text (inside footer) ----
    public String headline = "";
    public int headlineColor = 0xFFFFFFFF;
    public float headlineSize = 64f;
    public int headlineFamily = FONT_SANS;
    public boolean headlineBold = true;
    public boolean headlineItalic = false;
    public int headlineAlign = ALIGN_LEFT;
    /** Optional custom font file for headline and sub text. */
    public String headlineFontPath = "";
    public String subText = "";
    public int subColor = 0xFFCCCCCC;
    public float subSize = 34f;

    // ---- photo ----
    public float photoL = 0f;
    public float photoT = 0.09f;
    public float photoR = 1f;
    public float photoB = 0.72f;
    public int photoShape = SHAPE_RECT;
    public float photoCorner = 0f;
    public float photoBorderW = 0f;
    public int photoBorderColor = 0xFFFFFFFF;
    public float photoZoom = 1f;
    public float photoOffX = 0f;
    public float photoOffY = 0f;

    // ---- logo ----
    public boolean showLogo = true;
    public float logoL = 0.84f;
    public float logoT = 0.01f;
    public float logoR = 0.99f;
    public float logoB = 0.085f;
    public int logoShape = SHAPE_RECT;
    public int logoBgColor = 0xFFFFFFFF;

    // ---- outer border ----
    public float borderWidth = 0f;
    public int borderColor = 0xFFFFFFFF;

    // ---- date badge (sits on the top edge of the footer) ----
    public boolean showDate = false;
    /** true = always show today's date (Bangla), false = use dateText. */
    public boolean dateAuto = true;
    public String dateText = "";
    public int dateColor = 0xFFFFFFFF;
    public int dateBgColor = 0xFFC62828;
    public float dateSize = 30f;
    public int dateAlign = ALIGN_LEFT;
    /** Vertical shift as a fraction of card height (0 = centred on the footer edge). */
    public float dateOffsetY = 0f;

    // ---- decorations ----
    public List<Decoration> decorations = new ArrayList<Decoration>();

    public CardTemplate() {
    }

    /** Independent deep copy (used before editing so the original stays intact). */
    public CardTemplate copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name);
        o.put("category", category);
        o.put("width", width);
        o.put("height", height);
        putColor(o, "bgColor1", bgColor1);
        putColor(o, "bgColor2", bgColor2);
        o.put("gradientAngle", gradientAngle);

        o.put("showHeader", showHeader);
        o.put("headerTopFrac", d(headerTopFrac));
        o.put("headerHeightFrac", d(headerHeightFrac));
        o.put("headerWidthFrac", d(headerWidthFrac));
        putColor(o, "headerColor", headerColor);
        o.put("headerText", headerText);
        putColor(o, "headerTextColor", headerTextColor);
        o.put("headerTextSize", d(headerTextSize));
        o.put("headerFamily", headerFamily);
        o.put("headerBold", headerBold);
        o.put("headerItalic", headerItalic);
        o.put("headerAlign", headerAlign);
        o.put("headerFontPath", headerFontPath);

        o.put("footerTopFrac", d(footerTopFrac));
        putColor(o, "footerColor", footerColor);
        putColor(o, "footerAccentColor", footerAccentColor);
        o.put("footerAccentHeightFrac", d(footerAccentHeightFrac));
        o.put("footerPadFrac", d(footerPadFrac));
        o.put("footerWidthFrac", d(footerWidthFrac));

        o.put("headline", headline);
        putColor(o, "headlineColor", headlineColor);
        o.put("headlineSize", d(headlineSize));
        o.put("headlineFamily", headlineFamily);
        o.put("headlineBold", headlineBold);
        o.put("headlineItalic", headlineItalic);
        o.put("headlineAlign", headlineAlign);
        o.put("headlineFontPath", headlineFontPath);
        o.put("subText", subText);
        putColor(o, "subColor", subColor);
        o.put("subSize", d(subSize));

        o.put("photoL", d(photoL));
        o.put("photoT", d(photoT));
        o.put("photoR", d(photoR));
        o.put("photoB", d(photoB));
        o.put("photoShape", photoShape);
        o.put("photoCorner", d(photoCorner));
        o.put("photoBorderW", d(photoBorderW));
        putColor(o, "photoBorderColor", photoBorderColor);
        o.put("photoZoom", d(photoZoom));
        o.put("photoOffX", d(photoOffX));
        o.put("photoOffY", d(photoOffY));

        o.put("showLogo", showLogo);
        o.put("logoL", d(logoL));
        o.put("logoT", d(logoT));
        o.put("logoR", d(logoR));
        o.put("logoB", d(logoB));
        o.put("logoShape", logoShape);
        putColor(o, "logoBgColor", logoBgColor);

        o.put("borderWidth", d(borderWidth));
        putColor(o, "borderColor", borderColor);

        o.put("showDate", showDate);
        o.put("dateAuto", dateAuto);
        o.put("dateText", dateText);
        putColor(o, "dateColor", dateColor);
        putColor(o, "dateBgColor", dateBgColor);
        o.put("dateSize", d(dateSize));
        o.put("dateAlign", dateAlign);
        o.put("dateOffsetY", d(dateOffsetY));

        JSONArray arr = new JSONArray();
        for (Decoration d : decorations) {
            arr.put(d.toJson());
        }
        o.put("decorations", arr);
        return o;
    }

    /** float to a clean double (0.09f becomes 0.09, not 0.0900000035). */
    public static double d(float v) {
        return Double.parseDouble(Float.toString(v));
    }

    /** Stores a colour as readable text: "#AARRGGBB". */
    public static void putColor(JSONObject o, String key, int c) throws JSONException {
        o.put(key, String.format("#%08X", c));
    }

    /** Reads a colour written as "#AARRGGBB", "#RRGGBB" or as a plain number. */
    public static int color(JSONObject o, String key, int def) {
        Object v = o.opt(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            try {
                long x = Long.parseLong(s, 16);
                if (s.length() <= 6) {
                    x |= 0xFF000000L;
                }
                return (int) x;
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private static float f(JSONObject o, String key, float def) {
        return (float) o.optDouble(key, def);
    }

    public static CardTemplate fromJson(JSONObject o) {
        CardTemplate t = new CardTemplate();
        t.id = o.optString("id", t.id);
        t.name = o.optString("name", t.name);
        t.category = o.optString("category", t.category);
        t.width = o.optInt("width", t.width);
        t.height = o.optInt("height", t.height);
        t.bgColor1 = color(o, "bgColor1", t.bgColor1);
        t.bgColor2 = color(o, "bgColor2", t.bgColor2);
        t.gradientAngle = o.optInt("gradientAngle", t.gradientAngle);

        t.showHeader = o.optBoolean("showHeader", t.showHeader);
        t.headerTopFrac = f(o, "headerTopFrac", t.headerTopFrac);
        t.headerHeightFrac = f(o, "headerHeightFrac", t.headerHeightFrac);
        t.headerWidthFrac = f(o, "headerWidthFrac", t.headerWidthFrac);
        t.headerColor = color(o, "headerColor", t.headerColor);
        t.headerText = o.optString("headerText", t.headerText);
        t.headerTextColor = color(o, "headerTextColor", t.headerTextColor);
        t.headerTextSize = f(o, "headerTextSize", t.headerTextSize);
        t.headerFamily = o.optInt("headerFamily", t.headerFamily);
        t.headerBold = o.optBoolean("headerBold", t.headerBold);
        t.headerItalic = o.optBoolean("headerItalic", t.headerItalic);
        t.headerAlign = o.optInt("headerAlign", t.headerAlign);
        t.headerFontPath = o.optString("headerFontPath", t.headerFontPath);

        t.footerTopFrac = f(o, "footerTopFrac", t.footerTopFrac);
        t.footerColor = color(o, "footerColor", t.footerColor);
        t.footerAccentColor = color(o, "footerAccentColor", t.footerAccentColor);
        t.footerAccentHeightFrac = f(o, "footerAccentHeightFrac", t.footerAccentHeightFrac);
        t.footerPadFrac = f(o, "footerPadFrac", t.footerPadFrac);
        t.footerWidthFrac = f(o, "footerWidthFrac", t.footerWidthFrac);

        t.headline = o.optString("headline", t.headline);
        t.headlineColor = color(o, "headlineColor", t.headlineColor);
        t.headlineSize = f(o, "headlineSize", t.headlineSize);
        t.headlineFamily = o.optInt("headlineFamily", t.headlineFamily);
        t.headlineBold = o.optBoolean("headlineBold", t.headlineBold);
        t.headlineItalic = o.optBoolean("headlineItalic", t.headlineItalic);
        t.headlineAlign = o.optInt("headlineAlign", t.headlineAlign);
        t.headlineFontPath = o.optString("headlineFontPath", t.headlineFontPath);
        t.subText = o.optString("subText", t.subText);
        t.subColor = color(o, "subColor", t.subColor);
        t.subSize = f(o, "subSize", t.subSize);

        t.photoL = f(o, "photoL", t.photoL);
        t.photoT = f(o, "photoT", t.photoT);
        t.photoR = f(o, "photoR", t.photoR);
        t.photoB = f(o, "photoB", t.photoB);
        t.photoShape = o.optInt("photoShape", t.photoShape);
        t.photoCorner = f(o, "photoCorner", t.photoCorner);
        t.photoBorderW = f(o, "photoBorderW", t.photoBorderW);
        t.photoBorderColor = color(o, "photoBorderColor", t.photoBorderColor);
        t.photoZoom = f(o, "photoZoom", t.photoZoom);
        t.photoOffX = f(o, "photoOffX", t.photoOffX);
        t.photoOffY = f(o, "photoOffY", t.photoOffY);

        t.showLogo = o.optBoolean("showLogo", t.showLogo);
        t.logoL = f(o, "logoL", t.logoL);
        t.logoT = f(o, "logoT", t.logoT);
        t.logoR = f(o, "logoR", t.logoR);
        t.logoB = f(o, "logoB", t.logoB);
        t.logoShape = o.optInt("logoShape", t.logoShape);
        t.logoBgColor = color(o, "logoBgColor", t.logoBgColor);

        t.borderWidth = f(o, "borderWidth", t.borderWidth);
        t.borderColor = color(o, "borderColor", t.borderColor);

        t.showDate = o.optBoolean("showDate", t.showDate);
        t.dateAuto = o.optBoolean("dateAuto", t.dateAuto);
        t.dateText = o.optString("dateText", t.dateText);
        t.dateColor = color(o, "dateColor", t.dateColor);
        t.dateBgColor = color(o, "dateBgColor", t.dateBgColor);
        t.dateSize = f(o, "dateSize", t.dateSize);
        t.dateAlign = o.optInt("dateAlign", t.dateAlign);
        t.dateOffsetY = f(o, "dateOffsetY", t.dateOffsetY);

        JSONArray arr = o.optJSONArray("decorations");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject d = arr.optJSONObject(i);
                if (d != null) {
                    t.decorations.add(Decoration.fromJson(d));
                }
            }
        }
        return t;
    }
}

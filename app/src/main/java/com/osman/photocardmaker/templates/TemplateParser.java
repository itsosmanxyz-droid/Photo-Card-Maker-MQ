package com.osman.photocardmaker.templates;

import android.graphics.Color;

import com.osman.photocardmaker.models.Category;
import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.utils.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TemplateParser {
    public static final int SUPPORTED_VERSION = 1;
    private static final Pattern ID = Pattern.compile("^[a-zA-Z0-9_\\-]{1,48}$");

    public static class Catalog {
        public List<Category> categories = new ArrayList<>();
        public List<Template> templates = new ArrayList<>();
    }

    /** strict = untrusted remote data: validated, no local image paths, size/count limits. */
    public static Catalog parse(String json, boolean strict) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.optInt("version", 1) > SUPPORTED_VERSION) throw new JSONException("unsupported version");
        Catalog c = new Catalog();

        JSONArray cats = root.optJSONArray("categories");
        if (cats != null) {
            if (strict && cats.length() > 100) throw new JSONException("too many categories");
            for (int i = 0; i < cats.length(); i++) {
                try {
                    JSONObject o = cats.getJSONObject(i);
                    Category k = new Category();
                    k.id = o.getString("id");
                    if (!ID.matcher(k.id).matches()) continue;
                    k.name = cut(o.optString("name", k.id), 80);
                    k.nameBn = cut(o.optString("nameBn", ""), 80);
                    k.icon = cut(o.optString("icon", "🎨"), 8);
                    k.color = JsonUtil.color(o, "color", 0xFF6C4DF6) | 0xFF000000;
                    k.enabled = o.optBoolean("enabled", true);
                    c.categories.add(k);
                } catch (Exception ignored) { }
            }
        }

        JSONArray tpls = root.optJSONArray("templates");
        if (tpls != null) {
            if (strict && tpls.length() > 1000) throw new JSONException("too many templates");
            for (int i = 0; i < tpls.length(); i++) {
                try {
                    JSONObject o = tpls.getJSONObject(i);
                    Template t = new Template();
                    t.id = o.getString("id");
                    t.categoryId = o.getString("categoryId");
                    if (!ID.matcher(t.id).matches() || !ID.matcher(t.categoryId).matches()) continue;
                    t.name = cut(o.optString("name", t.id), 80);
                    t.nameBn = cut(o.optString("nameBn", ""), 80);
                    t.width = o.optInt("width", 1080);
                    t.height = o.optInt("height", 1080);
                    if (t.width < 64 || t.height < 64 || t.width > 6000 || t.height > 6000) continue;
                    t.enabled = o.optBoolean("enabled", true);
                    t.featured = o.optBoolean("featured", false);
                    t.trending = o.optBoolean("trending", false);
                    t.premium = o.optBoolean("premium", false);
                    t.recommended = o.optBoolean("recommended", false);
                    t.custom = o.optBoolean("custom", false);
                    String th = o.optString("thumbnail", "");
                    t.thumbnailUrl = th.startsWith("https://") && th.length() < 500 ? th : "";
                    JSONObject d = o.optJSONObject("design");
                    if (d != null) t.design = Design.fromJson(d, t.width, t.height, strict);
                    c.templates.add(t);
                } catch (Exception ignored) { }
            }
        }
        return c;
    }

    private static String cut(String s, int max) { return s.length() > max ? s.substring(0, max) : s; }
}

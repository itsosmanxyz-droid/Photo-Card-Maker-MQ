package com.osman.photocardmaker.models;

import com.osman.photocardmaker.utils.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Design {
    public int width = 1080, height = 1080;
    public int bg1 = 0xFFFFFFFF, bg2 = 0xFFFFFFFF;
    public int frame = 0;                       // 0 none, 1 solid, 2 double, 3 rounded, 4 dashed
    public int frameColor = 0xFFFFFFFF;
    public float frameWidth = 24f;
    public List<Element> elements = new ArrayList<>();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("width", width);
        o.put("height", height);
        o.put("bg", JsonUtil.hex(bg1));
        o.put("bg2", JsonUtil.hex(bg2));
        o.put("frame", frame);
        o.put("frameColor", JsonUtil.hex(frameColor));
        o.put("frameWidth", (double) frameWidth);
        JSONArray arr = new JSONArray();
        for (Element e : elements) arr.put(e.toJson());
        o.put("elements", arr);
        return o;
    }

    /** w/h &gt; 0 override the size stored in the JSON. strict = validate untrusted (remote) data. */
    public static Design fromJson(JSONObject o, int w, int h, boolean strict) throws JSONException {
        Design d = new Design();
        d.width = w > 0 ? w : o.optInt("width", 1080);
        d.height = h > 0 ? h : o.optInt("height", 1080);
        if (strict && (d.width < 64 || d.height < 64 || d.width > 6000 || d.height > 6000))
            throw new JSONException("bad size");
        d.bg1 = JsonUtil.color(o, "bg", 0xFFFFFFFF);
        d.bg2 = JsonUtil.color(o, "bg2", d.bg1);
        d.frame = Math.max(0, Math.min(4, o.optInt("frame", 0)));
        d.frameColor = JsonUtil.color(o, "frameColor", 0xFFFFFFFF);
        d.frameWidth = JsonUtil.clamp((float) o.optDouble("frameWidth", Math.min(d.width, d.height) * 0.025), 1f, 400f);
        JSONArray arr = o.optJSONArray("elements");
        if (arr != null) {
            if (strict && arr.length() > 80) throw new JSONException("too many elements");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject eo = arr.optJSONObject(i);
                if (eo != null) d.elements.add(Element.fromJson(eo, strict));
            }
        }
        return d;
    }

    public Design copy() {
        try {
            return fromJson(toJson(), 0, 0, false);
        } catch (JSONException e) {
            return new Design();
        }
    }
}

package com.osman.photocardmaker.storage;

import android.content.Context;
import android.graphics.Bitmap;

import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.models.Element;
import com.osman.photocardmaker.models.Project;
import com.osman.photocardmaker.utils.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectStore {
    public static File root(Context c) {
        File f = new File(c.getFilesDir(), "projects");
        f.mkdirs();
        return f;
    }

    public static File dir(Context c, String id) { return new File(root(c), id); }
    public static File imagesDir(Context c, String id) { return new File(dir(c, id), "images"); }
    public static File previewFile(Context c, String id) { return new File(dir(c, id), "preview.png"); }
    private static File jsonFile(Context c, String id) { return new File(dir(c, id), "project.json"); }
    public static boolean exists(Context c, String id) { return jsonFile(c, id).exists(); }

    public static List<Project> list(Context c) {
        List<Project> out = new ArrayList<>();
        File[] dirs = root(c).listFiles();
        if (dirs != null) {
            for (File d : dirs) {
                try {
                    if (new File(d, "project.json").exists()) out.add(loadProject(c, d.getName()));
                } catch (Exception ignored) { }
            }
        }
        Collections.sort(out, (a, b) -> Long.compare(b.updated, a.updated));
        return out;
    }

    public static Project loadProject(Context c, String id) throws Exception {
        JSONObject o = new JSONObject(Util.readFile(jsonFile(c, id)));
        Project p = new Project();
        p.id = id;
        p.name = o.optString("name", id);
        p.favorite = o.optBoolean("favorite", false);
        p.updated = o.optLong("updated", 0);
        JSONObject d = o.optJSONObject("design");
        if (d != null) { p.width = d.optInt("width"); p.height = d.optInt("height"); }
        return p;
    }

    public static Design loadDesign(Context c, String id) throws Exception {
        JSONObject o = new JSONObject(Util.readFile(jsonFile(c, id)));
        Design d = Design.fromJson(o.getJSONObject("design"), 0, 0, false);
        for (Element e : d.elements) {
            if (e.imagePath != null && !e.imagePath.isEmpty()) {
                e.imagePath = new File(imagesDir(c, id), new File(e.imagePath).getName()).getAbsolutePath();
            }
        }
        return d;
    }

    public static void save(Context c, String id, String name, boolean favorite, Design d, Bitmap preview) throws Exception {
        File dir = dir(c, id);
        dir.mkdirs();
        JSONObject dj = d.toJson();
        JSONArray arr = dj.getJSONArray("elements");
        Set<String> used = new HashSet<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject eo = arr.getJSONObject(i);
            String ip = eo.optString("image", "");
            if (!ip.isEmpty()) {
                String fn = new File(ip).getName();
                eo.put("image", fn);
                used.add(fn);
            }
        }
        JSONObject root = new JSONObject();
        root.put("name", name);
        root.put("favorite", favorite);
        root.put("updated", System.currentTimeMillis());
        root.put("design", dj);
        Util.writeFile(jsonFile(c, id), root.toString());
        if (preview != null) {
            try (FileOutputStream os = new FileOutputStream(previewFile(c, id))) {
                preview.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
        }
        File[] imgs = imagesDir(c, id).listFiles();
        if (imgs != null) for (File f : imgs) if (!used.contains(f.getName())) f.delete();
    }

    private static void patch(Context c, String id, String key, Object val) throws Exception {
        JSONObject o = new JSONObject(Util.readFile(jsonFile(c, id)));
        o.put(key, val);
        Util.writeFile(jsonFile(c, id), o.toString());
    }

    public static void rename(Context c, String id, String name) throws Exception { patch(c, id, "name", name); }
    public static void setFavorite(Context c, String id, boolean fav) throws Exception { patch(c, id, "favorite", fav); }
    public static void delete(Context c, String id) { Util.deleteRecursive(dir(c, id)); }
}

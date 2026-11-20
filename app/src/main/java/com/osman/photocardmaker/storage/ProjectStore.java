package com.osman.photocardmaker.storage;

import android.content.Context;
import android.graphics.Bitmap;

import com.osman.photocardmaker.model.CardTemplate;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Saves and loads editable projects (template + photo + logo) in local storage. */
public final class ProjectStore {

    /** A saved project. */
    public static class Info {
        public String id;
        public String title;
        public long updated;
        public CardTemplate template;
        public String photoPath;
        public String logoPath;
    }

    private ProjectStore() {
    }

    private static File dir(Context ctx) {
        return new LocalStorage(ctx).getProjectsDir();
    }

    public static File thumbFile(Context ctx, String id) {
        return new File(dir(ctx), id + ".png");
    }

    public static boolean save(Context ctx, String id, String title, CardTemplate t,
                               String photoPath, String logoPath, Bitmap thumb) {
        try {
            JSONObject o = new JSONObject();
            o.put("title", title);
            o.put("updated", System.currentTimeMillis());
            if (photoPath != null) {
                o.put("photoPath", photoPath);
            }
            if (logoPath != null) {
                o.put("logoPath", logoPath);
            }
            o.put("template", t.toJson());
            FileUtil.writeText(new File(dir(ctx), id + ".json"), o.toString());

            if (thumb != null) {
                FileOutputStream out = new FileOutputStream(thumbFile(ctx, id));
                try {
                    thumb.compress(Bitmap.CompressFormat.PNG, 90, out);
                } finally {
                    out.close();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Info load(Context ctx, String id) {
        return read(new File(dir(ctx), id + ".json"), id);
    }

    private static Info read(File f, String id) {
        try {
            JSONObject o = new JSONObject(FileUtil.readText(f));
            Info info = new Info();
            info.id = id;
            info.title = o.optString("title", id);
            info.updated = o.optLong("updated", f.lastModified());
            info.photoPath = o.optString("photoPath", null);
            info.logoPath = o.optString("logoPath", null);
            info.template = CardTemplate.fromJson(o.getJSONObject("template"));
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    public static List<Info> list(Context ctx) {
        List<Info> out = new ArrayList<Info>();
        File[] files = dir(ctx).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(".json");
            }
        });
        if (files == null) {
            return out;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                long x = a.lastModified();
                long y = b.lastModified();
                return x < y ? 1 : (x > y ? -1 : 0);
            }
        });
        for (File f : files) {
            String name = f.getName();
            String id = name.substring(0, name.length() - 5);
            Info info = read(f, id);
            if (info != null) {
                out.add(info);
            }
        }
        return out;
    }

    public static void delete(Context ctx, String id) {
        new File(dir(ctx), id + ".json").delete();
        thumbFile(ctx, id).delete();
    }
}

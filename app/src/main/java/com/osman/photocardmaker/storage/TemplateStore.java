package com.osman.photocardmaker.storage;

import android.content.Context;

import com.osman.photocardmaker.model.CardTemplate;

import org.json.JSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Stores user-created templates as JSON files. */
public final class TemplateStore {

    private TemplateStore() {
    }

    private static File dir(Context ctx) {
        return new LocalStorage(ctx).getTemplatesDir();
    }

    public static boolean save(Context ctx, CardTemplate t) {
        try {
            FileUtil.writeText(new File(dir(ctx), t.id + ".json"), t.toJson().toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<CardTemplate> list(Context ctx) {
        List<CardTemplate> out = new ArrayList<CardTemplate>();
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
            try {
                CardTemplate t = CardTemplate.fromJson(new JSONObject(FileUtil.readText(f)));
                if (t.id == null || t.id.length() == 0) {
                    String n = f.getName();
                    t.id = n.substring(0, n.length() - 5);
                }
                out.add(t);
            } catch (Exception e) {
                // skip broken file
            }
        }
        return out;
    }

    public static void delete(Context ctx, String id) {
        new File(dir(ctx), id + ".json").delete();
    }
}

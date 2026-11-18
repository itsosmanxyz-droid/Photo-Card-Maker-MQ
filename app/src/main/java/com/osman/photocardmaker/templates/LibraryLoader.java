package com.osman.photocardmaker.templates;

import android.content.Context;
import android.content.res.AssetManager;

import com.osman.photocardmaker.model.CardTemplate;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads the template library from the project's assets folder:
 *
 *   assets/templates/&lt;category folder&gt;/category.json   (optional title, description, color)
 *   assets/templates/&lt;category folder&gt;/&lt;Any_Name&gt;.json  (one file = one template)
 *
 * Adding a template or a whole category = adding files. No Java file needs to change.
 * A category folder with no template files is shown as "Coming Soon".
 */
public final class LibraryLoader {

    private static final String ROOT = "templates";
    private static final String CATEGORY_FILE = "category.json";

    private static final int[] DEFAULT_COLORS = {
            0xFFC62828, 0xFF1565C0, 0xFF00695C, 0xFFAD1457, 0xFFEF6C00,
            0xFF283593, 0xFF6A1B9A, 0xFF2E7D32, 0xFF4E342E};

    /** One folder of the library. */
    public static class Category {
        public String folder;
        public String title;
        public String description = "";
        public String letter;
        public int color;
        public final List<CardTemplate> templates = new ArrayList<CardTemplate>();

        public boolean isEmpty() {
            return templates.isEmpty();
        }
    }

    private LibraryLoader() {
    }

    public static List<Category> load(Context ctx) {
        List<Category> out = new ArrayList<Category>();
        AssetManager am = ctx.getAssets();
        String[] names;
        try {
            names = am.list(ROOT);
        } catch (IOException e) {
            return out;
        }
        if (names == null) {
            return out;
        }
        Arrays.sort(names);
        for (String name : names) {
            if (name.indexOf('.') >= 0) {
                continue; // a file in the root, not a category folder
            }
            Category c = loadCategory(am, name, out.size());
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    public static Category find(Context ctx, String folder) {
        for (Category c : load(ctx)) {
            if (c.folder.equals(folder)) {
                return c;
            }
        }
        return null;
    }

    private static Category loadCategory(AssetManager am, String folder, int index) {
        String[] files;
        try {
            files = am.list(ROOT + "/" + folder);
        } catch (IOException e) {
            return null;
        }
        if (files == null) {
            return null;
        }
        Arrays.sort(files);

        Category c = new Category();
        c.folder = folder;
        c.title = titleFromFolder(folder);
        c.letter = letterFromFolder(folder);
        c.color = DEFAULT_COLORS[index % DEFAULT_COLORS.length];

        for (String f : files) {
            if (!f.endsWith(".json")) {
                continue;
            }
            try {
                JSONObject o = new JSONObject(read(am, ROOT + "/" + folder + "/" + f));
                if (CATEGORY_FILE.equals(f)) {
                    c.title = o.optString("title", c.title);
                    c.description = o.optString("description", c.description);
                    c.letter = o.optString("letter", c.letter);
                    c.color = CardTemplate.color(o, "color", c.color);
                } else {
                    CardTemplate t = CardTemplate.fromJson(o);
                    if (t.id == null || t.id.length() == 0) {
                        t.id = folder + "/" + f;
                    }
                    if (t.name == null || t.name.length() == 0) {
                        t.name = f.substring(0, f.length() - 5).replace('_', ' ');
                    }
                    c.templates.add(t);
                }
            } catch (Exception e) {
                // skip a broken file, keep the rest of the library working
            }
        }
        return c;
    }

    private static String titleFromFolder(String folder) {
        int i = 0;
        while (i < folder.length() && Character.isDigit(folder.charAt(i))) {
            i++;
        }
        String s = folder.substring(i);
        while (s.startsWith("_") || s.startsWith(".") || s.startsWith(" ")) {
            s = s.substring(1);
        }
        s = s.replace('_', ' ');
        return s.length() == 0 ? folder : s;
    }

    private static String letterFromFolder(String folder) {
        int i = 0;
        while (i < folder.length() && Character.isDigit(folder.charAt(i))) {
            i++;
        }
        if (i > 0) {
            return folder.substring(0, Math.min(i, 2));
        }
        return folder.length() > 0 ? folder.substring(0, 1).toUpperCase() : "?";
    }

    private static String read(AssetManager am, String path) throws IOException {
        InputStream in = am.open(path);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}

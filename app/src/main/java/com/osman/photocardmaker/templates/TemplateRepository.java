package com.osman.photocardmaker.templates;

import android.content.Context;

import com.osman.photocardmaker.BuildConfig;
import com.osman.photocardmaker.models.Category;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.network.RemoteConfig;
import com.osman.photocardmaker.utils.Bg;
import com.osman.photocardmaker.utils.Util;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateRepository {
    private static final TemplateRepository I = new TemplateRepository();
    public static TemplateRepository get() { return I; }

    private TemplateParser.Catalog local;
    private volatile List<Category> cats = new ArrayList<>();
    private volatile List<Template> tpls = new ArrayList<>();

    private static File cacheFile(Context c) { return new File(c.getFilesDir(), "remote_templates.json"); }

    public synchronized void init(Context ctx) {
        if (local != null) return;
        Context c = ctx.getApplicationContext();
        try (InputStream in = c.getAssets().open("templates.json")) {
            local = TemplateParser.parse(Util.readStream(in), false);
        } catch (Exception e) {
            local = new TemplateParser.Catalog();
        }
        TemplateParser.Catalog remote = null;
        File f = cacheFile(c);
        if (f.exists()) {
            try { remote = TemplateParser.parse(Util.readFile(f), true); } catch (Exception ignored) { f.delete(); }
        }
        rebuild(remote);
    }

    /** Downloads templates.json from GitHub (or any https URL); on any failure local data stays. */
    public void refreshRemote(Context ctx, Runnable onUpdated) {
        final Context c = ctx.getApplicationContext();
        Bg.run(() -> {
            try {
                String url = BuildConfig.REMOTE_TEMPLATES_URL;
                if (url == null || url.contains("YOUR_GITHUB_USERNAME")) return;
                String json = RemoteConfig.fetchText(url);
                TemplateParser.Catalog r = TemplateParser.parse(json, true);   // validates before accepting
                Util.writeFile(cacheFile(c), json);
                synchronized (TemplateRepository.this) { rebuild(r); }
                if (onUpdated != null) Bg.main(onUpdated);
            } catch (Exception ignored) { }
        });
    }

    private void rebuild(TemplateParser.Catalog remote) {
        Map<String, Category> cm = new LinkedHashMap<>();
        Map<String, Template> tm = new LinkedHashMap<>();
        for (Category k : local.categories) cm.put(k.id, k);
        for (Template t : local.templates) tm.put(t.id, t);
        if (remote != null) {
            for (Category k : remote.categories) cm.put(k.id, k);
            for (Template t : remote.templates) {
                Template old = tm.get(t.id);
                if (t.design == null) {
                    if (old == null || old.design == null) continue;
                    t.design = old.design;
                }
                tm.put(t.id, t);
            }
        }
        List<Category> cl = new ArrayList<>();
        for (Category k : cm.values()) if (k.enabled) cl.add(k);
        List<Template> tl = new ArrayList<>();
        for (Template t : tm.values()) {
            Category k = cm.get(t.categoryId);
            if (t.enabled && t.design != null && k != null && k.enabled) tl.add(t);
        }
        cats = cl;
        tpls = tl;
    }

    public List<Category> categories() { return cats; }
    public List<Template> all() { return tpls; }

    public Category category(String id) {
        for (Category k : cats) if (k.id.equals(id)) return k;
        return null;
    }

    public Template find(String id) {
        for (Template t : tpls) if (t.id.equals(id)) return t;
        return null;
    }

    public List<Template> templatesOf(String catId) {
        List<Template> out = new ArrayList<>();
        for (Template t : tpls) if (t.categoryId.equals(catId)) out.add(t);
        return out;
    }

    public interface Filter { boolean ok(Template t); }

    public List<Template> filter(Filter f) {
        List<Template> out = new ArrayList<>();
        for (Template t : tpls) if (f.ok(t)) out.add(t);
        return out;
    }
}

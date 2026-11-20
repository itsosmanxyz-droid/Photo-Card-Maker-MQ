package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.core.AppModule;
import com.osman.photocardmaker.core.ModuleRegistry;
import com.osman.photocardmaker.model.CardTemplate;
import com.osman.photocardmaker.render.TemplateRenderer;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.storage.TemplateStore;
import com.osman.photocardmaker.templates.BlankTemplate;
import com.osman.photocardmaker.templates.LibraryLoader;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * One list screen for three sources:
 * - a library category (EXTRA_CATEGORY = folder name inside assets/templates),
 * - the user's own templates (EXTRA_MODULE_ID = "templates"),
 * - saved projects (EXTRA_MODULE_ID = "projects").
 */
public class TemplateGalleryActivity extends Activity {

    public static final String EXTRA_MODULE_ID = "module_id";
    public static final String EXTRA_CATEGORY = "category_folder";

    private static final int THUMB_WIDTH = 360;

    private static class Item {
        String title;
        String subtitle;
        Bitmap thumb;
        CardTemplate template;
        String projectId;
        boolean comingSoon;
    }

    private String moduleId;
    private String categoryFolder;
    private final List<Item> items = new ArrayList<Item>();
    private TemplateAdapter adapter;
    private TextView hint;
    private View emptyView;
    private GridView grid;
    private AdView bannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        categoryFolder = getIntent().getStringExtra(EXTRA_CATEGORY);
        moduleId = getIntent().getStringExtra(EXTRA_MODULE_ID);
        if (moduleId == null) {
            moduleId = "";
        }

        findViewById(R.id.btnGalleryBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        AppModule module = ModuleRegistry.findById(moduleId);
        if (module != null) {
            ((TextView) findViewById(R.id.tvGalleryTitle)).setText(module.getTitleRes());
        }
        hint = (TextView) findViewById(R.id.tvGalleryHint);
        emptyView = findViewById(R.id.emptyView);
        grid = (GridView) findViewById(R.id.gridTemplates);

        if ("templates".equals(moduleId)) {
            TextView action = (TextView) findViewById(R.id.btnGalleryAction);
            action.setText(R.string.btn_new);
            action.setVisibility(View.VISIBLE);
            action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openEditor(BlankTemplate.create(TemplateGalleryActivity.this));
                }
            });
        }

        bannerView = AdManager.attachBanner(this, (ViewGroup) findViewById(R.id.adBanner));

        adapter = new TemplateAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item it = items.get(position);
                if (it.comingSoon) {
                    Toast.makeText(TemplateGalleryActivity.this, R.string.coming_soon,
                            Toast.LENGTH_SHORT).show();
                } else if (it.projectId != null) {
                    Intent i = new Intent(TemplateGalleryActivity.this, EditorActivity.class);
                    i.putExtra(EditorActivity.EXTRA_PROJECT, it.projectId);
                    startActivity(i);
                } else {
                    openEditor(it.template);
                }
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(items.get(position));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerView != null) {
            bannerView.resume();
        }
        load();
    }

    @Override
    protected void onPause() {
        if (bannerView != null) {
            bannerView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bannerView != null) {
            bannerView.destroy();
        }
        super.onDestroy();
    }

    private void openEditor(CardTemplate t) {
        try {
            Intent i = new Intent(this, EditorActivity.class);
            i.putExtra(EditorActivity.EXTRA_TEMPLATE, t.toJson().toString());
            startActivity(i);
        } catch (JSONException e) {
            // cannot happen for valid templates
        }
    }

    private void confirmDelete(final Item it) {
        boolean deletable = !it.comingSoon && (it.projectId != null || "templates".equals(moduleId));
        if (!deletable) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (it.projectId != null) {
                            ProjectStore.delete(TemplateGalleryActivity.this, it.projectId);
                        } else {
                            TemplateStore.delete(TemplateGalleryActivity.this, it.template.id);
                        }
                        load();
                    }
                })
                .show();
    }

    private void load() {
        items.clear();
        boolean showEmpty = false;

        if ("projects".equals(moduleId)) {
            hint.setText(R.string.list_hint_projects);
            for (ProjectStore.Info p : ProjectStore.list(this)) {
                Item it = new Item();
                it.title = p.title;
                it.subtitle = p.template.name;
                it.projectId = p.id;
                it.template = p.template;
                it.thumb = BitmapFactory.decodeFile(ProjectStore.thumbFile(this, p.id).getPath());
                if (it.thumb == null) {
                    it.thumb = TemplateRenderer.render(p.template, THUMB_WIDTH, null, null);
                }
                items.add(it);
            }
            if (items.isEmpty()) {
                hint.setText(R.string.list_hint_projects_empty);
            }
        } else if ("templates".equals(moduleId)) {
            hint.setText(R.string.list_hint_templates);
            for (CardTemplate t : TemplateStore.list(this)) {
                items.add(templateItem(t));
            }
            if (items.isEmpty()) {
                hint.setText(R.string.list_hint_templates_empty);
            }
        } else if (categoryFolder != null) {
            LibraryLoader.Category cat = LibraryLoader.find(this, categoryFolder);
            if (cat != null) {
                ((TextView) findViewById(R.id.tvGalleryTitle)).setText(cat.title);
                for (CardTemplate t : cat.templates) {
                    items.add(templateItem(t));
                }
            }
            if (items.isEmpty()) {
                showEmpty = true;
                hint.setText("");
            } else {
                hint.setText(R.string.list_hint_default);
                items.add(comingSoonItem());
            }
        }

        emptyView.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        grid.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    private Item templateItem(CardTemplate t) {
        Item it = new Item();
        it.title = t.name;
        it.subtitle = t.category;
        it.template = t;
        it.thumb = TemplateRenderer.render(t, THUMB_WIDTH, null, null);
        return it;
    }

    /** Grey tile shown at the end of every category, like a "more coming" slot. */
    private Item comingSoonItem() {
        Bitmap b = Bitmap.createBitmap(THUMB_WIDTH, THUMB_WIDTH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(0xFFE9EBF1);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF8A8F9C);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.DEFAULT_BOLD);
        p.setTextSize(THUMB_WIDTH * 0.11f);
        c.drawText(getString(R.string.coming_soon), THUMB_WIDTH / 2f, THUMB_WIDTH / 2f, p);

        Item it = new Item();
        it.title = getString(R.string.coming_soon);
        it.subtitle = getString(R.string.coming_soon_tile);
        it.thumb = b;
        it.comingSoon = true;
        return it;
    }

    private class TemplateAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(TemplateGalleryActivity.this);

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = inflater.inflate(R.layout.item_template, parent, false);
            }
            Item it = items.get(position);
            ((ImageView) v.findViewById(R.id.ivPreview)).setImageBitmap(it.thumb);
            ((TextView) v.findViewById(R.id.tvName)).setText(it.title);
            ((TextView) v.findViewById(R.id.tvCategory)).setText(it.subtitle);
            return v;
        }
    }
}

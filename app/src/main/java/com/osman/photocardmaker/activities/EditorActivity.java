package com.osman.photocardmaker.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.editor.BitmapStore;
import com.osman.photocardmaker.editor.EditorView;
import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.models.Element;
import com.osman.photocardmaker.models.Project;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.storage.ExportUtil;
import com.osman.photocardmaker.storage.ImageImporter;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.templates.TemplateRepository;
import com.osman.photocardmaker.utils.Bg;
import com.osman.photocardmaker.utils.ColorPicker;
import com.osman.photocardmaker.utils.ShareUtil;
import com.osman.photocardmaker.utils.Util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

public class EditorActivity extends AppCompatActivity implements EditorView.Listener {
    public static final String EXTRA_TEMPLATE_ID = "template_id";
    public static final String EXTRA_PROJECT_ID = "project_id";
    public static final String EXTRA_W = "w";
    public static final String EXTRA_H = "h";

    private static final String[] EMOJIS = {"😀", "😍", "🥰", "😎", "🤩", "🥳", "🎉", "🎂", "🎁", "🎈",
            "❤️", "💖", "💕", "🌹", "🌸", "🌙", "⭐", "🌟", "✨", "🔥",
            "👑", "💎", "🕌", "☪️", "🤲", "📿", "🕋", "🏆", "☀️", "🌈",
            "🦋", "🍀", "🌺", "🎵", "📸", "🏷️", "💌", "🙏", "👍", "💯"};
    private static final int[][] GRADIENTS = {
            {0xFFFF9966, 0xFFFF5E62}, {0xFF2193B0, 0xFF6DD5ED}, {0xFF11998E, 0xFF38EF7D},
            {0xFF667EEA, 0xFF764BA2}, {0xFF0F0C29, 0xFF302B63}, {0xFFFF758C, 0xFFFF7EB3},
            {0xFFF7971E, 0xFFFFD200}, {0xFF004D40, 0xFF00796B}};
    private static final String[] GRADIENT_NAMES = {"Sunset", "Ocean", "Forest", "Royal", "Night", "Rose", "Gold", "Emerald"};
    private static final float[] RATIOS_W = {0, 1, 4, 3, 16, 9};
    private static final float[] RATIOS_H = {0, 1, 3, 4, 9, 16};

    private EditorView ev;
    private LinearLayout selPanel, selBar, toolsBar;
    private SeekBar sizeSeek, rotSeek;
    private TextView undoBtn, redoBtn;
    private boolean updatingUi, dirty, loaded;
    private String projectId, projectName;
    private boolean favorite;
    private Element pendingImageTarget;
    private Runnable pendingAfterPermission;
    private ActivityResultLauncher<String> imagePicker, permLauncher;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_editor);
        ev = findViewById(R.id.editor);
        selPanel = findViewById(R.id.selPanel);
        selBar = findViewById(R.id.selBar);
        toolsBar = findViewById(R.id.toolsBar);
        sizeSeek = findViewById(R.id.sizeSeek);
        rotSeek = findViewById(R.id.rotSeek);
        undoBtn = findViewById(R.id.btnUndo);
        redoBtn = findViewById(R.id.btnRedo);
        ev.setListener(this);

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);
        permLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            Runnable r = pendingAfterPermission;
            pendingAfterPermission = null;
            if (granted && r != null) r.run(); else Util.toast(this, R.string.perm_denied);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> confirmExit());
        undoBtn.setOnClickListener(v -> ev.undo());
        redoBtn.setOnClickListener(v -> ev.redo());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveProject(null));
        findViewById(R.id.btnExport).setOnClickListener(v -> showExportDialog());

        sizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser || updatingUi) return;
                Element e = ev.getSelected();
                if (e == null) return;
                e.scale = Math.max(0.05f, p / 100f);
                ev.endChange();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { if (ev.getSelected() != null) ev.beginChange(); }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        rotSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (!fromUser || updatingUi) return;
                Element e = ev.getSelected();
                if (e == null) return;
                e.rotation = p - 180;
                ev.endChange();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { if (ev.getSelected() != null) ev.beginChange(); }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { confirmExit(); }
        });

        buildToolsBar();
        updateUndoRedo();
        loadDesign();
    }

    // ---------------------------------------------------------------- loading
    private void loadDesign() {
        final String pid = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        final String tid = getIntent().getStringExtra(EXTRA_TEMPLATE_ID);
        final int cw = getIntent().getIntExtra(EXTRA_W, 0);
        final int ch = getIntent().getIntExtra(EXTRA_H, 0);
        projectName = getString(R.string.design_prefix) + " " + new SimpleDateFormat("MMdd-HHmm", Locale.US).format(new Date());
        Bg.run(() -> {
            Design d = null;
            String id = pid;
            String name = null;
            boolean fav = false;
            try {
                if (pid != null) {
                    Project p = ProjectStore.loadProject(this, pid);
                    d = ProjectStore.loadDesign(this, pid);
                    name = p.name;
                    fav = p.favorite;
                } else {
                    Template t = tid != null ? TemplateRepository.get().find(tid) : null;
                    if (t != null && t.design != null) {
                        d = t.design.copy();
                        if (cw > 0 && ch > 0) { d.width = cw; d.height = ch; }
                    } else {
                        d = new Design();
                        if (cw > 0 && ch > 0) { d.width = cw; d.height = ch; }
                    }
                    id = UUID.randomUUID().toString();
                }
                for (Element e : d.elements) {
                    if (e.type == Element.IMAGE && e.imagePath != null && !e.imagePath.isEmpty()) BitmapStore.get(e.imagePath);
                }
            } catch (Throwable t) {
                d = null;
            }
            final Design fd = d;
            final String fid = id, fname = name;
            final boolean ffav = fav;
            Bg.main(() -> {
                if (fd == null) { Util.toast(this, R.string.load_failed); finish(); return; }
                projectId = fid;
                if (fname != null) projectName = fname;
                favorite = ffav;
                ev.setDesign(fd);
                loaded = true;
                dirty = false;
                updateUndoRedo();
            });
        });
    }

    // ---------------------------------------------------------------- listener
    @Override public void onSelectionChanged(Element e) {
        selPanel.setVisibility(e == null ? View.GONE : View.VISIBLE);
        rebuildSelectionBar(e);
        syncSliders(e);
    }

    @Override public void onChanged() {
        if (loaded) dirty = true;
        updateUndoRedo();
        syncSliders(ev.getSelected());
    }

    @Override public void onRequestImage(Element placeholder) {
        pendingImageTarget = placeholder;
        imagePicker.launch("image/*");
    }

    @Override public void onEditText(Element e) { showTextDialog(e); }

    private void updateUndoRedo() {
        undoBtn.setAlpha(ev.canUndo() ? 1f : 0.35f);
        redoBtn.setAlpha(ev.canRedo() ? 1f : 0.35f);
    }

    private void syncSliders(Element e) {
        if (e == null) return;
        updatingUi = true;
        sizeSeek.setProgress(Math.max(0, Math.min(500, Math.round(e.scale * 100))));
        float r = ((e.rotation + 180f) % 360f + 360f) % 360f - 180f;
        rotSeek.setProgress(Math.max(0, Math.min(360, Math.round(r + 180f))));
        updatingUi = false;
    }

    // ---------------------------------------------------------------- ui helpers
    private TextView chip(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(13);
        t.setTextColor(getColor(R.color.text));
        t.setBackgroundResource(R.drawable.bg_chip);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Util.dp(this, 14), Util.dp(this, 8), Util.dp(this, 14), Util.dp(this, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Util.dp(this, 4);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        t.setOnClickListener(l);
        return t;
    }

    private void modify(Consumer<Element> c) {
        Element e = ev.getSelected();
        if (e == null) return;
        ev.beginChange();
        c.accept(e);
        ev.endChange();
    }

    private void buildToolsBar() {
        toolsBar.removeAllViews();
        toolsBar.addView(chip("🖼  " + getString(R.string.tool_photo), v -> { pendingImageTarget = null; imagePicker.launch("image/*"); }));
        toolsBar.addView(chip("🅣  " + getString(R.string.tool_text), v -> addText()));
        toolsBar.addView(chip("◆  " + getString(R.string.tool_shape), v -> showShapeDialog(true)));
        toolsBar.addView(chip("😀  " + getString(R.string.tool_sticker), v -> showStickerDialog()));
        toolsBar.addView(chip("🎨  " + getString(R.string.tool_bg), v -> showBackgroundDialog()));
        toolsBar.addView(chip("▣  " + getString(R.string.tool_frame), v -> showFrameDialog()));
    }

    private void rebuildSelectionBar(Element e) {
        selBar.removeAllViews();
        if (e == null) return;
        if (e.type == Element.TEXT) {
            selBar.addView(chip(getString(R.string.act_edit), v -> { Element s = ev.getSelected(); if (s != null) showTextDialog(s); }));
            selBar.addView(chip(getString(R.string.act_font), v -> showFontDialog()));
            selBar.addView(chip(getString(R.string.act_color), v -> ColorPicker.show(this, getString(R.string.pick_color), false, c -> modify(x -> x.color = c))));
            selBar.addView(chip(getString(R.string.act_bold), v -> modify(x -> x.bold = !x.bold)));
            selBar.addView(chip(getString(R.string.act_italic), v -> modify(x -> x.italic = !x.italic)));
            selBar.addView(chip(getString(R.string.act_align), v -> modify(x -> x.align = (x.align + 1) % 3)));
            selBar.addView(chip(getString(R.string.act_shadow), v -> modify(x -> x.shadow = !x.shadow)));
            selBar.addView(chip(getString(R.string.act_outline), v -> modify(x -> x.outline = !x.outline)));
            selBar.addView(chip(getString(R.string.act_outline_color), v -> ColorPicker.show(this, getString(R.string.pick_color), false, c -> modify(x -> { x.outlineColor = c; x.outline = true; }))));
            selBar.addView(chip(getString(R.string.act_text_bg), v -> ColorPicker.show(this, getString(R.string.pick_color), true, c -> modify(x -> x.textBg = c))));
        } else if (e.type == Element.IMAGE) {
            selBar.addView(chip(getString(R.string.act_replace), v -> { pendingImageTarget = ev.getSelected(); imagePicker.launch("image/*"); }));
            selBar.addView(chip(getString(R.string.act_crop), v -> showCropDialog()));
            selBar.addView(chip(getString(R.string.act_mask), v -> showMaskDialog()));
        } else {
            selBar.addView(chip(getString(R.string.act_shape), v -> showShapeDialog(false)));
            selBar.addView(chip(getString(R.string.act_color), v -> ColorPicker.show(this, getString(R.string.pick_color), false, c -> modify(x -> x.color = c))));
            selBar.addView(chip(getString(R.string.act_gradient), v -> ColorPicker.show(this, getString(R.string.act_gradient), true, c -> modify(x -> x.color2 = c))));
        }
        selBar.addView(chip(getString(R.string.act_opacity), v -> showOpacityDialog()));
        selBar.addView(chip(getString(R.string.act_flip), v -> modify(x -> x.flipH = !x.flipH)));
        selBar.addView(chip(getString(R.string.act_rotate90), v -> modify(x -> x.rotation += 90f)));
        selBar.addView(chip(getString(R.string.act_up), v -> ev.moveSelected(1)));
        selBar.addView(chip(getString(R.string.act_down), v -> ev.moveSelected(-1)));
        selBar.addView(chip(getString(R.string.act_duplicate), v -> ev.duplicateSelected()));
        selBar.addView(chip(getString(R.string.act_delete), v -> ev.deleteSelected()));
    }

    // ---------------------------------------------------------------- adding things
    private void addText() {
        Design d = ev.getDesign();
        Element e = new Element();
        e.type = Element.TEXT;
        e.text = getString(R.string.default_text);
        e.w = d.width * 0.7f;
        e.textSize = d.width * 0.07f;
        e.x = d.width / 2f;
        e.y = d.height / 2f;
        float lum = (0.299f * Color.red(d.bg1) + 0.587f * Color.green(d.bg1) + 0.114f * Color.blue(d.bg1)) / 255f;
        e.color = lum > 0.55f ? 0xFF222222 : 0xFFFFFFFF;
        ev.addElement(e);
        showTextDialog(e);
    }

    private void showTextDialog(final Element target) {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setText(target.text);
        et.setSelection(target.text.length());
        et.setMinLines(2);
        LinearLayout box = new LinearLayout(this);
        int p = Util.dp(this, 20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(et, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle(R.string.act_edit)
                .setView(box)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String s = et.getText().toString();
                    if (s.trim().isEmpty()) return;
                    Element cur = ev.getSelected() != null ? ev.getSelected() : target;
                    ev.beginChange();
                    cur.text = s;
                    ev.endChange();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showFontDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.act_font)
                .setItems(Element.FONTS, (d, i) -> modify(x -> x.font = Element.FONTS[i]))
                .show();
    }

    private void showShapeDialog(final boolean add) {
        String[] names = {getString(R.string.shape_rect), getString(R.string.shape_round), getString(R.string.shape_circle),
                getString(R.string.shape_triangle), getString(R.string.shape_star), getString(R.string.shape_heart)};
        new AlertDialog.Builder(this).setTitle(add ? R.string.tool_shape : R.string.act_shape)
                .setItems(names, (d, i) -> {
                    if (add) {
                        Design ds = ev.getDesign();
                        Element e = new Element();
                        e.type = Element.SHAPE;
                        e.shape = i;
                        e.w = e.h = Math.min(ds.width, ds.height) * 0.35f;
                        e.x = ds.width / 2f;
                        e.y = ds.height / 2f;
                        e.color = 0xFFFF5722;
                        ev.addElement(e);
                    } else {
                        modify(x -> x.shape = i);
                    }
                }).show();
    }

    private void showStickerDialog() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        final AlertDialog[] dlg = new AlertDialog[1];
        for (final String s : EMOJIS) {
            TextView t = new TextView(this);
            t.setText(s);
            t.setTextSize(28);
            t.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Util.dp(this, 56);
            lp.height = Util.dp(this, 56);
            t.setLayoutParams(lp);
            t.setOnClickListener(v -> {
                Design d = ev.getDesign();
                Element e = new Element();
                e.type = Element.TEXT;
                e.text = s;
                e.textSize = d.width * 0.2f;
                e.w = e.textSize * 2f;
                e.x = d.width / 2f;
                e.y = d.height / 2f;
                ev.addElement(e);
                dlg[0].dismiss();
            });
            grid.addView(t);
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setPadding(Util.dp(this, 12), Util.dp(this, 8), Util.dp(this, 12), 0);
        sv.addView(grid);
        dlg[0] = new AlertDialog.Builder(this).setTitle(R.string.tool_sticker).setView(sv).show();
    }

    private void showBackgroundDialog() {
        String[] items = {getString(R.string.bg_solid), getString(R.string.bg_grad_start), getString(R.string.bg_grad_end), getString(R.string.bg_presets)};
        new AlertDialog.Builder(this).setTitle(R.string.tool_bg).setItems(items, (d, i) -> {
            final Design ds = ev.getDesign();
            if (i == 0) {
                ColorPicker.show(this, getString(R.string.pick_color), false, c -> { ev.beginChange(); ds.bg1 = c; ds.bg2 = c; ev.endChange(); });
            } else if (i == 1) {
                ColorPicker.show(this, getString(R.string.pick_color), false, c -> { ev.beginChange(); ds.bg1 = c; ev.endChange(); });
            } else if (i == 2) {
                ColorPicker.show(this, getString(R.string.pick_color), false, c -> { ev.beginChange(); ds.bg2 = c; ev.endChange(); });
            } else {
                new AlertDialog.Builder(this).setTitle(R.string.bg_presets).setItems(GRADIENT_NAMES, (d2, k) -> {
                    ev.beginChange();
                    ds.bg1 = GRADIENTS[k][0];
                    ds.bg2 = GRADIENTS[k][1];
                    ev.endChange();
                }).show();
            }
        }).show();
    }

    private void showFrameDialog() {
        String[] items = {getString(R.string.frame_none), getString(R.string.frame_solid), getString(R.string.frame_double),
                getString(R.string.frame_round), getString(R.string.frame_dashed)};
        new AlertDialog.Builder(this).setTitle(R.string.tool_frame).setItems(items, (d, i) -> {
            final Design ds = ev.getDesign();
            if (i == 0) {
                ev.beginChange(); ds.frame = 0; ev.endChange();
            } else {
                ColorPicker.show(this, getString(R.string.pick_color), false, c -> {
                    ev.beginChange();
                    ds.frame = i;
                    ds.frameColor = c;
                    ds.frameWidth = Math.min(ds.width, ds.height) * 0.025f;
                    ev.endChange();
                });
            }
        }).show();
    }

    private void showCropDialog() {
        String[] items = {getString(R.string.crop_original), "1:1", "4:3", "3:4", "16:9", "9:16"};
        new AlertDialog.Builder(this).setTitle(R.string.act_crop).setItems(items, (d, i) -> modify(x -> {
            if (i == 0) {
                Bitmap b = BitmapStore.get(x.imagePath);
                if (b != null) x.h = x.w * b.getHeight() / (float) b.getWidth();
            } else {
                x.h = x.w * RATIOS_H[i] / RATIOS_W[i];
            }
        })).show();
    }

    private void showMaskDialog() {
        String[] items = {getString(R.string.mask_none), getString(R.string.mask_round), getString(R.string.mask_circle)};
        new AlertDialog.Builder(this).setTitle(R.string.act_mask)
                .setItems(items, (d, i) -> modify(x -> x.shape = i)).show();
    }

    private void showOpacityDialog() {
        Element e = ev.getSelected();
        if (e == null) return;
        final SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(Math.round(e.alpha * 100f / 255f));
        LinearLayout box = new LinearLayout(this);
        int p = Util.dp(this, 20);
        box.setPadding(p, p, p, 0);
        box.addView(sb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int pr, boolean fromUser) {
                if (!fromUser) return;
                Element cur = ev.getSelected();
                if (cur == null) return;
                cur.alpha = Math.max(10, Math.round(pr * 255f / 100f));
                ev.endChange();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { ev.beginChange(); }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        new AlertDialog.Builder(this).setTitle(R.string.act_opacity).setView(box)
                .setPositiveButton(R.string.ok, null).show();
    }

    // ---------------------------------------------------------------- images
    private void onImagePicked(Uri uri) {
        final Element target = pendingImageTarget;
        pendingImageTarget = null;
        if (uri == null || projectId == null) return;
        Bg.run(() -> {
            try {
                final String path = ImageImporter.importImage(this, uri, ProjectStore.imagesDir(this, projectId));
                Bg.main(() -> applyImage(target, path));
            } catch (Throwable t) {
                Bg.main(() -> Util.toast(this, R.string.image_failed));
            }
        });
    }

    private void applyImage(Element target, String path) {
        Bitmap b = BitmapStore.get(path);
        if (b == null) { Util.toast(this, R.string.image_failed); return; }
        Design d = ev.getDesign();
        if (target == null) {
            for (Element e : d.elements) {
                if (e.type == Element.IMAGE && (e.imagePath == null || e.imagePath.isEmpty())) { target = e; break; }
            }
        }
        if (target != null && d.elements.contains(target)) {
            final Element t = target;
            ev.beginChange();
            t.imagePath = path;
            ev.endChange();
            ev.select(t);
        } else {
            Element e = new Element();
            e.type = Element.IMAGE;
            float s = Math.min(d.width * 0.7f / b.getWidth(), d.height * 0.7f / b.getHeight());
            e.w = b.getWidth() * s;
            e.h = b.getHeight() * s;
            e.x = d.width / 2f;
            e.y = d.height / 2f;
            e.imagePath = path;
            ev.addElement(e);
        }
    }

    // ---------------------------------------------------------------- save / export
    private AlertDialog busy(int msg) {
        AlertDialog dlg = new AlertDialog.Builder(this).setMessage(msg).setCancelable(false).create();
        dlg.show();
        return dlg;
    }

    private void saveProject(final Runnable after) {
        if (!loaded) return;
        final Design snap = ev.getDesign().copy();
        final String pid = projectId, name = projectName;
        final boolean fav = favorite;
        Bg.run(() -> {
            try {
                Bitmap pv = ExportUtil.render(snap, 512f / Math.max(snap.width, snap.height), false);
                ProjectStore.save(this, pid, name, fav, snap, pv);
                Bg.main(() -> { dirty = false; Util.toast(this, R.string.saved); if (after != null) after.run(); });
            } catch (Throwable t) {
                Bg.main(() -> Util.toast(this, R.string.save_failed));
            }
        });
    }

    private void showExportDialog() {
        if (!loaded) return;
        String[] items = {getString(R.string.export_jpg_std), getString(R.string.export_jpg_high),
                getString(R.string.export_png_std), getString(R.string.export_png_high), getString(R.string.export_share)};
        new AlertDialog.Builder(this).setTitle(R.string.export).setItems(items, (d, i) -> {
            if (i == 4) ShareUtil.shareDesign(this, ev.getDesign());
            else doExport(i >= 2, i % 2 == 1);
        }).show();
    }

    private void doExport(final boolean png, final boolean high) {
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingAfterPermission = () -> doExport(png, high);
            permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
        final Design snap = ev.getDesign().copy();
        final AlertDialog dlg = busy(R.string.exporting);
        Bg.run(() -> {
            String err = null;
            try {
                float scale = ExportUtil.safeScale(snap, high ? 2f : 1f);
                Bitmap b = ExportUtil.render(snap, scale, false);
                ExportUtil.saveToGallery(this, b, png, high ? 100 : 88);
                b.recycle();
            } catch (OutOfMemoryError oom) {
                err = "memory";
            } catch (Throwable t) {
                err = String.valueOf(t.getMessage());
            }
            final String fe = err;
            Bg.main(() -> {
                dlg.dismiss();
                if (fe == null) Util.toast(this, R.string.export_ok);
                else Util.toast(this, getString(R.string.export_failed) + ": " + fe);
            });
        });
    }

    // ---------------------------------------------------------------- exit
    private void confirmExit() {
        if (!dirty) { leave(); return; }
        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_title)
                .setMessage(R.string.exit_msg)
                .setPositiveButton(R.string.save, (d, w) -> saveProject(this::leave))
                .setNegativeButton(R.string.discard, (d, w) -> leave())
                .setNeutralButton(R.string.cancel, null)
                .show();
    }

    private void leave() {
        final String pid = projectId;
        if (pid != null && !ProjectStore.exists(this, pid)) {
            final android.content.Context app = getApplicationContext();
            Bg.run(() -> ProjectStore.delete(app, pid));      // drop images of a never-saved design
        }
        finish();
    }
}

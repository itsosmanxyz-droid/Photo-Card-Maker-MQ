package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.model.CardTemplate;
import com.osman.photocardmaker.model.Decoration;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.render.ExportEngine;
import com.osman.photocardmaker.render.TemplateRenderer;
import com.osman.photocardmaker.storage.AppSettings;
import com.osman.photocardmaker.storage.FileUtil;
import com.osman.photocardmaker.storage.ImageUtils;
import com.osman.photocardmaker.storage.LocalStorage;
import com.osman.photocardmaker.storage.LogoStore;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.storage.TemplateStore;
import com.osman.photocardmaker.templates.BlankTemplate;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * The photo card editor: live preview on top, tabbed controls below.
 * Open it with EXTRA_TEMPLATE (template JSON) or EXTRA_PROJECT (saved project id).
 */
public class EditorActivity extends Activity {

    public static final String EXTRA_TEMPLATE = "template_json";
    public static final String EXTRA_PROJECT = "project_id";

    private static final int REQ_PHOTO = 101;
    private static final int REQ_LOGO = 102;
    private static final int REQ_FONT_HEADER = 103;
    private static final int REQ_FONT_BODY = 104;
    private static final int REQ_STORAGE = 201;

    private static final String[] TAB_NAMES = {"Text", "Photo", "Logo", "Colors", "Layout", "Shapes"};
    private static final String[] FAMILIES = {"Sans-serif", "Serif", "Monospace"};
    private static final String[] ALIGNS = {"Left", "Center", "Right"};
    private static final String[] SHAPES = {"Rectangle", "Circle"};
    private static final String[] SIZE_NAMES = {
            "Square 1080 x 1080", "Portrait 1080 x 1350", "Story 1080 x 1920",
            "Poster 1080 x 1527", "YouTube 1280 x 720", "Landscape 1200 x 630"};
    private static final int[][] SIZES = {
            {1080, 1080}, {1080, 1350}, {1080, 1920}, {1080, 1527}, {1280, 720}, {1200, 630}};

    private interface TextCallback {
        void onText(String text);
    }

    private CardTemplate t;
    private String projectId;
    private String projectTitle;
    private String photoPath;
    private String logoPath;
    private Bitmap photo;
    private Bitmap logo;
    private boolean dirty;
    private int currentTab;

    private CardPreviewView preview;
    private PanelBuilder pb;
    private TextView[] tabViews;
    private AppSettings settings;
    private LocalStorage storage;

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            dirty = true;
            preview.invalidate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        settings = new AppSettings(this);
        storage = new LocalStorage(this);

        String pid = getIntent().getStringExtra(EXTRA_PROJECT);
        if (pid != null) {
            ProjectStore.Info info = ProjectStore.load(this, pid);
            if (info != null) {
                t = info.template;
                projectId = info.id;
                projectTitle = info.title;
                photoPath = info.photoPath;
                logoPath = info.logoPath;
            }
        }
        if (t == null) {
            String json = getIntent().getStringExtra(EXTRA_TEMPLATE);
            if (json != null) {
                try {
                    t = CardTemplate.fromJson(new JSONObject(json));
                } catch (JSONException e) {
                    t = null;
                }
            }
        }
        if (t == null) {
            t = BlankTemplate.create(this);
        }
        if (logoPath == null) {
            String d = settings.getDefaultLogo();
            if (d != null && new File(d).exists()) {
                logoPath = d;
            }
        }
        photo = ImageUtils.decodeFile(photoPath, 2048);
        logo = ImageUtils.decodeFile(logoPath, 1024);

        preview = (CardPreviewView) findViewById(R.id.preview);
        preview.setData(t, photo, logo);
        preview.setOnEditedListener(refresh);

        pb = new PanelBuilder(this, (LinearLayout) findViewById(R.id.panel), refresh);

        buildTabs();
        selectTab(0);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProject();
            }
        });
        findViewById(R.id.btnSaveTpl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTemplate();
            }
        });
        findViewById(R.id.btnExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExport();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!dirty) {
            finish();
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage("Leave without saving your changes?")
                .setNegativeButton("Stay", null)
                .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        finish();
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------ tabs

    private void buildTabs() {
        LinearLayout bar = (LinearLayout) findViewById(R.id.tabBar);
        tabViews = new TextView[TAB_NAMES.length];
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            TextView tv = new TextView(this);
            tv.setText(TAB_NAMES[i]);
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            int p = dp(14);
            tv.setPadding(p, dp(10), p, dp(10));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectTab(index);
                }
            });
            bar.addView(tv);
            tabViews[i] = tv;
        }
    }

    private void selectTab(int index) {
        currentTab = index;
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setTextColor(i == index ? 0xFFC62828 : 0xFF6B6F7B);
        }
        buildPanel();
    }

    private void buildPanel() {
        pb.clear();
        if (currentTab == 0) {
            buildTextTab();
        } else if (currentTab == 1) {
            buildPhotoTab();
        } else if (currentTab == 2) {
            buildLogoTab();
        } else if (currentTab == 3) {
            buildColorTab();
        } else if (currentTab == 4) {
            buildLayoutTab();
        } else {
            buildShapesTab();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    // ------------------------------------------------------------- text tab

    private void buildTextTab() {
        pb.section("Texts");
        pb.text("Header label", t.headerText, false, new PanelBuilder.StringSetter() {
            @Override public void set(String v) { t.headerText = v; }
        });
        pb.text("Headline", t.headline, true, new PanelBuilder.StringSetter() {
            @Override public void set(String v) { t.headline = v; }
        });
        pb.text("Sub text", t.subText, false, new PanelBuilder.StringSetter() {
            @Override public void set(String v) { t.subText = v; }
        });

        pb.section("Date");
        pb.toggle("Show date on card", t.showDate, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.showDate = v; }
        });
        pb.toggle("Use today's date automatically", t.dateAuto, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.dateAuto = v; }
        });
        pb.text("Custom date text (used when automatic is off)", t.dateText, false,
                new PanelBuilder.StringSetter() {
                    @Override public void set(String v) { t.dateText = v; }
                });
        pb.slider("Date size", 16, 80, t.dateSize, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.dateSize = v; }
        });
        pb.choice("Date position", ALIGNS, t.dateAlign, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.dateAlign = v; }
        });
        pb.slider("Date up / down", -0.1f, 0.1f, t.dateOffsetY, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.dateOffsetY = v; }
        });

        pb.section("Header font");
        pb.choice("Font", FAMILIES, t.headerFamily, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headerFamily = v; }
        });
        pb.toggle("Bold", t.headerBold, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.headerBold = v; }
        });
        pb.toggle("Italic", t.headerItalic, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.headerItalic = v; }
        });
        pb.slider("Size", 12, 140, t.headerTextSize, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.headerTextSize = v; }
        });
        pb.choice("Alignment", ALIGNS, t.headerAlign, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headerAlign = v; }
        });
        pb.button("Import custom font (.ttf / .otf)", new Runnable() {
            @Override public void run() { pickFile(REQ_FONT_HEADER, "*/*"); }
        });
        pb.button("Use default font", new Runnable() {
            @Override
            public void run() {
                t.headerFontPath = "";
                refresh.run();
            }
        });

        pb.section("Headline and sub text font");
        pb.choice("Font", FAMILIES, t.headlineFamily, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headlineFamily = v; }
        });
        pb.toggle("Bold", t.headlineBold, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.headlineBold = v; }
        });
        pb.toggle("Italic", t.headlineItalic, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.headlineItalic = v; }
        });
        pb.slider("Headline size", 20, 200, t.headlineSize, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.headlineSize = v; }
        });
        pb.slider("Sub text size", 12, 100, t.subSize, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.subSize = v; }
        });
        pb.choice("Alignment", ALIGNS, t.headlineAlign, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headlineAlign = v; }
        });
        pb.button("Import custom font (.ttf / .otf)", new Runnable() {
            @Override public void run() { pickFile(REQ_FONT_BODY, "*/*"); }
        });
        pb.button("Use default font", new Runnable() {
            @Override
            public void run() {
                t.headlineFontPath = "";
                refresh.run();
            }
        });
    }

    // ------------------------------------------------------------ photo tab

    private void buildPhotoTab() {
        pb.section("Photo");
        pb.button("Choose photo from gallery", new Runnable() {
            @Override public void run() { pickFile(REQ_PHOTO, "image/*"); }
        });
        pb.button("Remove photo", new Runnable() {
            @Override
            public void run() {
                photo = null;
                photoPath = null;
                preview.setBitmaps(photo, logo);
                refresh.run();
            }
        });
        pb.slider("Zoom", 1f, 4f, t.photoZoom, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoZoom = v; }
        });
        pb.slider("Move left / right", -1f, 1f, t.photoOffX, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoOffX = v; }
        });
        pb.slider("Move up / down", -1f, 1f, t.photoOffY, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoOffY = v; }
        });

        pb.section("Photo frame");
        pb.choice("Shape", SHAPES, t.photoShape, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.photoShape = v; }
        });
        pb.slider("Corner radius", 0, 200, t.photoCorner, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoCorner = v; }
        });
        pb.slider("Border width", 0, 60, t.photoBorderW, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoBorderW = v; }
        });
        pb.color("Border colour", t.photoBorderColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.photoBorderColor = v; }
        });

        pb.section("Photo position (fraction of card)");
        pb.slider("Left", 0, 1, t.photoL, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoL = v; }
        });
        pb.slider("Top", 0, 1, t.photoT, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoT = v; }
        });
        pb.slider("Right", 0, 1, t.photoR, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoR = v; }
        });
        pb.slider("Bottom", 0, 1, t.photoB, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.photoB = v; }
        });
    }

    // ------------------------------------------------------------- logo tab

    private void buildLogoTab() {
        pb.section("Logo");
        pb.toggle("Show logo", t.showLogo, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.showLogo = v; }
        });
        pb.button("Upload new logo", new Runnable() {
            @Override public void run() { pickFile(REQ_LOGO, "image/*"); }
        });
        pb.button("Remove logo (show placeholder)", new Runnable() {
            @Override
            public void run() {
                logo = null;
                logoPath = null;
                preview.setBitmaps(photo, logo);
                refresh.run();
            }
        });

        List<File> logos = LogoStore.list(this);
        if (!logos.isEmpty()) {
            pb.section("My logos (tap to use)");
            HorizontalScrollView hs = new HorizontalScrollView(this);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            hs.addView(row);
            for (final File f : logos) {
                Bitmap b = ImageUtils.decodeFile(f.getPath(), 160);
                if (b == null) {
                    continue;
                }
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(b);
                iv.setBackgroundColor(0xFFDDDDDD);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(72));
                lp.setMargins(0, 0, dp(8), 0);
                row.addView(iv, lp);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logoPath = f.getPath();
                        logo = ImageUtils.decodeFile(logoPath, 1024);
                        preview.setBitmaps(photo, logo);
                        refresh.run();
                    }
                });
            }
            pb.add(hs);
        }

        pb.section("Logo style");
        pb.choice("Shape", SHAPES, t.logoShape, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.logoShape = v; }
        });
        pb.color("Logo background", t.logoBgColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.logoBgColor = v; }
        });

        pb.section("Logo position (fraction of card)");
        pb.slider("Left", 0, 1, t.logoL, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.logoL = v; }
        });
        pb.slider("Top", 0, 1, t.logoT, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.logoT = v; }
        });
        pb.slider("Right", 0, 1, t.logoR, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.logoR = v; }
        });
        pb.slider("Bottom", 0, 1, t.logoB, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.logoB = v; }
        });
    }

    // ----------------------------------------------------------- colours tab

    private void buildColorTab() {
        pb.section("Background (gradient)");
        pb.color("Colour 1", t.bgColor1, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.bgColor1 = v; }
        });
        pb.color("Colour 2 (same as 1 = solid)", t.bgColor2, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.bgColor2 = v; }
        });
        pb.slider("Gradient angle", 0, 360, t.gradientAngle, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.gradientAngle = Math.round(v); }
        });

        pb.section("Header");
        pb.color("Header colour", t.headerColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headerColor = v; }
        });
        pb.color("Header text colour", t.headerTextColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headerTextColor = v; }
        });

        pb.section("Footer");
        pb.color("Footer colour", t.footerColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.footerColor = v; }
        });
        pb.color("Footer accent line", t.footerAccentColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.footerAccentColor = v; }
        });
        pb.color("Headline colour", t.headlineColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.headlineColor = v; }
        });
        pb.color("Sub text colour", t.subColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.subColor = v; }
        });

        pb.section("Date badge");
        pb.color("Date text colour", t.dateColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.dateColor = v; }
        });
        pb.color("Date badge colour", t.dateBgColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.dateBgColor = v; }
        });

        pb.section("Borders");
        pb.color("Card border colour", t.borderColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.borderColor = v; }
        });
        pb.color("Photo border colour", t.photoBorderColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.photoBorderColor = v; }
        });
        pb.color("Logo background", t.logoBgColor, new PanelBuilder.IntSetter() {
            @Override public void set(int v) { t.logoBgColor = v; }
        });
    }

    // ------------------------------------------------------------ layout tab

    private void buildLayoutTab() {
        int match = -1;
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i][0] == t.width && SIZES[i][1] == t.height) {
                match = i;
            }
        }
        final int offset = match < 0 ? 1 : 0;
        String[] names = new String[SIZE_NAMES.length + offset];
        if (offset == 1) {
            names[0] = "Current " + t.width + " x " + t.height;
        }
        for (int i = 0; i < SIZE_NAMES.length; i++) {
            names[i + offset] = SIZE_NAMES[i];
        }
        pb.section("Canvas");
        pb.choice("Card size", names, match < 0 ? 0 : match, new PanelBuilder.IntSetter() {
            @Override
            public void set(int v) {
                int i = v - offset;
                if (i >= 0 && i < SIZES.length) {
                    t.width = SIZES[i][0];
                    t.height = SIZES[i][1];
                }
            }
        });
        pb.slider("Card border width", 0, 60, t.borderWidth, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.borderWidth = v; }
        });

        pb.section("Header");
        pb.toggle("Show header", t.showHeader, new PanelBuilder.BoolSetter() {
            @Override public void set(boolean v) { t.showHeader = v; }
        });
        pb.slider("Top", 0, 1, t.headerTopFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.headerTopFrac = v; }
        });
        pb.slider("Height", 0.02f, 0.3f, t.headerHeightFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.headerHeightFrac = v; }
        });
        pb.slider("Width", 0.1f, 1f, t.headerWidthFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.headerWidthFrac = v; }
        });

        pb.section("Footer");
        pb.slider("Top", 0.1f, 0.98f, t.footerTopFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.footerTopFrac = v; }
        });
        pb.slider("Width", 0.2f, 1f, t.footerWidthFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.footerWidthFrac = v; }
        });
        pb.slider("Accent line height", 0f, 0.05f, t.footerAccentHeightFrac,
                new PanelBuilder.FloatSetter() {
                    @Override public void set(float v) { t.footerAccentHeightFrac = v; }
                });
        pb.slider("Text padding", 0f, 0.2f, t.footerPadFrac, new PanelBuilder.FloatSetter() {
            @Override public void set(float v) { t.footerPadFrac = v; }
        });
    }

    // ------------------------------------------------------------ shapes tab

    private void buildShapesTab() {
        pb.section("Decorations");
        pb.button("+ Rectangle", new Runnable() {
            @Override public void run() { addDecoration(Decoration.RECT); }
        });
        pb.button("+ Circle", new Runnable() {
            @Override public void run() { addDecoration(Decoration.CIRCLE); }
        });
        pb.button("+ Slanted bar", new Runnable() {
            @Override public void run() { addDecoration(Decoration.SLANT); }
        });

        final String[] typeNames = {"Rectangle", "Circle", "Slanted bar"};
        for (int i = 0; i < t.decorations.size(); i++) {
            final Decoration d = t.decorations.get(i);
            pb.section("Shape " + (i + 1) + " - " + typeNames[Math.max(0, Math.min(2, d.type))]);
            pb.color("Colour", d.color, new PanelBuilder.IntSetter() {
                @Override public void set(int v) { d.color = v; }
            });
            pb.toggle("Draw above photo and text", d.onTop, new PanelBuilder.BoolSetter() {
                @Override public void set(boolean v) { d.onTop = v; }
            });
            pb.slider("Left", -0.5f, 1.5f, d.l, new PanelBuilder.FloatSetter() {
                @Override public void set(float v) { d.l = v; }
            });
            pb.slider("Top", -0.5f, 1.5f, d.t, new PanelBuilder.FloatSetter() {
                @Override public void set(float v) { d.t = v; }
            });
            pb.slider("Right", -0.5f, 1.5f, d.r, new PanelBuilder.FloatSetter() {
                @Override public void set(float v) { d.r = v; }
            });
            pb.slider("Bottom", -0.5f, 1.5f, d.b, new PanelBuilder.FloatSetter() {
                @Override public void set(float v) { d.b = v; }
            });
            pb.button("Delete this shape", new Runnable() {
                @Override
                public void run() {
                    t.decorations.remove(d);
                    refresh.run();
                    buildPanel();
                }
            });
        }
    }

    private void addDecoration(int type) {
        Decoration d = new Decoration(type, 0.30f, 0.40f, 0.70f, 0.55f, 0x99FFC107, true);
        t.decorations.add(d);
        refresh.run();
        buildPanel();
    }

    // -------------------------------------------------------- file picking

    private void pickFile(int req, String mime) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType(mime);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            AdManager.suppressAppOpenOnce();
            startActivityForResult(Intent.createChooser(i, "Select file"), req);
        } catch (Exception e) {
            toast("No file picker available");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        long now = System.currentTimeMillis();
        if (req == REQ_PHOTO) {
            File dest = new File(storage.getPhotosDir(), "photo_" + now + ".jpg");
            if (ImageUtils.importImage(this, uri, dest, 2048, false)) {
                photoPath = dest.getPath();
                photo = ImageUtils.decodeFile(photoPath, 2048);
                t.photoZoom = 1f;
                t.photoOffX = 0f;
                t.photoOffY = 0f;
                preview.setBitmaps(photo, logo);
                refresh.run();
            } else {
                toast("Could not open this image");
            }
        } else if (req == REQ_LOGO) {
            File f = LogoStore.add(this, uri);
            if (f != null) {
                logoPath = f.getPath();
                logo = ImageUtils.decodeFile(logoPath, 1024);
                t.showLogo = true;
                preview.setBitmaps(photo, logo);
                refresh.run();
                buildPanel();
            } else {
                toast("Could not open this image");
            }
        } else if (req == REQ_FONT_HEADER || req == REQ_FONT_BODY) {
            importFont(uri, req == REQ_FONT_HEADER);
        }
    }

    private void importFont(Uri uri, boolean header) {
        File dest = new File(storage.getFontsDir(), "font_" + System.currentTimeMillis() + ".ttf");
        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                toast("Could not read font");
                return;
            }
            FileUtil.copy(in, dest);
            Typeface.createFromFile(dest);
        } catch (Exception e) {
            dest.delete();
            toast("This is not a valid font file");
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
        if (header) {
            t.headerFontPath = dest.getPath();
        } else {
            t.headlineFontPath = dest.getPath();
        }
        refresh.run();
        toast("Font applied");
    }

    // -------------------------------------------------------- save dialogs

    private void promptText(String title, String initial, final TextCallback cb) {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setText(initial);
        et.setSelection(et.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(et)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (s.length() == 0) {
                            s = "Untitled";
                        }
                        cb.onText(s);
                    }
                })
                .show();
    }

    private void saveProject() {
        String def = projectTitle != null ? projectTitle : t.name;
        promptText("Project name", def, new TextCallback() {
            @Override
            public void onText(String s) {
                if (projectId == null) {
                    projectId = "p_" + System.currentTimeMillis();
                }
                projectTitle = s;
                Bitmap thumb = TemplateRenderer.render(t, 360, photo, logo);
                boolean ok = ProjectStore.save(EditorActivity.this, projectId, s, t,
                        photoPath, logoPath, thumb);
                if (ok) {
                    dirty = false;
                    toast("Project saved");
                } else {
                    toast("Could not save project");
                }
            }
        });
    }

    private void saveTemplate() {
        promptText("Template name", t.name + " copy", new TextCallback() {
            @Override
            public void onText(String s) {
                CardTemplate c = t.copy();
                c.id = "u_" + System.currentTimeMillis();
                c.name = s;
                c.category = "Custom";
                if (TemplateStore.save(EditorActivity.this, c)) {
                    toast("Saved to Custom Templates");
                } else {
                    toast("Could not save template");
                }
            }
        });
    }

    // -------------------------------------------------------------- export

    private void startExport() {
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29
                && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE);
            return;
        }
        doExport();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_STORAGE) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                doExport();
            } else {
                toast("Storage permission is needed to save the image");
            }
        }
    }

    private void doExport() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Exporting...");
        pd.setCancelable(false);
        pd.show();

        final CardTemplate snapshot = t.copy();
        final Bitmap p = photo;
        final Bitmap l = logo;
        final int scale = AdManager.effectiveScale(this, settings.getExportScale());
        final String format = settings.getExportFormat();
        final String name = "PhotoCard_" + System.currentTimeMillis();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri saved = null;
                String error = null;
                try {
                    Bitmap out = ExportEngine.render(snapshot, p, l, scale);
                    saved = ExportEngine.saveToGallery(EditorActivity.this, out, format, name);
                    out.recycle();
                } catch (Throwable e) {
                    error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                }
                final Uri uri = saved;
                final String err = error;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) {
                            return;
                        }
                        pd.dismiss();
                        if (uri != null) {
                            AdManager.afterExport(EditorActivity.this, new Runnable() {
                                @Override
                                public void run() {
                                    if (!isFinishing()) {
                                        showExported(uri);
                                    }
                                }
                            });
                        } else {
                            toast("Export failed: " + err);
                        }
                    }
                });
            }
        }).start();
    }

    private void showExported(final Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("Saved")
                .setMessage("Image saved to Pictures/PhotoCardMakerMQ")
                .setNegativeButton("OK", null)
                .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Intent s = new Intent(Intent.ACTION_SEND);
                        s.setType(AppSettings.FORMAT_JPG.equals(settings.getExportFormat())
                                ? "image/jpeg" : "image/png");
                        s.putExtra(Intent.EXTRA_STREAM, uri);
                        s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        AdManager.suppressAppOpenOnce();
                        startActivity(Intent.createChooser(s, "Share"));
                    }
                })
                .show();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

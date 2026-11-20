package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.storage.AppSettings;
import com.osman.photocardmaker.storage.LogoStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Upload, choose a default and delete logos. */
public class LogoManagerActivity extends Activity {

    private static final int REQ_LOGO = 301;

    private final List<File> logos = new ArrayList<File>();
    private final List<Bitmap> thumbs = new ArrayList<Bitmap>();
    private AppSettings settings;
    private LogoAdapter adapter;
    private TextView hint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        settings = new AppSettings(this);

        ((TextView) findViewById(R.id.tvGalleryTitle)).setText(R.string.logo_title);
        hint = (TextView) findViewById(R.id.tvGalleryHint);
        hint.setText(R.string.logo_hint);

        findViewById(R.id.btnGalleryBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView action = (TextView) findViewById(R.id.btnGalleryAction);
        action.setText(R.string.logo_add);
        action.setVisibility(View.VISIBLE);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    AdManager.suppressAppOpenOnce();
                    startActivityForResult(Intent.createChooser(i, "Select logo"), REQ_LOGO);
                } catch (Exception e) {
                    Toast.makeText(LogoManagerActivity.this, "No file picker available",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        adapter = new LogoAdapter();
        GridView grid = (GridView) findViewById(R.id.gridTemplates);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                settings.setDefaultLogo(logos.get(position).getPath());
                Toast.makeText(LogoManagerActivity.this, "Default logo set", Toast.LENGTH_SHORT)
                        .show();
                adapter.notifyDataSetChanged();
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(logos.get(position));
                return true;
            }
        });
        load();
    }

    private void confirmDelete(final File f) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (f.getPath().equals(settings.getDefaultLogo())) {
                            settings.setDefaultLogo(null);
                        }
                        LogoStore.delete(f);
                        load();
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_LOGO && res == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            File f = LogoStore.add(this, uri);
            if (f == null) {
                Toast.makeText(this, "Could not open this image", Toast.LENGTH_SHORT).show();
            } else {
                if (settings.getDefaultLogo() == null) {
                    settings.setDefaultLogo(f.getPath());
                }
                load();
            }
        }
    }

    private void load() {
        logos.clear();
        thumbs.clear();
        for (File f : LogoStore.list(this)) {
            Bitmap b = com.osman.photocardmaker.storage.ImageUtils.decodeFile(f.getPath(), 256);
            if (b != null) {
                logos.add(f);
                thumbs.add(b);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class LogoAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(LogoManagerActivity.this);

        @Override public int getCount() { return logos.size(); }
        @Override public Object getItem(int position) { return logos.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = inflater.inflate(R.layout.item_template, parent, false);
            }
            ImageView iv = (ImageView) v.findViewById(R.id.ivPreview);
            int h = (int) (110 * getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h));
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setBackgroundColor(0xFFDDDDDD);
            iv.setImageBitmap(thumbs.get(position));

            boolean isDefault = logos.get(position).getPath().equals(settings.getDefaultLogo());
            ((TextView) v.findViewById(R.id.tvName)).setText("Logo " + (position + 1));
            ((TextView) v.findViewById(R.id.tvCategory)).setText(
                    isDefault ? "Default logo" : "Tap to make default");
            return v;
        }
    }
}

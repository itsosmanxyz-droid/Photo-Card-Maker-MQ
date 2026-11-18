package com.osman.photocardmaker.render;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.osman.photocardmaker.model.CardTemplate;
import com.osman.photocardmaker.storage.AppSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Renders a card in high quality and saves it to the device gallery. */
public final class ExportEngine {

    private static final float MAX_DIMENSION = 4096f;
    private static final String FOLDER = "PhotoCardMakerMQ";

    private ExportEngine() {
    }

    /** Renders at scale x template size, limited to a safe maximum so memory is not exhausted. */
    public static Bitmap render(CardTemplate t, Bitmap photo, Bitmap logo, int scale) {
        float factor = Math.max(1, scale);
        float maxDim = Math.max(t.width, t.height);
        if (maxDim * factor > MAX_DIMENSION) {
            factor = MAX_DIMENSION / maxDim;
        }
        while (true) {
            try {
                int outW = Math.max(1, Math.round(t.width * factor));
                return TemplateRenderer.render(t, outW, photo, logo);
            } catch (OutOfMemoryError e) {
                factor *= 0.7f;
                if (factor < 0.4f) {
                    throw e;
                }
            }
        }
    }

    /**
     * Saves the bitmap to Pictures/PhotoCardMakerMQ and returns its content Uri.
     * Android 10+ needs no permission; older versions need storage permission.
     */
    public static Uri saveToGallery(Context ctx, Bitmap bmp, String format, String name)
            throws IOException {
        boolean png = !AppSettings.FORMAT_JPG.equals(format);
        String mime = png ? "image/png" : "image/jpeg";
        String fileName = name + (png ? ".png" : ".jpg");
        Bitmap.CompressFormat cf = png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put("_display_name", fileName);
            v.put("mime_type", mime);
            v.put("relative_path", "Pictures/" + FOLDER);
            v.put("is_pending", 1);
            Uri uri = ctx.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                throw new IOException("Could not create gallery entry");
            }
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            if (os == null) {
                throw new IOException("Could not open gallery entry");
            }
            try {
                bmp.compress(cf, 95, os);
            } finally {
                os.close();
            }
            ContentValues done = new ContentValues();
            done.put("is_pending", 0);
            ctx.getContentResolver().update(uri, done, null, null);
            return uri;
        }

        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), FOLDER);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create folder " + dir.getPath());
        }
        File f = new File(dir, fileName);
        FileOutputStream out = new FileOutputStream(f);
        try {
            bmp.compress(cf, 95, out);
        } finally {
            out.close();
        }
        ContentValues v = new ContentValues();
        v.put("_data", f.getAbsolutePath());
        v.put("title", name);
        v.put("_display_name", fileName);
        v.put("mime_type", mime);
        Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
        if (uri == null) {
            throw new IOException("Could not register image in gallery");
        }
        return uri;
    }
}

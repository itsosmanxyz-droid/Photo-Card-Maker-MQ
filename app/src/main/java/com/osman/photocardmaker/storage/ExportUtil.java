package com.osman.photocardmaker.storage;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.osman.photocardmaker.editor.DesignRenderer;
import com.osman.photocardmaker.models.Design;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExportUtil {
    private static final long MAX_PIXELS = 16_000_000L;

    public static float safeScale(Design d, float scale) {
        scale = Math.min(scale, 4096f / Math.max(d.width, d.height));
        while ((long) (d.width * scale) * (long) (d.height * scale) > MAX_PIXELS) scale *= 0.9f;
        return scale;
    }

    public static Bitmap render(Design d, float scale, boolean placeholders) {
        int w = Math.max(1, Math.round(d.width * scale));
        int h = Math.max(1, Math.round(d.height * scale));
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(Color.WHITE);
        c.scale(w / (float) d.width, h / (float) d.height);
        DesignRenderer.draw(c, d, placeholders);
        return b;
    }

    public static Uri saveToGallery(Context ctx, Bitmap b, boolean png, int quality) throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "PhotoCardMQ_" + stamp + (png ? ".png" : ".jpg");
        String mime = png ? "image/png" : "image/jpeg";
        Bitmap.CompressFormat fmt = png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentResolver r = ctx.getContentResolver();
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            v.put(MediaStore.Images.Media.MIME_TYPE, mime);
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoCardMakerMQ");
            v.put(MediaStore.Images.Media.IS_PENDING, 1);
            Uri u = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("insert failed");
            try (OutputStream os = r.openOutputStream(u)) {
                if (os == null || !b.compress(fmt, quality, os)) throw new IOException("write failed");
            } catch (IOException e) {
                r.delete(u, null, null);
                throw e;
            }
            v.clear();
            v.put(MediaStore.Images.Media.IS_PENDING, 0);
            r.update(u, v, null, null);
            return u;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PhotoCardMakerMQ");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdir failed");
            File f = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(f)) {
                if (!b.compress(fmt, quality, os)) throw new IOException("write failed");
            }
            MediaScannerConnection.scanFile(ctx, new String[]{f.getAbsolutePath()}, new String[]{mime}, null);
            return Uri.fromFile(f);
        }
    }

    public static Uri cacheForShare(Context ctx, Bitmap b) throws IOException {
        File dir = new File(ctx.getCacheDir(), "shared");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdir failed");
        File f = new File(dir, "design_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream os = new FileOutputStream(f)) {
            if (!b.compress(Bitmap.CompressFormat.PNG, 100, os)) throw new IOException("write failed");
        }
        return FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", f);
    }
}

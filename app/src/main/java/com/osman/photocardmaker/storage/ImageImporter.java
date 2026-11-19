package com.osman.photocardmaker.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import com.osman.photocardmaker.editor.BitmapStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageImporter {
    private static final int MAX = 2048;

    public static String importImage(Context c, Uri uri, File dir) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IOException("cannot open");
            BitmapFactory.decodeStream(in, null, o);
        }
        if (o.outWidth <= 0 || o.outHeight <= 0) throw new IOException("not an image");
        int s = 1;
        while (o.outWidth / s > MAX * 2 || o.outHeight / s > MAX * 2) s *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = s;
        Bitmap bmp;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, o);
        }
        if (bmp == null) throw new IOException("decode failed");

        int orient = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in != null) orient = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) { }
        Matrix m = new Matrix();
        switch (orient) {
            case ExifInterface.ORIENTATION_ROTATE_90: m.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.postScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: m.postScale(1, -1); break;
            default: break;
        }
        float big = Math.max(bmp.getWidth(), bmp.getHeight());
        if (big > MAX) { float k = MAX / big; m.postScale(k, k); }
        if (!m.isIdentity()) {
            Bitmap t = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (t != bmp) bmp.recycle();
            bmp = t;
        }
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdir failed");
        boolean png = bmp.hasAlpha();
        File f = new File(dir, "img_" + System.currentTimeMillis() + (png ? ".png" : ".jpg"));
        try (FileOutputStream os = new FileOutputStream(f)) {
            if (!bmp.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, os))
                throw new IOException("write failed");
        }
        BitmapStore.put(f.getAbsolutePath(), bmp);
        return f.getAbsolutePath();
    }
}

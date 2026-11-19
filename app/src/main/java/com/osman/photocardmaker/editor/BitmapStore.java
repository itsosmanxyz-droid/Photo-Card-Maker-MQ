package com.osman.photocardmaker.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

public class BitmapStore {
    private static final LruCache<String, Bitmap> C =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 6)) {
                @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount() / 1024; }
            };

    public static Bitmap get(String path) {
        if (path == null || path.isEmpty()) return null;
        Bitmap b = C.get(path);
        if (b == null) {
            b = decode(path, 2048);
            if (b != null) C.put(path, b);
        }
        return b;
    }

    public static void put(String path, Bitmap b) { C.put(path, b); }

    public static Bitmap decode(String path, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            int s = 1;
            while (o.outWidth / s > max * 2 || o.outHeight / s > max * 2) s *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = s;
            return BitmapFactory.decodeFile(path, o);
        } catch (Throwable t) {
            return null;
        }
    }
}

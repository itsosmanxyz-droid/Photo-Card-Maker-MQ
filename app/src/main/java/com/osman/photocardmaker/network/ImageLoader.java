package com.osman.photocardmaker.network;

import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.ImageView;

import com.osman.photocardmaker.utils.Bg;

import java.util.concurrent.Callable;

public class ImageLoader {
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 10)) {
                @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount() / 1024; }
            };

    public static void clear() { CACHE.evictAll(); }

    public static void load(final ImageView iv, final String key, final Callable<Bitmap> producer) {
        iv.setTag(key);
        Bitmap cached = CACHE.get(key);
        if (cached != null) { iv.setImageBitmap(cached); return; }
        iv.setImageDrawable(null);
        Bg.run(() -> {
            Bitmap r = null;
            try { r = producer.call(); } catch (Throwable ignored) { }
            if (r != null) CACHE.put(key, r);
            final Bitmap fr = r;
            Bg.main(() -> { if (fr != null && key.equals(iv.getTag())) iv.setImageBitmap(fr); });
        });
    }
}

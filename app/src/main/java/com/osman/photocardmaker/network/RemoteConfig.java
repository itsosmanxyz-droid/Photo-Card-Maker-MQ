package com.osman.photocardmaker.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteConfig {
    private static final int MAX_JSON = 1024 * 1024;
    private static final int MAX_IMAGE = 3 * 1024 * 1024;

    private static byte[] download(String url, int limit) throws IOException {
        if (url == null || !url.startsWith("https://")) throw new IOException("https only");
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("http " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n, total = 0;
                while ((n = in.read(buf)) > 0) {
                    total += n;
                    if (total > limit) throw new IOException("too large");
                    bo.write(buf, 0, n);
                }
                return bo.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    public static String fetchText(String url) throws IOException {
        return new String(download(url, MAX_JSON), "UTF-8");
    }

    public static Bitmap fetchBitmap(Context ctx, String url) throws IOException {
        File dir = new File(ctx.getCacheDir(), "thumbs");
        dir.mkdirs();
        File f = new File(dir, Integer.toHexString(url.hashCode()) + ".img");
        if (!f.exists()) {
            byte[] data = download(url, MAX_IMAGE);
            try (FileOutputStream os = new FileOutputStream(f)) { os.write(data); }
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int s = 1;
        while (o.outWidth / s > 1024) s *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = s;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        if (b == null) { f.delete(); throw new IOException("bad image"); }
        return b;
    }
}

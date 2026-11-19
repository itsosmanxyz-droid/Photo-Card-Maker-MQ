package com.osman.photocardmaker.utils;

import android.content.Context;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Util {
    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static boolean isBn(Context c) {
        return "bn".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }

    public static void toast(Context c, String s) { Toast.makeText(c, s, Toast.LENGTH_SHORT).show(); }
    public static void toast(Context c, int r) { Toast.makeText(c, r, Toast.LENGTH_SHORT).show(); }

    public static String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toString("UTF-8");
    }

    public static String readFile(File f) throws IOException {
        try (FileInputStream in = new FileInputStream(f)) { return readStream(in); }
    }

    public static void writeFile(File f, String s) throws IOException {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        File t = new File(parent, f.getName() + ".tmp");
        try (FileOutputStream o = new FileOutputStream(t)) { o.write(s.getBytes("UTF-8")); }
        if (!t.renameTo(f)) {
            f.delete();
            if (!t.renameTo(f)) throw new IOException("rename failed");
        }
    }

    public static void deleteRecursive(File f) {
        if (f == null) return;
        if (f.isDirectory()) {
            File[] k = f.listFiles();
            if (k != null) for (File x : k) deleteRecursive(x);
        }
        f.delete();
    }

    public static long dirSize(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long s = 0;
        File[] k = f.listFiles();
        if (k != null) for (File x : k) s += dirSize(x);
        return s;
    }

    public static String fmtDate(long t) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(t));
    }
}

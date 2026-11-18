package com.osman.photocardmaker.storage;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Manages the user's uploaded logos (stored as PNG in the app's private folder). */
public final class LogoStore {

    private LogoStore() {
    }

    private static File dir(Context ctx) {
        return new LocalStorage(ctx).getLogosDir();
    }

    /** Imports a logo from a picked image. Returns the stored file or null on failure. */
    public static File add(Context ctx, Uri uri) {
        File dest = new File(dir(ctx), "logo_" + System.currentTimeMillis() + ".png");
        if (ImageUtils.importImage(ctx, uri, dest, 1024, true)) {
            return dest;
        }
        return null;
    }

    public static List<File> list(Context ctx) {
        List<File> out = new ArrayList<File>();
        File[] files = dir(ctx).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(".png");
            }
        });
        if (files == null) {
            return out;
        }
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                long x = a.lastModified();
                long y = b.lastModified();
                return x < y ? 1 : (x > y ? -1 : 0);
            }
        });
        out.addAll(Arrays.asList(files));
        return out;
    }

    public static void delete(File f) {
        f.delete();
    }
}

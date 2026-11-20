package com.osman.photocardmaker.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Small file helpers shared by the storage classes. */
public final class FileUtil {

    private FileUtil() {
    }

    public static String readText(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    public static void writeText(File f, String text) throws IOException {
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /** Copies the stream into dest. Does not close the input stream. */
    public static void copy(InputStream in, File dest) throws IOException {
        FileOutputStream out = new FileOutputStream(dest);
        try {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } finally {
            out.close();
        }
    }
}

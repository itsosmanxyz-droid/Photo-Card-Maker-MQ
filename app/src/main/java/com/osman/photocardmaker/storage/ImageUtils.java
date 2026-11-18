package com.osman.photocardmaker.storage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Image import / decode helpers (EXIF-aware, memory-safe). */
public final class ImageUtils {

    private ImageUtils() {
    }

    /** Copies the content of a Uri into a local file. */
    public static boolean copyUriToFile(Context ctx, Uri uri, File dest) {
        InputStream in = null;
        try {
            in = ctx.getContentResolver().openInputStream(uri);
            if (in == null) {
                return false;
            }
            FileUtil.copy(in, dest);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Imports an image picked by the user: fixes rotation, shrinks it to maxSize
     * and stores it in dest as PNG (logos) or JPEG (photos).
     */
    public static boolean importImage(Context ctx, Uri uri, File dest, int maxSize, boolean png) {
        File tmp = new File(ctx.getCacheDir(), "import_" + System.nanoTime() + ".tmp");
        try {
            if (!copyUriToFile(ctx, uri, tmp)) {
                return false;
            }
            Bitmap b = decodeFile(tmp.getPath(), maxSize);
            if (b == null) {
                return false;
            }
            FileOutputStream out = new FileOutputStream(dest);
            try {
                b.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, out);
            } finally {
                out.close();
            }
            b.recycle();
            return true;
        } catch (Exception e) {
            return false;
        } catch (OutOfMemoryError e) {
            return false;
        } finally {
            tmp.delete();
        }
    }

    /** Decodes an image file so that its longest side is at most maxSize. Returns null on failure. */
    public static Bitmap decodeFile(String path, int maxSize) {
        if (path == null) {
            return null;
        }
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) {
                return null;
            }
            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxSize) {
                sample *= 2;
            }
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            d.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap b = BitmapFactory.decodeFile(path, d);
            if (b == null) {
                return null;
            }
            int big = Math.max(b.getWidth(), b.getHeight());
            if (big > maxSize) {
                float s = maxSize / (float) big;
                Bitmap sc = Bitmap.createScaledBitmap(b,
                        Math.max(1, Math.round(b.getWidth() * s)),
                        Math.max(1, Math.round(b.getHeight() * s)), true);
                if (sc != b) {
                    b.recycle();
                    b = sc;
                }
            }
            int rot = exifRotation(path);
            if (rot != 0) {
                Matrix m = new Matrix();
                m.postRotate(rot);
                Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (r != b) {
                    b.recycle();
                    b = r;
                }
            }
            return b;
        } catch (Throwable e) {
            return null;
        }
    }

    private static int exifRotation(String path) {
        try {
            ExifInterface ei = new ExifInterface(path);
            int o = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) {
                return 90;
            } else if (o == ExifInterface.ORIENTATION_ROTATE_180) {
                return 180;
            } else if (o == ExifInterface.ORIENTATION_ROTATE_270) {
                return 270;
            }
        } catch (Exception e) {
            // ignore: no EXIF data
        }
        return 0;
    }
}

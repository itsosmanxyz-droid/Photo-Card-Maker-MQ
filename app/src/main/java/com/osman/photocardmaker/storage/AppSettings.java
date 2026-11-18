package com.osman.photocardmaker.storage;

import android.content.Context;
import android.content.SharedPreferences;

/** Local app settings stored in SharedPreferences (no cloud, no network). */
public class AppSettings {

    private static final String PREFS = "photocardmaker_settings";
    private static final String KEY_SCALE = "export_scale";
    private static final String KEY_FORMAT = "export_format";
    private static final String KEY_LOGO = "default_logo";

    public static final String FORMAT_PNG = "png";
    public static final String FORMAT_JPG = "jpg";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Export scale multiplier: 1, 2 or 3. Default 2 (HD). */
    public int getExportScale() {
        int v = prefs.getInt(KEY_SCALE, 2);
        if (v < 1 || v > 3) {
            v = 2;
        }
        return v;
    }

    public void setExportScale(int scale) {
        prefs.edit().putInt(KEY_SCALE, scale).apply();
    }

    /** Path of the logo used by default in new cards, or null. */
    public String getDefaultLogo() {
        return prefs.getString(KEY_LOGO, null);
    }

    public void setDefaultLogo(String path) {
        if (path == null) {
            prefs.edit().remove(KEY_LOGO).apply();
        } else {
            prefs.edit().putString(KEY_LOGO, path).apply();
        }
    }

    public String getExportFormat() {
        return prefs.getString(KEY_FORMAT, FORMAT_PNG);
    }

    public void setExportFormat(String format) {
        prefs.edit().putString(KEY_FORMAT, format).apply();
    }
}

package com.osman.photocardmaker.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.HashSet;
import java.util.Set;

public class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("pcm_prefs", Context.MODE_PRIVATE);
    }

    public static int getTheme(Context c) {
        return sp(c).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setTheme(Context c, int mode) { sp(c).edit().putInt("theme", mode).apply(); }

    private static Set<String> set(Context c, String key) {
        return new HashSet<>(sp(c).getStringSet(key, new HashSet<String>()));
    }

    public static boolean isFavorite(Context c, String id) { return set(c, "fav_templates").contains(id); }

    public static void toggleFavorite(Context c, String id) {
        Set<String> s = set(c, "fav_templates");
        if (!s.remove(id)) s.add(id);
        sp(c).edit().putStringSet("fav_templates", s).apply();
    }

    public static Set<String> favorites(Context c) { return set(c, "fav_templates"); }

    public static boolean isUnlocked(Context c, String id) { return set(c, "unlocked").contains(id); }

    public static void unlock(Context c, String id) {
        Set<String> s = set(c, "unlocked");
        s.add(id);
        sp(c).edit().putStringSet("unlocked", s).apply();
    }

    public static int getInt(Context c, String k, int d) { return sp(c).getInt(k, d); }
    public static void putInt(Context c, String k, int v) { sp(c).edit().putInt(k, v).apply(); }
}

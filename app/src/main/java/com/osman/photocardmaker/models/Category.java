package com.osman.photocardmaker.models;

import android.content.Context;

import com.osman.photocardmaker.utils.Util;

public class Category {
    public String id = "";
    public String name = "";
    public String nameBn = "";
    public String icon = "🎨";
    public int color = 0xFF6C4DF6;
    public boolean enabled = true;

    public String getName(Context c) {
        return Util.isBn(c) && nameBn != null && !nameBn.isEmpty() ? nameBn : name;
    }
}

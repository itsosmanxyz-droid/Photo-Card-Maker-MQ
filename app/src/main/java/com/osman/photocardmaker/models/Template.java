package com.osman.photocardmaker.models;

import android.content.Context;

import com.osman.photocardmaker.utils.Util;

public class Template {
    public String id = "";
    public String name = "";
    public String nameBn = "";
    public String categoryId = "";
    public int width = 1080;
    public int height = 1080;
    public boolean enabled = true;
    public boolean featured, trending, premium, recommended;
    public boolean custom;
    public String thumbnailUrl = "";
    public Design design;

    public String getName(Context c) {
        return Util.isBn(c) && nameBn != null && !nameBn.isEmpty() ? nameBn : name;
    }
}

package com.osman.photocardmaker;

import android.app.Application;

import com.osman.photocardmaker.ads.AdManager;

/** Starts AdMob once, when the app process starts. */
public class PhotoCardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AdManager.init(this);
    }
}

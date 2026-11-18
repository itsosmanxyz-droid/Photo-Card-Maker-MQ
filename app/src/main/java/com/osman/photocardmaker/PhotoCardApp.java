package com.osman.photocardmaker;

import android.app.Application;

import com.osman.photocardmaker.ads.AdsManager;

/** Application class: prepares the ads manager (no network call happens here). */
public class PhotoCardApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AdsManager.init(this);
    }
}

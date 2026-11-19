package com.osman.photocardmaker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.osman.photocardmaker.templates.TemplateRepository;
import com.osman.photocardmaker.utils.Prefs;

public class PhotoCardApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getTheme(this));
        TemplateRepository.get().init(this);
    }
}

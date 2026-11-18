package com.osman.photocardmaker.storage;

import android.content.Context;

import java.io.File;

/**
 * Creates and exposes the app's private folders.
 * Everything lives in the app's internal storage, so no storage
 * permission is required and nothing leaves the device.
 */
public class LocalStorage {

    private final File root;

    public LocalStorage(Context context) {
        root = context.getApplicationContext().getFilesDir();
        ensure(getProjectsDir());
        ensure(getTemplatesDir());
        ensure(getLogosDir());
        ensure(getExportsDir());
        ensure(getPhotosDir());
        ensure(getFontsDir());
    }

    public File getRoot() {
        return root;
    }

    public File getProjectsDir() {
        return new File(root, "projects");
    }

    public File getTemplatesDir() {
        return new File(root, "templates");
    }

    public File getLogosDir() {
        return new File(root, "logos");
    }

    public File getExportsDir() {
        return new File(root, "exports");
    }

    public File getPhotosDir() {
        return new File(root, "photos");
    }

    public File getFontsDir() {
        return new File(root, "fonts");
    }

    private void ensure(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}

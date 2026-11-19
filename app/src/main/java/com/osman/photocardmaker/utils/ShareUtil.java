package com.osman.photocardmaker.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.storage.ExportUtil;
import com.osman.photocardmaker.storage.ProjectStore;

public class ShareUtil {
    public static void shareDesign(Activity a, Design d) {
        final Design snap = d.copy();
        Bg.run(() -> {
            try {
                float s = ExportUtil.safeScale(snap, 1f);
                Bitmap b = ExportUtil.render(snap, s, false);
                Uri uri = ExportUtil.cacheForShare(a, b);
                Bg.main(() -> {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("image/png");
                    i.putExtra(Intent.EXTRA_STREAM, uri);
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    a.startActivity(Intent.createChooser(i, a.getString(R.string.m_share)));
                });
            } catch (Throwable t) {
                Bg.main(() -> Util.toast(a, R.string.export_failed));
            }
        });
    }

    public static void shareProject(Activity a, String projectId) {
        Bg.run(() -> {
            try {
                Design d = ProjectStore.loadDesign(a, projectId);
                Bg.main(() -> shareDesign(a, d));
            } catch (Throwable t) {
                Bg.main(() -> Util.toast(a, R.string.load_failed));
            }
        });
    }
}

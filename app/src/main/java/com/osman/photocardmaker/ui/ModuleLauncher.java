package com.osman.photocardmaker.ui;

import android.content.Context;
import android.content.Intent;

import com.osman.photocardmaker.core.AppModule;
import com.osman.photocardmaker.templates.BlankTemplate;

import org.json.JSONException;

/**
 * Decides which screen a feature module opens. When a new module gets a real
 * screen, add one branch here; modules without a screen open the placeholder host.
 */
public final class ModuleLauncher {

    private ModuleLauncher() {
    }

    public static void open(Context context, AppModule module) {
        String id = module.getId();
        Intent i;
        if ("logo".equals(id)) {
            i = new Intent(context, LogoManagerActivity.class);
        } else if ("custom".equals(id)) {
            i = new Intent(context, EditorActivity.class);
            try {
                i.putExtra(EditorActivity.EXTRA_TEMPLATE,
                        BlankTemplate.create(context).toJson().toString());
            } catch (JSONException e) {
                return;
            }
        } else if ("templates".equals(id) || "projects".equals(id)) {
            i = new Intent(context, TemplateGalleryActivity.class);
            i.putExtra(TemplateGalleryActivity.EXTRA_MODULE_ID, id);
        } else {
            i = new Intent(context, ModuleHostActivity.class);
            i.putExtra(ModuleHostActivity.EXTRA_MODULE_ID, id);
        }
        context.startActivity(i);
    }

    /** Opens one template category of the library. */
    public static void openCategory(Context context, String folder) {
        Intent i = new Intent(context, TemplateGalleryActivity.class);
        i.putExtra(TemplateGalleryActivity.EXTRA_CATEGORY, folder);
        context.startActivity(i);
    }
}

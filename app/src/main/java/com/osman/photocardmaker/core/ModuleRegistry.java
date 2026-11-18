package com.osman.photocardmaker.core;

import com.osman.photocardmaker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Feature modules shown in the "Your space" section of the home screen.
 * Template categories are NOT listed here: they come from the assets/templates
 * folder (see LibraryLoader), so adding a template never touches this file.
 * Add a new feature module by calling register(...) in the static block.
 */
public final class ModuleRegistry {

    private static final List<AppModule> MODULES = new ArrayList<AppModule>();

    static {
        register(new BaseModule("custom", R.string.mod_custom_t, R.string.mod_custom_d,
                R.color.accent_purple, "C", 3, true));
        register(new BaseModule("projects", R.string.mod_projects_t, R.string.mod_projects_d,
                R.color.accent_blue, "J", 5, true));
        register(new BaseModule("templates", R.string.mod_template_t, R.string.mod_template_d,
                R.color.accent_brown, "M", 5, true));
        register(new BaseModule("logo", R.string.mod_logo_t, R.string.mod_logo_d,
                R.color.accent_green, "L", 4, true));
    }

    private ModuleRegistry() {
    }

    public static void register(AppModule module) {
        MODULES.add(module);
    }

    public static List<AppModule> getAll() {
        return Collections.unmodifiableList(MODULES);
    }

    public static AppModule findById(String id) {
        for (AppModule m : MODULES) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }
}

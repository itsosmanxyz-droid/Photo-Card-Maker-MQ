package com.osman.photocardmaker.core;

/**
 * Contract for every feature module of the app.
 * To add a new feature later: create a class implementing this interface
 * and register it in ModuleRegistry. No other file needs to change.
 */
public interface AppModule {

    /** Unique, stable id (used for intents and saved data). */
    String getId();

    /** String resource id for the module title. */
    int getTitleRes();

    /** String resource id for the short description. */
    int getDescRes();

    /** Color resource id used for the dashboard icon badge. */
    int getColorRes();

    /** One or two letters shown inside the icon badge. */
    String getIconText();

    /** Build step in which the module becomes usable. */
    int getStep();

    /** True when the module is implemented and can be opened. */
    boolean isReady();
}

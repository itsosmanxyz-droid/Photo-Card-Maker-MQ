package com.osman.photocardmaker.core;

/** Simple immutable implementation of AppModule. */
public class BaseModule implements AppModule {

    private final String id;
    private final int titleRes;
    private final int descRes;
    private final int colorRes;
    private final String iconText;
    private final int step;
    private final boolean ready;

    public BaseModule(String id, int titleRes, int descRes, int colorRes,
                      String iconText, int step, boolean ready) {
        this.id = id;
        this.titleRes = titleRes;
        this.descRes = descRes;
        this.colorRes = colorRes;
        this.iconText = iconText;
        this.step = step;
        this.ready = ready;
    }

    @Override public String getId() { return id; }
    @Override public int getTitleRes() { return titleRes; }
    @Override public int getDescRes() { return descRes; }
    @Override public int getColorRes() { return colorRes; }
    @Override public String getIconText() { return iconText; }
    @Override public int getStep() { return step; }
    @Override public boolean isReady() { return ready; }
}

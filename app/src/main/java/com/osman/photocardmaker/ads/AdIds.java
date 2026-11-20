package com.osman.photocardmaker.ads;

import java.util.ArrayList;
import java.util.List;

/**
 * All AdMob ad unit IDs in one place.
 *
 * USE_TEST_ADS = true  -> Google's official test ads (safe to click while developing).
 * USE_TEST_ADS = false -> your real ads.
 *
 * NEVER click your own real ads. To test safely with real IDs, add your phone's
 * test device ID to TEST_DEVICE_IDS (the ID is printed in Logcat the first time
 * an ad loads: "Use RequestConfiguration.Builder().setTestDeviceIds(...)").
 */
public final class AdIds {

    private AdIds() {
    }

    public static final boolean USE_TEST_ADS = false;

    /** Put your own phone's test device IDs here. */
    public static final List<String> TEST_DEVICE_IDS = new ArrayList<String>();

    // ---- real ad units ----
    private static final String REAL_APP_OPEN = "ca-app-pub-9414513035654232/9955121801";
    private static final String REAL_BANNER = "ca-app-pub-9414513035654232/9408326894";
    private static final String REAL_INTERSTITIAL = "ca-app-pub-9414513035654232/3964428529";
    private static final String REAL_NATIVE = "ca-app-pub-9414513035654232/6207448485";
    private static final String REAL_REWARDED = "ca-app-pub-9414513035654232/3640994893";
    private static final String REAL_REWARDED_INT = "ca-app-pub-9414513035654232/6399020177";

    // ---- Google test ad units ----
    private static final String TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    private static final String TEST_BANNER = "ca-app-pub-3940256099942544/6300978111";
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    private static final String TEST_REWARDED_INT = "ca-app-pub-3940256099942544/5354046379";

    public static String appOpen() {
        return USE_TEST_ADS ? TEST_APP_OPEN : REAL_APP_OPEN;
    }

    public static String banner() {
        return USE_TEST_ADS ? TEST_BANNER : REAL_BANNER;
    }

    public static String interstitial() {
        return USE_TEST_ADS ? TEST_INTERSTITIAL : REAL_INTERSTITIAL;
    }

    public static String nativeAd() {
        return USE_TEST_ADS ? TEST_NATIVE : REAL_NATIVE;
    }

    public static String rewarded() {
        return USE_TEST_ADS ? TEST_REWARDED : REAL_REWARDED;
    }

    public static String rewardedInterstitial() {
        return USE_TEST_ADS ? TEST_REWARDED_INT : REAL_REWARDED_INT;
    }
}

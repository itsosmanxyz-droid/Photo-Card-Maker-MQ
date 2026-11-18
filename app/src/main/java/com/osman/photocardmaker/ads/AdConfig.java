package com.osman.photocardmaker.ads;

/**
 * All AdMob settings in one place.
 *
 * IMPORTANT: keep TEST_MODE = true while you develop and test. Clicking or
 * loading LIVE ads on your own device can get the AdMob account suspended.
 * Set TEST_MODE = false only for the build you publish.
 */
public final class AdConfig {

    /** true = Google's official test ad units, false = your real ad units. */
    public static final boolean TEST_MODE = true;

    /**
     * true = when the export quality is "Full HD (3x)", offer a rewarded
     * interstitial video (the user can also choose normal HD for free).
     */
    public static final boolean GATE_FULL_HD = true;

    // ---- your AdMob app ID (also written in AndroidManifest.xml) ----
    // ca-app-pub-9414513035654232~5010303925

    // ---- your real ad units ----
    private static final String APP_OPEN = "ca-app-pub-9414513035654232/9955121801";
    private static final String BANNER = "ca-app-pub-9414513035654232/9408326894";
    private static final String INTERSTITIAL = "ca-app-pub-9414513035654232/3964428529";
    private static final String NATIVE = "ca-app-pub-9414513035654232/6207448485";
    private static final String REWARDED = "ca-app-pub-9414513035654232/3640994893";
    private static final String REWARDED_INTERSTITIAL = "ca-app-pub-9414513035654232/6399020177";

    // ---- Google's official test ad units ----
    private static final String T_APP_OPEN = "ca-app-pub-3940256099942544/9257395921";
    private static final String T_BANNER = "ca-app-pub-3940256099942544/9214589741";
    private static final String T_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String T_NATIVE = "ca-app-pub-3940256099942544/2247696110";
    private static final String T_REWARDED = "ca-app-pub-3940256099942544/5224354917";
    private static final String T_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379";

    // ---- how often ads may appear (keeps the app friendly and policy-safe) ----
    /** App open ads start only after the app was opened this many times. */
    public static final int APP_OPEN_MIN_LAUNCHES = 3;
    public static final long APP_OPEN_COOLDOWN_MS = 4L * 60L * 1000L;
    /** The app must have been in the background at least this long. */
    public static final long APP_OPEN_MIN_BACKGROUND_MS = 15L * 1000L;
    /** An interstitial may follow every Nth export. */
    public static final int INTERSTITIAL_EVERY_N_EXPORTS = 2;
    public static final long INTERSTITIAL_COOLDOWN_MS = 90L * 1000L;
    /** Reward for watching a rewarded video in Settings: no ads for this long. */
    public static final long AD_FREE_MS = 60L * 60L * 1000L;

    private AdConfig() {
    }

    public static String appOpen() { return TEST_MODE ? T_APP_OPEN : APP_OPEN; }
    public static String banner() { return TEST_MODE ? T_BANNER : BANNER; }
    public static String interstitial() { return TEST_MODE ? T_INTERSTITIAL : INTERSTITIAL; }
    public static String nativeAd() { return TEST_MODE ? T_NATIVE : NATIVE; }
    public static String rewarded() { return TEST_MODE ? T_REWARDED : REWARDED; }
    public static String rewardedInterstitial() {
        return TEST_MODE ? T_REWARDED_INTERSTITIAL : REWARDED_INTERSTITIAL;
    }
}

package com.osman.photocardmaker.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.osman.photocardmaker.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Single entry point for every ad in the app. Screens never touch the
 * Google Mobile Ads SDK directly, they only call the static methods below.
 *
 * Flow: consent (UMP) -> MobileAds.initialize -> preload full-screen ads.
 * Placements:
 *   App open ........ when the user returns to the app (not at first launch)
 *   Banner .......... bottom of the list screens (gallery, projects, logos)
 *   Native .......... home screen, between the two sections
 *   Interstitial .... after an export, when the user closes the "Saved" dialog
 *   Rewarded ........ Settings: watch a video for 1 hour without ads
 *   Rewarded inter. . Full HD export: optional video (free HD export is always possible)
 */
public final class AdsManager {

    public interface RewardCallback {
        /** rewarded = true when the user watched the video and earned the reward. */
        void onResult(boolean rewarded);
    }

    private static final String PREFS = "photocardmaker_ads";
    private static final String KEY_LAUNCHES = "launch_count";
    private static final String KEY_AD_FREE_UNTIL = "ad_free_until";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final List<Runnable> WHEN_READY = new ArrayList<Runnable>();

    private static Application app;
    private static SharedPreferences prefs;
    private static boolean started;
    private static boolean sdkInitializing;
    private static boolean sdkReady;

    private static AppOpenAd appOpenAd;
    private static long appOpenLoadTime;
    private static boolean appOpenLoading;
    private static boolean appOpenShowing;
    private static long lastAppOpenShown;

    private static InterstitialAd interstitial;
    private static boolean interstitialLoading;
    private static long lastInterstitialShown;
    private static int exportCount;

    private static RewardedAd rewarded;
    private static boolean rewardedLoading;

    private static RewardedInterstitialAd rewardedInterstitial;
    private static boolean rewardedInterstitialLoading;

    private static NativeAd currentNative;

    private static int startedActivities;
    private static boolean wasInBackground;
    private static long backgroundSince;
    private static long suppressAppOpenUntil;
    private static long lastFullScreenClosed;

    private AdsManager() {
    }

    // ------------------------------------------------------------------
    // Start-up
    // ------------------------------------------------------------------

    /** Called once from the Application class. Does not contact any ad server. */
    public static void init(Application application) {
        app = application;
        prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_LAUNCHES, prefs.getInt(KEY_LAUNCHES, 0) + 1).apply();

        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity activity) {
                if (startedActivities == 0 && wasInBackground) {
                    onReturnToForeground(activity);
                }
                startedActivities++;
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities--;
                if (startedActivities <= 0) {
                    startedActivities = 0;
                    wasInBackground = true;
                    backgroundSince = System.currentTimeMillis();
                }
            }

            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
    }

    /**
     * Called by the home screen. Asks for consent where the law requires it
     * (UMP), then initialises the Mobile Ads SDK and preloads ads.
     */
    public static void start(final Activity activity) {
        if (started) {
            return;
        }
        started = true;

        final ConsentInformation consent = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consent.requestConsentInfoUpdate(activity, params,
                new ConsentInformation.OnConsentInfoUpdateSuccessListener() {
                    @Override
                    public void onConsentInfoUpdateSuccess() {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,
                                new ConsentForm.OnConsentFormDismissedListener() {
                                    @Override
                                    public void onConsentFormDismissed(FormError error) {
                                        initIfAllowed(consent);
                                    }
                                });
                    }
                },
                new ConsentInformation.OnConsentInfoUpdateFailureListener() {
                    @Override
                    public void onConsentInfoUpdateFailure(FormError error) {
                        initIfAllowed(consent);
                    }
                });

        // consent gathered in an earlier session may already allow ad requests
        initIfAllowed(consent);
    }

    private static void initIfAllowed(ConsentInformation consent) {
        if (consent.canRequestAds()) {
            initSdk();
        }
    }

    private static void initSdk() {
        if (sdkInitializing || sdkReady || app == null) {
            return;
        }
        sdkInitializing = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                MobileAds.initialize(app, new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus status) {
                        MAIN.post(new Runnable() {
                            @Override
                            public void run() {
                                onSdkReady();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private static void onSdkReady() {
        sdkReady = true;
        List<Runnable> pending = new ArrayList<Runnable>(WHEN_READY);
        WHEN_READY.clear();
        for (Runnable r : pending) {
            r.run();
        }
        preloadAll();
    }

    private static void runWhenReady(Runnable r) {
        if (sdkReady) {
            r.run();
        } else {
            WHEN_READY.add(r);
        }
    }

    private static void preloadAll() {
        if (isAdFree()) {
            return;
        }
        loadAppOpen();
        loadInterstitial();
        loadRewarded();
        loadRewardedInterstitial();
    }

    private static AdRequest request() {
        return new AdRequest.Builder().build();
    }

    // ------------------------------------------------------------------
    // Ad-free reward
    // ------------------------------------------------------------------

    public static boolean isAdFree() {
        return prefs != null && prefs.getLong(KEY_AD_FREE_UNTIL, 0L) > System.currentTimeMillis();
    }

    public static long adFreeRemainingMs() {
        if (prefs == null) {
            return 0L;
        }
        return Math.max(0L, prefs.getLong(KEY_AD_FREE_UNTIL, 0L) - System.currentTimeMillis());
    }

    private static void grantAdFree() {
        if (prefs != null) {
            prefs.edit().putLong(KEY_AD_FREE_UNTIL,
                    System.currentTimeMillis() + AdConfig.AD_FREE_MS).apply();
        }
    }

    private static boolean adsAllowed() {
        return sdkReady && !isAdFree();
    }

    // ------------------------------------------------------------------
    // App open
    // ------------------------------------------------------------------

    /** Call before opening a picker / share sheet so returning from it shows no app open ad. */
    public static void suppressNextAppOpen() {
        suppressAppOpenUntil = System.currentTimeMillis() + 10L * 60L * 1000L;
    }

    private static boolean appOpenAvailable() {
        return appOpenAd != null
                && System.currentTimeMillis() - appOpenLoadTime < 4L * 3600L * 1000L;
    }

    private static void loadAppOpen() {
        if (!sdkReady || appOpenLoading || appOpenAvailable()) {
            return;
        }
        appOpenLoading = true;
        AppOpenAd.load(app, AdConfig.appOpen(), request(), new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(AppOpenAd ad) {
                appOpenAd = ad;
                appOpenLoadTime = System.currentTimeMillis();
                appOpenLoading = false;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                appOpenLoading = false;
            }
        });
    }

    private static void onReturnToForeground(Activity activity) {
        if (!adsAllowed() || appOpenShowing) {
            return;
        }
        if (activity.getClass().getName().startsWith("com.google.android.gms.ads")) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < suppressAppOpenUntil) {
            suppressAppOpenUntil = 0L;
            return;
        }
        if (now - backgroundSince < AdConfig.APP_OPEN_MIN_BACKGROUND_MS) {
            return;
        }
        if (prefs.getInt(KEY_LAUNCHES, 0) < AdConfig.APP_OPEN_MIN_LAUNCHES) {
            return;
        }
        if (now - lastAppOpenShown < AdConfig.APP_OPEN_COOLDOWN_MS
                || now - lastFullScreenClosed < 60L * 1000L) {
            return;
        }
        if (!appOpenAvailable()) {
            loadAppOpen();
            return;
        }
        appOpenShowing = true;
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                appOpenShowing = false;
                lastAppOpenShown = System.currentTimeMillis();
                lastFullScreenClosed = lastAppOpenShown;
                loadAppOpen();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                appOpenAd = null;
                appOpenShowing = false;
                loadAppOpen();
            }
        });
        appOpenAd.show(activity);
    }

    // ------------------------------------------------------------------
    // Banner
    // ------------------------------------------------------------------

    /** Puts an adaptive banner into the container. The container stays empty if no ad loads. */
    public static void attachBanner(final Activity activity, final FrameLayout container) {
        container.removeAllViews();
        if (isAdFree()) {
            return;
        }
        runWhenReady(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || isAdFree()) {
                    return;
                }
                final AdView view = new AdView(activity);
                view.setAdUnitId(AdConfig.banner());
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                int widthDp = (int) (dm.widthPixels / dm.density);
                view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        activity, widthDp));
                view.setAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        container.removeAllViews();
                        view.destroy();
                    }
                });
                container.removeAllViews();
                container.addView(view);
                view.loadAd(request());
            }
        });
    }

    public static void detachBanner(FrameLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View v = container.getChildAt(i);
            if (v instanceof AdView) {
                ((AdView) v).destroy();
            }
        }
        container.removeAllViews();
    }

    // ------------------------------------------------------------------
    // Native advanced
    // ------------------------------------------------------------------

    /** Loads a native ad into the slot (slot stays hidden until an ad is ready). */
    public static void loadNative(final Activity activity, final ViewGroup slot) {
        slot.setVisibility(View.GONE);
        if (isAdFree()) {
            return;
        }
        runWhenReady(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing() || isAdFree()) {
                    return;
                }
                AdLoader loader = new AdLoader.Builder(activity, AdConfig.nativeAd())
                        .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                            @Override
                            public void onNativeAdLoaded(NativeAd ad) {
                                if (activity.isFinishing()) {
                                    ad.destroy();
                                    return;
                                }
                                if (currentNative != null) {
                                    currentNative.destroy();
                                }
                                currentNative = ad;
                                bindNative(activity, slot, ad);
                            }
                        })
                        .withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError error) {
                                slot.setVisibility(View.GONE);
                            }
                        })
                        .withNativeAdOptions(new NativeAdOptions.Builder().build())
                        .build();
                loader.loadAd(request());
            }
        });
    }

    private static void bindNative(Activity activity, ViewGroup slot, NativeAd ad) {
        NativeAdView root = (NativeAdView) activity.getLayoutInflater()
                .inflate(R.layout.native_ad_card, slot, false);

        root.setHeadlineView(root.findViewById(R.id.ad_headline));
        root.setBodyView(root.findViewById(R.id.ad_body));
        root.setCallToActionView(root.findViewById(R.id.ad_cta));
        root.setIconView(root.findViewById(R.id.ad_icon));
        root.setMediaView((MediaView) root.findViewById(R.id.ad_media));

        ((TextView) root.getHeadlineView()).setText(ad.getHeadline());

        if (ad.getBody() == null) {
            root.getBodyView().setVisibility(View.GONE);
        } else {
            root.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) root.getBodyView()).setText(ad.getBody());
        }
        if (ad.getCallToAction() == null) {
            root.getCallToActionView().setVisibility(View.GONE);
        } else {
            root.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) root.getCallToActionView()).setText(ad.getCallToAction());
        }
        if (ad.getIcon() == null) {
            root.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) root.getIconView()).setImageDrawable(ad.getIcon().getDrawable());
            root.getIconView().setVisibility(View.VISIBLE);
        }
        if (ad.getMediaContent() != null) {
            root.getMediaView().setMediaContent(ad.getMediaContent());
        }

        root.setNativeAd(ad);
        slot.removeAllViews();
        slot.addView(root);
        slot.setVisibility(View.VISIBLE);
    }

    public static void destroyNative() {
        if (currentNative != null) {
            currentNative.destroy();
            currentNative = null;
        }
    }

    // ------------------------------------------------------------------
    // Interstitial (after export)
    // ------------------------------------------------------------------

    private static void loadInterstitial() {
        if (!sdkReady || interstitialLoading || interstitial != null) {
            return;
        }
        interstitialLoading = true;
        InterstitialAd.load(app, AdConfig.interstitial(), request(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitial = ad;
                        interstitialLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        interstitial = null;
                        interstitialLoading = false;
                    }
                });
    }

    /**
     * Call when an export finished and the user closed the "Saved" dialog.
     * Shows an interstitial on every Nth export (with a cool-down), then runs 'afterwards'.
     */
    public static void showInterstitialAfterExport(Activity activity, final Runnable afterwards) {
        exportCount++;
        long now = System.currentTimeMillis();
        boolean due = exportCount % AdConfig.INTERSTITIAL_EVERY_N_EXPORTS == 0;
        if (!adsAllowed() || !due || interstitial == null
                || now - lastInterstitialShown < AdConfig.INTERSTITIAL_COOLDOWN_MS) {
            loadInterstitial();
            if (afterwards != null) {
                afterwards.run();
            }
            return;
        }
        interstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitial = null;
                lastInterstitialShown = System.currentTimeMillis();
                lastFullScreenClosed = lastInterstitialShown;
                loadInterstitial();
                if (afterwards != null) {
                    afterwards.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                interstitial = null;
                loadInterstitial();
                if (afterwards != null) {
                    afterwards.run();
                }
            }
        });
        interstitial.show(activity);
    }

    // ------------------------------------------------------------------
    // Rewarded (Settings: 1 hour without ads)
    // ------------------------------------------------------------------

    private static void loadRewarded() {
        if (!sdkReady || rewardedLoading || rewarded != null) {
            return;
        }
        rewardedLoading = true;
        RewardedAd.load(app, AdConfig.rewarded(), request(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewarded = ad;
                rewardedLoading = false;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                rewarded = null;
                rewardedLoading = false;
            }
        });
    }

    public static boolean isRewardedReady() {
        return sdkReady && rewarded != null;
    }

    /** Shows the rewarded video. On reward the user gets AdConfig.AD_FREE_MS without ads. */
    public static void showRewarded(Activity activity, final RewardCallback callback) {
        if (!isRewardedReady()) {
            loadRewarded();
            callback.onResult(false);
            return;
        }
        final boolean[] earned = {false};
        rewarded.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewarded = null;
                lastFullScreenClosed = System.currentTimeMillis();
                if (earned[0]) {
                    grantAdFree();
                }
                loadRewarded();
                callback.onResult(earned[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                rewarded = null;
                loadRewarded();
                callback.onResult(false);
            }
        });
        rewarded.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(RewardItem item) {
                earned[0] = true;
            }
        });
    }

    // ------------------------------------------------------------------
    // Rewarded interstitial (optional video for Full HD export)
    // ------------------------------------------------------------------

    private static void loadRewardedInterstitial() {
        if (!sdkReady || rewardedInterstitialLoading || rewardedInterstitial != null) {
            return;
        }
        rewardedInterstitialLoading = true;
        RewardedInterstitialAd.load(app, AdConfig.rewardedInterstitial(), request(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        rewardedInterstitial = ad;
                        rewardedInterstitialLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        rewardedInterstitial = null;
                        rewardedInterstitialLoading = false;
                    }
                });
    }

    /** True when a video is loaded and ads are allowed, so the app can offer it. */
    public static boolean canOfferRewardedInterstitial() {
        return adsAllowed() && rewardedInterstitial != null;
    }

    /**
     * Shows the rewarded interstitial. The caller MUST show an intro dialog first
     * that explains the reward and lets the user say no (AdMob policy).
     */
    public static void showRewardedInterstitial(Activity activity, final RewardCallback callback) {
        if (rewardedInterstitial == null) {
            loadRewardedInterstitial();
            callback.onResult(false);
            return;
        }
        final boolean[] earned = {false};
        rewardedInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedInterstitial = null;
                lastFullScreenClosed = System.currentTimeMillis();
                loadRewardedInterstitial();
                callback.onResult(earned[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                rewardedInterstitial = null;
                loadRewardedInterstitial();
                callback.onResult(false);
            }
        });
        rewardedInterstitial.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(RewardItem item) {
                earned[0] = true;
            }
        });
    }

    // ------------------------------------------------------------------
    // Privacy options (required by the consent rules in some regions)
    // ------------------------------------------------------------------

    public static boolean isPrivacyOptionsRequired() {
        if (app == null) {
            return false;
        }
        return UserMessagingPlatform.getConsentInformation(app).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity activity, final Runnable afterwards) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity,
                new ConsentForm.OnConsentFormDismissedListener() {
                    @Override
                    public void onConsentFormDismissed(FormError error) {
                        if (afterwards != null) {
                            afterwards.run();
                        }
                    }
                });
    }
}

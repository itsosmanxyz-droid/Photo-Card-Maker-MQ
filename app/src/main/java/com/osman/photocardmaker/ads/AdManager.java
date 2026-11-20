package com.osman.photocardmaker.ads;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.osman.photocardmaker.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Central place for every ad in the app.
 *
 *  - Banner ........ attachBanner()      (Home, Templates list, Settings)
 *  - Native ........ loadNative()        (Home dashboard)
 *  - Interstitial .. afterExport()       (after saving a card)
 *  - Rewarded ...... showRewarded()      (Settings: unlock HD export for 24 hours)
 *  - Rewarded int. . afterExport()       (every 3rd export, optional, can be skipped)
 *  - App open ...... AppOpenManager      (when the user comes back to the app)
 */
public final class AdManager {

    /**
     * true  = 3x (HD) export needs a rewarded video first (unlocked for 24 hours).
     * false = 3x export is always free.
     */
    public static final boolean GATE_HD_EXPORT = true;

    private static final long HD_UNLOCK_MS = 24L * 60L * 60L * 1000L;
    private static final long INTERSTITIAL_GAP_MS = 60L * 1000L;
    private static final long RETRY_MS = 60L * 1000L;
    private static final String PREFS = "ads_prefs";
    private static final String KEY_HD_UNTIL = "hd_unlock_until";
    private static final String KEY_EXPORTS = "export_count";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static Application app;
    private static boolean initialized;
    private static boolean sdkReady;
    private static boolean fullScreenShowing;
    private static long lastInterstitialTime;

    private static InterstitialAd interstitial;
    private static boolean loadingInterstitial;
    private static RewardedAd rewarded;
    private static boolean loadingRewarded;
    private static RewardedInterstitialAd rewardedInterstitial;
    private static boolean loadingRewardedInterstitial;

    private static AppOpenManager appOpenManager;

    private AdManager() {
    }

    /** Result of a rewarded ad. */
    public interface RewardCallback {
        void onResult(boolean earned);
    }

    // ------------------------------------------------------------------ init

    public static void init(Application application) {
        if (initialized) {
            return;
        }
        initialized = true;
        app = application;

        List<String> testIds = new ArrayList<String>();
        testIds.add(AdRequest.DEVICE_ID_EMULATOR);
        testIds.addAll(AdIds.TEST_DEVICE_IDS);
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(testIds).build());

        appOpenManager = new AppOpenManager(application);
        application.registerActivityLifecycleCallbacks(appOpenManager);

        MobileAds.initialize(application, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus status) {
                sdkReady = true;
                loadInterstitial();
                loadRewarded();
                loadRewardedInterstitial();
                appOpenManager.load();
            }
        });
    }

    static boolean isSdkReady() {
        return sdkReady;
    }

    static boolean isFullScreenAdShowing() {
        return fullScreenShowing;
    }

    static void setFullScreenAdShowing(boolean showing) {
        fullScreenShowing = showing;
    }

    /** Call before opening a picker or share sheet so the app-open ad does not pop up on return. */
    public static void suppressAppOpenOnce() {
        if (appOpenManager != null) {
            appOpenManager.suppressNext();
        }
    }

    static boolean isOnline() {
        if (app == null) {
            return false;
        }
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm == null ? null : cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable e) {
            return true;
        }
    }

    private static AdRequest request() {
        return new AdRequest.Builder().build();
    }

    // ---------------------------------------------------------------- banner

    /**
     * Adds an adaptive banner to the container. The container stays hidden
     * until an ad has actually loaded, so there is no blank gap.
     * Call destroy() on the returned view in the Activity's onDestroy().
     */
    public static AdView attachBanner(Activity activity, final ViewGroup container) {
        if (container == null) {
            return null;
        }
        final AdView adView = new AdView(activity);
        adView.setAdUnitId(AdIds.banner());
        adView.setAdSize(adaptiveSize(activity));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                container.setVisibility(View.GONE);
            }
        });
        container.removeAllViews();
        container.addView(adView);
        container.setVisibility(View.GONE);
        if (isOnline()) {
            adView.loadAd(request());
        }
        return adView;
    }

    private static AdSize adaptiveSize(Activity activity) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int widthDp = (int) (dm.widthPixels / dm.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp);
    }

    // ---------------------------------------------------------------- native

    /**
     * Loads one native ad into the container. The container stays hidden
     * until the ad has loaded. Returns the loaded ad through holder[0] so the
     * Activity can destroy it in onDestroy().
     */
    public static void loadNative(final Activity activity, final ViewGroup container,
                                  final NativeAd[] holder) {
        if (container == null || !isOnline()) {
            return;
        }
        container.setVisibility(View.GONE);
        AdLoader loader = new AdLoader.Builder(activity, AdIds.nativeAd())
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        if (activity.isFinishing()) {
                            nativeAd.destroy();
                            return;
                        }
                        if (holder != null) {
                            if (holder[0] != null) {
                                holder[0].destroy();
                            }
                            holder[0] = nativeAd;
                        }
                        NativeAdView view = (NativeAdView) LayoutInflater.from(activity)
                                .inflate(R.layout.ad_native, container, false);
                        populateNative(nativeAd, view);
                        container.removeAllViews();
                        container.addView(view);
                        container.setVisibility(View.VISIBLE);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        container.setVisibility(View.GONE);
                    }
                })
                .build();
        loader.loadAd(request());
    }

    private static void populateNative(NativeAd ad, NativeAdView view) {
        MediaView media = (MediaView) view.findViewById(R.id.adMedia);
        view.setMediaView(media);

        TextView headline = (TextView) view.findViewById(R.id.adHeadline);
        headline.setText(ad.getHeadline());
        view.setHeadlineView(headline);

        TextView body = (TextView) view.findViewById(R.id.adBody);
        if (ad.getBody() == null) {
            body.setVisibility(View.GONE);
        } else {
            body.setVisibility(View.VISIBLE);
            body.setText(ad.getBody());
        }
        view.setBodyView(body);

        Button cta = (Button) view.findViewById(R.id.adCta);
        if (ad.getCallToAction() == null) {
            cta.setVisibility(View.GONE);
        } else {
            cta.setVisibility(View.VISIBLE);
            cta.setText(ad.getCallToAction());
        }
        view.setCallToActionView(cta);

        ImageView icon = (ImageView) view.findViewById(R.id.adIcon);
        if (ad.getIcon() == null) {
            icon.setVisibility(View.GONE);
        } else {
            icon.setImageDrawable(ad.getIcon().getDrawable());
            icon.setVisibility(View.VISIBLE);
        }
        view.setIconView(icon);

        TextView advertiser = (TextView) view.findViewById(R.id.adAdvertiser);
        if (ad.getAdvertiser() == null) {
            advertiser.setVisibility(View.GONE);
        } else {
            advertiser.setText(ad.getAdvertiser());
            advertiser.setVisibility(View.VISIBLE);
        }
        view.setAdvertiserView(advertiser);

        if (ad.getMediaContent() != null) {
            media.setMediaContent(ad.getMediaContent());
        }
        view.setNativeAd(ad);
    }

    // ---------------------------------------------------------- interstitial

    private static void loadInterstitial() {
        if (!sdkReady || app == null || loadingInterstitial || interstitial != null) {
            return;
        }
        if (!isOnline()) {
            return;
        }
        loadingInterstitial = true;
        InterstitialAd.load(app, AdIds.interstitial(), request(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        loadingInterstitial = false;
                        interstitial = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        loadingInterstitial = false;
                        interstitial = null;
                        MAIN.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadInterstitial();
                            }
                        }, RETRY_MS);
                    }
                });
    }

    private static void showInterstitial(Activity activity, final Runnable after) {
        final InterstitialAd ad = interstitial;
        if (ad == null) {
            loadInterstitial();
            after.run();
            return;
        }
        interstitial = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                fullScreenShowing = false;
                lastInterstitialTime = System.currentTimeMillis();
                loadInterstitial();
                after.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                fullScreenShowing = false;
                loadInterstitial();
                after.run();
            }
        });
        fullScreenShowing = true;
        ad.show(activity);
    }

    // --------------------------------------------------------------- rewarded

    private static void loadRewarded() {
        if (!sdkReady || app == null || loadingRewarded || rewarded != null) {
            return;
        }
        if (!isOnline()) {
            return;
        }
        loadingRewarded = true;
        RewardedAd.load(app, AdIds.rewarded(), request(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                loadingRewarded = false;
                rewarded = ad;
            }

            @Override
            public void onAdFailedToLoad(LoadAdError error) {
                loadingRewarded = false;
                rewarded = null;
                MAIN.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadRewarded();
                    }
                }, RETRY_MS);
            }
        });
    }

    public static boolean isRewardedReady() {
        return rewarded != null;
    }

    /**
     * Shows a rewarded video. Returns false (and shows nothing) when no ad is
     * ready yet. The callback gets true only if the user earned the reward.
     */
    public static boolean showRewarded(Activity activity, final RewardCallback cb) {
        final RewardedAd ad = rewarded;
        if (ad == null) {
            loadRewarded();
            return false;
        }
        rewarded = null;
        final boolean[] earned = new boolean[1];
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                fullScreenShowing = false;
                loadRewarded();
                cb.onResult(earned[0]);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                fullScreenShowing = false;
                loadRewarded();
                cb.onResult(false);
            }
        });
        fullScreenShowing = true;
        ad.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(RewardItem item) {
                earned[0] = true;
            }
        });
        return true;
    }

    // ------------------------------------------------------ rewarded interstitial

    private static void loadRewardedInterstitial() {
        if (!sdkReady || app == null || loadingRewardedInterstitial
                || rewardedInterstitial != null) {
            return;
        }
        if (!isOnline()) {
            return;
        }
        loadingRewardedInterstitial = true;
        RewardedInterstitialAd.load(app, AdIds.rewardedInterstitial(), request(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        loadingRewardedInterstitial = false;
                        rewardedInterstitial = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        loadingRewardedInterstitial = false;
                        rewardedInterstitial = null;
                        MAIN.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadRewardedInterstitial();
                            }
                        }, RETRY_MS);
                    }
                });
    }

    private static void showRewardedInterstitial(Activity activity, final Runnable after) {
        final RewardedInterstitialAd ad = rewardedInterstitial;
        if (ad == null) {
            loadRewardedInterstitial();
            after.run();
            return;
        }
        rewardedInterstitial = null;
        final Context ctx = activity.getApplicationContext();
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                fullScreenShowing = false;
                loadRewardedInterstitial();
                after.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                fullScreenShowing = false;
                loadRewardedInterstitial();
                after.run();
            }
        });
        fullScreenShowing = true;
        ad.show(activity, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(RewardItem item) {
                unlockHd(ctx);
            }
        });
    }

    // ---------------------------------------------------------- export flow

    /**
     * Call after a card was exported successfully. Decides which ad (if any)
     * to show, then always runs "after" (shows the "Saved" dialog).
     *
     *  - 1st export ................ no ad
     *  - every 3rd export .......... rewarded interstitial (opt-in dialog, can be skipped)
     *  - other exports ............. interstitial (at most one per minute)
     */
    public static void afterExport(final Activity activity, final Runnable after) {
        SharedPreferences sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int count = sp.getInt(KEY_EXPORTS, 0) + 1;
        sp.edit().putInt(KEY_EXPORTS, count).commit();

        if (count < 2 || !isOnline()) {
            after.run();
            return;
        }

        if (count % 3 == 0 && rewardedInterstitial != null && GATE_HD_EXPORT
                && !isHdUnlocked(activity)) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.ad_ri_title)
                    .setMessage(R.string.ad_ri_message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.ad_no_thanks,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    after.run();
                                }
                            })
                    .setPositiveButton(R.string.ad_watch,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    showRewardedInterstitial(activity, after);
                                }
                            })
                    .show();
            return;
        }

        long now = System.currentTimeMillis();
        if (interstitial != null && now - lastInterstitialTime >= INTERSTITIAL_GAP_MS) {
            showInterstitial(activity, after);
        } else {
            after.run();
        }
    }

    // ------------------------------------------------------------ HD unlock

    public static boolean isHdUnlocked(Context c) {
        if (!GATE_HD_EXPORT) {
            return true;
        }
        long until = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_HD_UNTIL, 0L);
        return System.currentTimeMillis() < until;
    }

    public static void unlockHd(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_HD_UNTIL, System.currentTimeMillis() + HD_UNLOCK_MS).commit();
    }

    /** Export scale that is actually allowed right now (3x falls back to 2x when locked). */
    public static int effectiveScale(Context c, int wanted) {
        if (wanted >= 3 && !isHdUnlocked(c)) {
            return 2;
        }
        return wanted;
    }
}

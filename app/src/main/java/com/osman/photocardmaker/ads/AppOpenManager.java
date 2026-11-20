package com.osman.photocardmaker.ads;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.osman.photocardmaker.ui.EditorActivity;

/**
 * Shows an app-open ad when the user comes back to the app after being away.
 * It is NOT shown:
 *  - on a cold start (the ad is loaded in the background for next time),
 *  - when the user was away for less than 30 seconds,
 *  - more than once every 3 minutes,
 *  - inside the editor (the user is in the middle of work),
 *  - right after a picker / share sheet / another ad.
 */
final class AppOpenManager implements Application.ActivityLifecycleCallbacks {

    private static final long AD_EXPIRY_MS = 4L * 60L * 60L * 1000L;
    private static final long MIN_AWAY_MS = 30L * 1000L;
    private static final long MIN_GAP_MS = 3L * 60L * 1000L;
    private static final String ADS_ACTIVITY = "com.google.android.gms.ads.AdActivity";

    private final Application app;
    private AppOpenAd ad;
    private long loadedAt;
    private boolean loading;

    private int startedCount;
    private long stoppedAt;
    private long lastShown;
    private boolean suppress;

    AppOpenManager(Application app) {
        this.app = app;
    }

    void suppressNext() {
        suppress = true;
    }

    void load() {
        if (!AdManager.isSdkReady() || loading || isAdAvailable() || !AdManager.isOnline()) {
            return;
        }
        loading = true;
        AppOpenAd.load(app, AdIds.appOpen(), new AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd loadedAd) {
                        loading = false;
                        ad = loadedAd;
                        loadedAt = System.currentTimeMillis();
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError error) {
                        loading = false;
                    }
                });
    }

    private boolean isAdAvailable() {
        return ad != null && System.currentTimeMillis() - loadedAt < AD_EXPIRY_MS;
    }

    private void showIfAvailable(Activity activity) {
        if (AdManager.isFullScreenAdShowing()) {
            return;
        }
        if (!isAdAvailable()) {
            ad = null;
            load();
            return;
        }
        if (System.currentTimeMillis() - lastShown < MIN_GAP_MS) {
            return;
        }
        final AppOpenAd toShow = ad;
        ad = null;
        toShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                lastShown = System.currentTimeMillis();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AdManager.setFullScreenAdShowing(false);
                load();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                AdManager.setFullScreenAdShowing(false);
                load();
            }
        });
        AdManager.setFullScreenAdShowing(true);
        toShow.show(activity);
    }

    private static boolean isAdsActivity(Activity a) {
        return ADS_ACTIVITY.equals(a.getClass().getName());
    }

    // ------------------------------------------------------ lifecycle

    @Override
    public void onActivityStarted(Activity activity) {
        if (isAdsActivity(activity)) {
            return;
        }
        startedCount++;
        if (startedCount == 1) {
            long away = stoppedAt == 0 ? 0 : System.currentTimeMillis() - stoppedAt;
            boolean comingBack = stoppedAt != 0 && away >= MIN_AWAY_MS;
            stoppedAt = 0;
            if (suppress) {
                suppress = false;
                return;
            }
            if (comingBack && !(activity instanceof EditorActivity)) {
                showIfAvailable(activity);
            } else if (!isAdAvailable()) {
                load();
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (isAdsActivity(activity)) {
            return;
        }
        startedCount--;
        if (startedCount <= 0) {
            startedCount = 0;
            if (activity.isChangingConfigurations() || AdManager.isFullScreenAdShowing()) {
                stoppedAt = 0;
            } else {
                stoppedAt = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }
}

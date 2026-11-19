package com.osman.photocardmaker.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.osman.photocardmaker.BuildConfig;
import com.osman.photocardmaker.utils.Prefs;

import java.util.ArrayList;
import java.util.List;

/** AdMob: consent (UMP) -> SDK init -> adaptive banner, capped interstitial, rewarded. */
public class AdManager {
    private static boolean started, ready;
    private static InterstitialAd interstitial;
    private static RewardedAd rewarded;
    private static long lastInterstitial = 0;
    private static final List<Runnable> WAITING = new ArrayList<>();

    public static void init(Activity a) {
        final ConsentInformation ci = UserMessagingPlatform.getConsentInformation(a);
        ci.requestConsentInfoUpdate(a, new ConsentRequestParameters.Builder().build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(a, err -> {
                    if (ci.canRequestAds()) startSdk(a);
                }),
                err -> { if (ci.canRequestAds()) startSdk(a); });
        if (ci.canRequestAds()) startSdk(a);
    }

    private static synchronized void startSdk(Context c) {
        if (started) return;
        started = true;
        final Context app = c.getApplicationContext();
        MobileAds.initialize(app, status -> {
            ready = true;
            loadInterstitial(app);
            loadRewarded(app);
            List<Runnable> run;
            synchronized (AdManager.class) { run = new ArrayList<>(WAITING); WAITING.clear(); }
            for (Runnable r : run) r.run();
        });
    }

    private static void whenReady(Runnable r) {
        if (ready) { r.run(); return; }
        synchronized (AdManager.class) { WAITING.add(r); }
    }

    // ---- Banner (Home only) ---------------------------------------------
    public static void showBanner(final Activity a, final FrameLayout holder) {
        whenReady(() -> a.runOnUiThread(() -> {
            if (a.isDestroyed() || a.isFinishing() || holder.getChildCount() > 0) return;
            DisplayMetrics dm = a.getResources().getDisplayMetrics();
            int wDp = (int) ((holder.getWidth() > 0 ? holder.getWidth() : dm.widthPixels) / dm.density);
            AdView v = new AdView(a);
            v.setAdUnitId(BuildConfig.ADMOB_BANNER);
            v.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(a, wDp));
            holder.addView(v);
            v.loadAd(new AdRequest.Builder().build());
        }));
    }

    // ---- Interstitial (capped: every 3rd editor open, min 90 s apart) -----
    private static void loadInterstitial(Context c) {
        InterstitialAd.load(c, BuildConfig.ADMOB_INTERSTITIAL, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(@NonNull InterstitialAd ad) { interstitial = ad; }
                    @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { interstitial = null; }
                });
    }

    public static void maybeShowInterstitial(final Activity a, final Runnable next) {
        int count = Prefs.getInt(a, "ad_open_count", 0) + 1;
        Prefs.putInt(a, "ad_open_count", count);
        long now = System.currentTimeMillis();
        final InterstitialAd ad = interstitial;
        if (ad == null || count % 3 != 0 || now - lastInterstitial < 90_000L) { next.run(); return; }
        interstitial = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            private boolean done;
            private void finish() {
                if (done) return;
                done = true;
                lastInterstitial = System.currentTimeMillis();
                loadInterstitial(a.getApplicationContext());
                next.run();
            }
            @Override public void onAdDismissedFullScreenContent() { finish(); }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) { finish(); }
        });
        ad.show(a);
    }

    // ---- Rewarded (premium unlock) ----------------------------------------
    private static void loadRewarded(Context c) {
        RewardedAd.load(c, BuildConfig.ADMOB_REWARDED, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override public void onAdLoaded(@NonNull RewardedAd ad) { rewarded = ad; }
                    @Override public void onAdFailedToLoad(@NonNull LoadAdError e) { rewarded = null; }
                });
    }

    public static void showRewarded(final Activity a, final Runnable onReward, final Runnable onUnavailable) {
        final RewardedAd ad = rewarded;
        if (ad == null) {
            if (ready) loadRewarded(a.getApplicationContext());
            onUnavailable.run();
            return;
        }
        rewarded = null;
        final boolean[] earned = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() {
                loadRewarded(a.getApplicationContext());
                if (earned[0]) onReward.run();
            }
            @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError e) {
                loadRewarded(a.getApplicationContext());
                onUnavailable.run();
            }
        });
        ad.show(a, item -> earned[0] = true);
    }

    // ---- Consent privacy options (Settings) -------------------------------
    public static boolean isPrivacyOptionsRequired(Activity a) {
        return UserMessagingPlatform.getConsentInformation(a).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity a) {
        UserMessagingPlatform.showPrivacyOptionsForm(a, err -> { });
    }
}

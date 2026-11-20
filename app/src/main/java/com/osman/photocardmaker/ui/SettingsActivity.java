package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdView;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.storage.AppSettings;
import com.osman.photocardmaker.storage.LocalStorage;

/** Export quality / format settings, saved locally. */
public class SettingsActivity extends Activity {

    private AppSettings settings;
    private AdView bannerView;
    private RadioGroup rgScaleGroup;
    private boolean revertingScale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = new AppSettings(this);

        findViewById(R.id.btnSettingsBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final RadioGroup rgScale = (RadioGroup) findViewById(R.id.rgScale);
        rgScaleGroup = rgScale;
        RadioGroup rgFormat = (RadioGroup) findViewById(R.id.rgFormat);

        int scale = AdManager.effectiveScale(this, settings.getExportScale());
        if (scale == 1) {
            rgScale.check(R.id.rbScale1);
        } else if (scale == 3) {
            rgScale.check(R.id.rbScale3);
        } else {
            rgScale.check(R.id.rbScale2);
        }

        if (AppSettings.FORMAT_JPG.equals(settings.getExportFormat())) {
            rgFormat.check(R.id.rbJpg);
        } else {
            rgFormat.check(R.id.rbPng);
        }

        rgScale.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (revertingScale) {
                    return;
                }
                if (checkedId == R.id.rbScale1) {
                    settings.setExportScale(1);
                } else if (checkedId == R.id.rbScale3) {
                    if (AdManager.isHdUnlocked(SettingsActivity.this)) {
                        settings.setExportScale(3);
                    } else {
                        askToUnlockHd();
                    }
                } else {
                    settings.setExportScale(2);
                }
            }
        });

        rgFormat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbJpg) {
                    settings.setExportFormat(AppSettings.FORMAT_JPG);
                } else {
                    settings.setExportFormat(AppSettings.FORMAT_PNG);
                }
            }
        });

        TextView path = (TextView) findViewById(R.id.tvStoragePath);
        path.setText(new LocalStorage(this).getRoot().getAbsolutePath());

        bannerView = AdManager.attachBanner(this, (ViewGroup) findViewById(R.id.adBanner));
    }

    /** 3x was tapped but is locked: go back to the saved size and offer the rewarded video. */
    private void askToUnlockHd() {
        selectScale(AdManager.effectiveScale(this, settings.getExportScale()));
        new AlertDialog.Builder(this)
                .setTitle(R.string.ad_hd_title)
                .setMessage(R.string.ad_hd_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ad_watch, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        boolean started = AdManager.showRewarded(SettingsActivity.this,
                                new AdManager.RewardCallback() {
                                    @Override
                                    public void onResult(boolean earned) {
                                        if (earned) {
                                            AdManager.unlockHd(SettingsActivity.this);
                                            settings.setExportScale(3);
                                            selectScale(3);
                                            Toast.makeText(SettingsActivity.this,
                                                    R.string.ad_hd_unlocked,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(SettingsActivity.this,
                                                    R.string.ad_hd_not_earned,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        if (!started) {
                            Toast.makeText(SettingsActivity.this, R.string.ad_not_ready,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .show();
    }

    private void selectScale(int scale) {
        revertingScale = true;
        if (scale == 1) {
            rgScaleGroup.check(R.id.rbScale1);
        } else if (scale == 3) {
            rgScaleGroup.check(R.id.rbScale3);
        } else {
            rgScaleGroup.check(R.id.rbScale2);
        }
        revertingScale = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerView != null) {
            bannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        if (bannerView != null) {
            bannerView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bannerView != null) {
            bannerView.destroy();
        }
        super.onDestroy();
    }
}

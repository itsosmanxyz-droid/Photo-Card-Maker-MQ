package com.osman.photocardmaker.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdsManager;
import com.osman.photocardmaker.storage.AppSettings;
import com.osman.photocardmaker.storage.LocalStorage;

/** Export quality / format settings, saved locally. */
public class SettingsActivity extends Activity {

    private AppSettings settings;

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

        RadioGroup rgScale = (RadioGroup) findViewById(R.id.rgScale);
        RadioGroup rgFormat = (RadioGroup) findViewById(R.id.rgFormat);

        int scale = settings.getExportScale();
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
                if (checkedId == R.id.rbScale1) {
                    settings.setExportScale(1);
                } else if (checkedId == R.id.rbScale3) {
                    settings.setExportScale(3);
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

        findViewById(R.id.btnWatchAd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdsManager.showRewarded(SettingsActivity.this, new AdsManager.RewardCallback() {
                    @Override
                    public void onResult(boolean rewarded) {
                        if (rewarded) {
                            Toast.makeText(SettingsActivity.this, R.string.ads_reward_granted,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SettingsActivity.this, R.string.ads_not_ready,
                                    Toast.LENGTH_SHORT).show();
                        }
                        refreshAdStatus();
                    }
                });
            }
        });

        View privacy = findViewById(R.id.btnPrivacy);
        if (AdsManager.isPrivacyOptionsRequired()) {
            privacy.setVisibility(View.VISIBLE);
            privacy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AdsManager.showPrivacyOptions(SettingsActivity.this, null);
                }
            });
        }
        refreshAdStatus();
    }

    private void refreshAdStatus() {
        TextView status = (TextView) findViewById(R.id.tvAdStatus);
        if (AdsManager.isAdFree()) {
            long minutes = (AdsManager.adFreeRemainingMs() + 59999L) / 60000L;
            status.setText(getString(R.string.ads_status_free, (int) minutes));
        } else {
            status.setText(R.string.ads_status_on);
        }
    }
}

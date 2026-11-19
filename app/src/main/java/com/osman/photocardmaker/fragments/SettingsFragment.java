package com.osman.photocardmaker.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;

import com.osman.photocardmaker.BuildConfig;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.network.ImageLoader;
import com.osman.photocardmaker.utils.Prefs;
import com.osman.photocardmaker.utils.Util;

import java.io.File;

public class SettingsFragment extends Fragment {
    private LinearLayout list;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        list = v.findViewById(R.id.list);
        Context c = requireContext();
        row(getString(R.string.set_language), Util.isBn(c) ? getString(R.string.lang_bn) : getString(R.string.lang_en), x -> chooseLanguage());
        row(getString(R.string.set_theme), themeName(Prefs.getTheme(c)), x -> chooseTheme());
        row(getString(R.string.set_privacy), null, x -> openUrl(BuildConfig.PRIVACY_URL));
        if (AdManager.isPrivacyOptionsRequired(requireActivity()))
            row(getString(R.string.set_ad_privacy), null, x -> AdManager.showPrivacyOptions(requireActivity()));
        row(getString(R.string.set_about), null, x -> new AlertDialog.Builder(c)
                .setTitle(R.string.app_name).setMessage(R.string.about_msg)
                .setPositiveButton(R.string.ok, null).show());
        row(getString(R.string.set_share_app), null, x -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, c.getPackageName()));
            startActivity(Intent.createChooser(i, getString(R.string.set_share_app)));
        });
        row(getString(R.string.set_rate), null, x -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + c.getPackageName()))); }
            catch (ActivityNotFoundException e) { openUrl("https://play.google.com/store/apps/details?id=" + c.getPackageName()); }
        });
        row(getString(R.string.set_contact), BuildConfig.SUPPORT_EMAIL, x -> {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + BuildConfig.SUPPORT_EMAIL));
            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            try { startActivity(i); } catch (ActivityNotFoundException e) { Util.toast(c, R.string.no_app); }
        });
        row(getString(R.string.set_version), BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", null);
        row(getString(R.string.set_clear_cache), null, x -> clearCache());
    }

    private String themeName(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) return getString(R.string.theme_light);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) return getString(R.string.theme_dark);
        return getString(R.string.theme_system);
    }

    private void chooseLanguage() {
        final String[] tags = {"en", "bn"};
        String[] names = {getString(R.string.lang_en), getString(R.string.lang_bn)};
        new AlertDialog.Builder(requireContext()).setTitle(R.string.set_language)
                .setItems(names, (d, i) -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[i])))
                .show();
    }

    private void chooseTheme() {
        final int[] modes = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
        String[] names = {getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark)};
        new AlertDialog.Builder(requireContext()).setTitle(R.string.set_theme)
                .setItems(names, (d, i) -> {
                    Prefs.setTheme(requireContext(), modes[i]);
                    AppCompatDelegate.setDefaultNightMode(modes[i]);
                }).show();
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException e) { Util.toast(requireContext(), R.string.no_app); }
    }

    private void clearCache() {
        Context c = requireContext();
        Util.deleteRecursive(new File(c.getCacheDir(), "shared"));
        Util.deleteRecursive(new File(c.getCacheDir(), "thumbs"));
        ImageLoader.clear();
        Util.toast(c, R.string.cache_cleared);
    }

    private void row(String title, @Nullable String sub, @Nullable View.OnClickListener l) {
        Context c = requireContext();
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Util.dp(c, 20), Util.dp(c, 14), Util.dp(c, 20), Util.dp(c, 14));
        TypedValue tv = new TypedValue();
        c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        r.setBackgroundResource(tv.resourceId);
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(16);
        t.setTextColor(c.getColor(R.color.text));
        r.addView(t);
        if (sub != null) {
            TextView s = new TextView(c);
            s.setText(sub);
            s.setTextSize(13);
            s.setTextColor(c.getColor(R.color.text2));
            r.addView(s);
        }
        if (l != null) r.setOnClickListener(l);
        list.addView(r);
    }
}

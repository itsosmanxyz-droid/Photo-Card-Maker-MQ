package com.osman.photocardmaker.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.fragments.CategoriesFragment;
import com.osman.photocardmaker.fragments.HomeFragment;
import com.osman.photocardmaker.fragments.ProjectsFragment;
import com.osman.photocardmaker.fragments.SettingsFragment;
import com.osman.photocardmaker.network.ImageLoader;
import com.osman.photocardmaker.templates.TemplateRepository;

public class MainActivity extends AppCompatActivity {
    public interface Refreshable { void refresh(); }

    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        nav = findViewById(R.id.nav);
        nav.setOnItemSelectedListener(item -> { show(item.getItemId()); return true; });
        if (b == null) nav.setSelectedItemId(R.id.nav_home);

        AdManager.init(this);
        TemplateRepository.get().refreshRemote(this, () -> {
            ImageLoader.clear();
            Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
            if (f instanceof Refreshable) ((Refreshable) f).refresh();
        });
    }

    public void select(int menuId) { nav.setSelectedItemId(menuId); }

    private void show(int id) {
        Fragment f;
        if (id == R.id.nav_categories) f = new CategoriesFragment();
        else if (id == R.id.nav_projects) f = new ProjectsFragment();
        else if (id == R.id.nav_settings) f = new SettingsFragment();
        else f = new HomeFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
    }
}

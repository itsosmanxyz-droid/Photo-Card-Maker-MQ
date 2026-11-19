package com.osman.photocardmaker.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.activities.EditorActivity;
import com.osman.photocardmaker.activities.MainActivity;
import com.osman.photocardmaker.activities.TemplateListActivity;
import com.osman.photocardmaker.ads.AdManager;
import com.osman.photocardmaker.adapters.CategoryAdapter;
import com.osman.photocardmaker.adapters.ProjectAdapter;
import com.osman.photocardmaker.adapters.TemplateAdapter;
import com.osman.photocardmaker.models.Category;
import com.osman.photocardmaker.models.Project;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.templates.TemplateActions;
import com.osman.photocardmaker.templates.TemplateRepository;
import com.osman.photocardmaker.utils.Prefs;
import com.osman.photocardmaker.utils.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment implements MainActivity.Refreshable {
    private LinearLayout sections;
    private RecyclerView searchRv;
    private TextView searchEmpty;
    private EditText search;
    private View hero;
    private FrameLayout banner;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_home, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        sections = v.findViewById(R.id.sections);
        searchRv = v.findViewById(R.id.searchResults);
        searchEmpty = v.findViewById(R.id.searchEmpty);
        search = v.findViewById(R.id.search);
        hero = v.findViewById(R.id.hero);
        banner = v.findViewById(R.id.bannerHolder);
        searchRv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        hero.setOnClickListener(x -> ((MainActivity) requireActivity()).select(R.id.nav_categories));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void onTextChanged(CharSequence a, int b, int c, int d) { }
            @Override public void afterTextChanged(Editable e) { applySearch(e.toString()); }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        AdManager.showBanner(requireActivity(), banner);
    }

    @Override
    public void refresh() {
        if (sections == null || getContext() == null) return;
        applySearch(search.getText().toString());
        buildSections();
    }

    private void applySearch(String q) {
        q = q.trim().toLowerCase(Locale.ROOT);
        boolean on = !q.isEmpty();
        sections.setVisibility(on ? View.GONE : View.VISIBLE);
        hero.setVisibility(on ? View.GONE : View.VISIBLE);
        searchRv.setVisibility(on ? View.VISIBLE : View.GONE);
        if (!on) { searchEmpty.setVisibility(View.GONE); return; }
        final Context ctx = requireContext();
        final String fq = q;
        List<Template> res = TemplateRepository.get().filter(t -> {
            Category k = TemplateRepository.get().category(t.categoryId);
            String hay = (t.name + " " + t.nameBn + " " + (k != null ? k.name + " " + k.nameBn : "")).toLowerCase(Locale.ROOT);
            return hay.contains(fq);
        });
        searchEmpty.setVisibility(res.isEmpty() ? View.VISIBLE : View.GONE);
        searchRv.setAdapter(new TemplateAdapter(ctx, res, false, t -> TemplateActions.open(requireActivity(), t), null));
    }

    private void buildSections() {
        sections.removeAllViews();
        final Context ctx = requireContext();
        TemplateRepository repo = TemplateRepository.get();
        TemplateAdapter.Listener tl = t -> TemplateActions.open(requireActivity(), t);
        Runnable favChanged = () -> sections.post(this::buildSections);

        addTemplates(ctx, R.string.sec_featured, repo.filter(t -> t.featured), tl, favChanged);

        List<Category> cats = repo.categories();
        if (!cats.isEmpty()) {
            RecyclerView rv = addSection(ctx, R.string.sec_categories);
            rv.setAdapter(new CategoryAdapter(ctx, cats, true, c -> {
                Intent i = new Intent(ctx, TemplateListActivity.class);
                i.putExtra(TemplateListActivity.EXTRA_CATEGORY, c.id);
                startActivity(i);
            }));
        }

        addTemplates(ctx, R.string.sec_trending, repo.filter(t -> t.trending), tl, favChanged);

        List<Project> projects = ProjectStore.list(ctx);
        if (projects.size() > 8) projects = new ArrayList<>(projects.subList(0, 8));
        if (!projects.isEmpty()) {
            RecyclerView rv = addSection(ctx, R.string.sec_recent);
            rv.setAdapter(new ProjectAdapter(ctx, projects, true, new ProjectAdapter.Listener() {
                @Override public void onOpen(Project p) {
                    Intent i = new Intent(ctx, EditorActivity.class);
                    i.putExtra(EditorActivity.EXTRA_PROJECT_ID, p.id);
                    startActivity(i);
                }
                @Override public void onMenu(View anchor, Project p) { }
            }));
        }

        final Set<String> favs = Prefs.favorites(ctx);
        addTemplates(ctx, R.string.sec_favorites, repo.filter(t -> favs.contains(t.id)), tl, favChanged);
        addTemplates(ctx, R.string.sec_premium, repo.filter(t -> t.premium), tl, favChanged);
        addTemplates(ctx, R.string.sec_recommended, repo.filter(t -> t.recommended), tl, favChanged);
    }

    private void addTemplates(Context ctx, int title, List<Template> list, TemplateAdapter.Listener l, Runnable fav) {
        if (list.isEmpty()) return;
        RecyclerView rv = addSection(ctx, title);
        rv.setAdapter(new TemplateAdapter(ctx, list, true, l, fav));
    }

    private RecyclerView addSection(Context ctx, int titleRes) {
        TextView t = new TextView(ctx);
        t.setText(titleRes);
        t.setTextColor(ctx.getColor(R.color.text));
        t.setTextSize(17);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setPadding(Util.dp(ctx, 16), Util.dp(ctx, 14), Util.dp(ctx, 16), Util.dp(ctx, 4));
        sections.addView(t);
        RecyclerView rv = new RecyclerView(ctx);
        rv.setLayoutManager(new LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false));
        rv.setClipToPadding(false);
        rv.setPadding(Util.dp(ctx, 10), 0, Util.dp(ctx, 10), 0);
        sections.addView(rv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return rv;
    }
}

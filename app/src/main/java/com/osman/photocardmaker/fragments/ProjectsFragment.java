package com.osman.photocardmaker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.activities.EditorActivity;
import com.osman.photocardmaker.activities.MainActivity;
import com.osman.photocardmaker.adapters.ProjectAdapter;
import com.osman.photocardmaker.models.Project;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.utils.ShareUtil;
import com.osman.photocardmaker.utils.Util;

import java.util.ArrayList;
import java.util.List;

public class ProjectsFragment extends Fragment implements MainActivity.Refreshable {
    private RecyclerView list;
    private TextView empty;
    private TabLayout tabs;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_projects, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        list = v.findViewById(R.id.list);
        empty = v.findViewById(R.id.empty);
        tabs = v.findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText(R.string.tab_all));
        tabs.addTab(tabs.newTab().setText(R.string.tab_favs));
        list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { refresh(); }
            @Override public void onTabUnselected(TabLayout.Tab t) { }
            @Override public void onTabReselected(TabLayout.Tab t) { }
        });
    }

    @Override public void onResume() { super.onResume(); refresh(); }

    @Override
    public void refresh() {
        if (list == null || getContext() == null) return;
        boolean favsOnly = tabs.getSelectedTabPosition() == 1;
        List<Project> all = ProjectStore.list(requireContext());
        List<Project> shown = new ArrayList<>();
        for (Project p : all) if (!favsOnly || p.favorite) shown.add(p);
        empty.setText(favsOnly ? R.string.no_favs : R.string.no_projects);
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        list.setAdapter(new ProjectAdapter(requireContext(), shown, false, new ProjectAdapter.Listener() {
            @Override public void onOpen(Project p) { edit(p); }
            @Override public void onMenu(View anchor, Project p) { showMenu(anchor, p); }
        }));
    }

    private void edit(Project p) {
        Intent i = new Intent(requireContext(), EditorActivity.class);
        i.putExtra(EditorActivity.EXTRA_PROJECT_ID, p.id);
        startActivity(i);
    }

    private void showMenu(View anchor, final Project p) {
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenuInflater().inflate(R.menu.project_menu, pm.getMenu());
        pm.getMenu().findItem(R.id.m_favorite).setTitle(p.favorite ? R.string.m_unfavorite : R.string.m_favorite);
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.m_edit) edit(p);
            else if (id == R.id.m_rename) rename(p);
            else if (id == R.id.m_favorite) {
                try { ProjectStore.setFavorite(requireContext(), p.id, !p.favorite); } catch (Exception ignored) { }
                refresh();
            } else if (id == R.id.m_share) ShareUtil.shareProject(requireActivity(), p.id);
            else if (id == R.id.m_delete) confirmDelete(p);
            return true;
        });
        pm.show();
    }

    private void rename(final Project p) {
        final EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setText(p.name);
        et.setSelection(p.name.length());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.rename_title)
                .setView(et)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    String n = et.getText().toString().trim();
                    if (n.isEmpty()) return;
                    try { ProjectStore.rename(requireContext(), p.id, n); } catch (Exception ignored) { }
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(final Project p) {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.delete_confirm)
                .setPositiveButton(R.string.m_delete, (d, w) -> { ProjectStore.delete(requireContext(), p.id); refresh(); })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}

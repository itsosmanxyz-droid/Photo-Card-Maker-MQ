package com.osman.photocardmaker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.activities.MainActivity;
import com.osman.photocardmaker.activities.TemplateListActivity;
import com.osman.photocardmaker.adapters.CategoryAdapter;
import com.osman.photocardmaker.templates.TemplateRepository;

public class CategoriesFragment extends Fragment implements MainActivity.Refreshable {
    private RecyclerView list;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_categories, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        list = v.findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        refresh();
    }

    @Override
    public void refresh() {
        if (list == null || getContext() == null) return;
        list.setAdapter(new CategoryAdapter(requireContext(), TemplateRepository.get().categories(), false, c -> {
            Intent i = new Intent(requireContext(), TemplateListActivity.class);
            i.putExtra(TemplateListActivity.EXTRA_CATEGORY, c.id);
            startActivity(i);
        }));
    }
}

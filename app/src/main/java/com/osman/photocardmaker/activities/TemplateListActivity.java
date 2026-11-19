package com.osman.photocardmaker.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.adapters.TemplateAdapter;
import com.osman.photocardmaker.models.Category;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.templates.TemplateActions;
import com.osman.photocardmaker.templates.TemplateRepository;

import java.util.List;

public class TemplateListActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY = "category_id";

    private RecyclerView list;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_template_list);
        findViewById(R.id.back).setOnClickListener(v -> finish());
        list = findViewById(R.id.list);
        list.setLayoutManager(new GridLayoutManager(this, 2));
        Category c = TemplateRepository.get().category(getIntent().getStringExtra(EXTRA_CATEGORY));
        ((TextView) findViewById(R.id.title)).setText(c != null ? c.getName(this) : getString(R.string.app_name));
    }

    @Override
    protected void onResume() {
        super.onResume();
        List<Template> items = TemplateRepository.get().templatesOf(getIntent().getStringExtra(EXTRA_CATEGORY));
        findViewById(R.id.comingSoon).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        list.setAdapter(new TemplateAdapter(this, items, false, t -> TemplateActions.open(this, t), null));
    }
}

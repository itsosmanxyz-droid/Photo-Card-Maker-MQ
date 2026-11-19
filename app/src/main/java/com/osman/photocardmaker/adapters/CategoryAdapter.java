package com.osman.photocardmaker.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.osman.photocardmaker.R;
import com.osman.photocardmaker.models.Category;
import com.osman.photocardmaker.utils.Util;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
    public interface Listener { void onClick(Category c); }

    private final Context ctx;
    private final List<Category> items;
    private final boolean horizontal;
    private final Listener listener;

    public CategoryAdapter(Context ctx, List<Category> items, boolean horizontal, Listener l) {
        this.ctx = ctx;
        this.items = items;
        this.horizontal = horizontal;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView icon, name;
        VH(View v) {
            super(v);
            card = (MaterialCardView) v;
            icon = v.findViewById(R.id.icon);
            name = v.findViewById(R.id.name);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_category, parent, false);
        if (horizontal) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = Util.dp(ctx, 150);
            v.setLayoutParams(lp);
        }
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final Category c = items.get(pos);
        h.card.setCardBackgroundColor(c.color);
        h.icon.setText(c.icon);
        h.name.setText(c.getName(ctx));
        h.itemView.setOnClickListener(v -> listener.onClick(c));
    }

    @Override public int getItemCount() { return items.size(); }
}

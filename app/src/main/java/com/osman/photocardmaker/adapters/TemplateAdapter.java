package com.osman.photocardmaker.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.models.Template;
import com.osman.photocardmaker.network.ImageLoader;
import com.osman.photocardmaker.network.RemoteConfig;
import com.osman.photocardmaker.storage.ExportUtil;
import com.osman.photocardmaker.utils.Prefs;
import com.osman.photocardmaker.utils.Util;

import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {
    public interface Listener { void onClick(Template t); }

    private final Context ctx;
    private final List<Template> items;
    private final boolean horizontal;
    private final Listener listener;
    private final Runnable onFavChanged;

    public TemplateAdapter(Context ctx, List<Template> items, boolean horizontal, Listener l, Runnable onFavChanged) {
        this.ctx = ctx;
        this.items = items;
        this.horizontal = horizontal;
        this.listener = l;
        this.onFavChanged = onFavChanged;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, size, pro, fav;
        VH(View v) {
            super(v);
            img = v.findViewById(R.id.preview);
            name = v.findViewById(R.id.name);
            size = v.findViewById(R.id.size);
            pro = v.findViewById(R.id.pro);
            fav = v.findViewById(R.id.fav);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_template, parent, false);
        if (horizontal) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = Util.dp(ctx, 170);
            v.setLayoutParams(lp);
        }
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final Template t = items.get(pos);
        h.name.setText(t.getName(ctx));
        h.size.setText(t.width + " × " + t.height);
        h.pro.setVisibility(t.premium && !Prefs.isUnlocked(ctx, t.id) ? View.VISIBLE : View.GONE);
        h.fav.setText(Prefs.isFavorite(ctx, t.id) ? "♥" : "♡");
        ImageLoader.load(h.img, "t:" + t.id + ":" + t.thumbnailUrl, () -> {
            Bitmap b = null;
            if (t.thumbnailUrl != null && !t.thumbnailUrl.isEmpty()) {
                try { b = RemoteConfig.fetchBitmap(ctx, t.thumbnailUrl); } catch (Exception ignored) { }
            }
            if (b == null && t.design != null)
                b = ExportUtil.render(t.design, 400f / Math.max(t.width, t.height), true);
            return b;
        });
        h.itemView.setOnClickListener(v -> listener.onClick(t));
        h.fav.setOnClickListener(v -> {
            Prefs.toggleFavorite(ctx, t.id);
            int p = h.getBindingAdapterPosition();
            if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
            if (onFavChanged != null) onFavChanged.run();
        });
    }

    @Override public int getItemCount() { return items.size(); }
}

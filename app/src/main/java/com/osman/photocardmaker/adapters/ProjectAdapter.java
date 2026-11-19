package com.osman.photocardmaker.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.models.Project;
import com.osman.photocardmaker.network.ImageLoader;
import com.osman.photocardmaker.storage.ProjectStore;
import com.osman.photocardmaker.utils.Util;

import java.io.File;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.VH> {
    public interface Listener {
        void onOpen(Project p);
        void onMenu(View anchor, Project p);
    }

    private final Context ctx;
    private final List<Project> items;
    private final boolean compact;
    private final Listener listener;

    public ProjectAdapter(Context ctx, List<Project> items, boolean compact, Listener l) {
        this.ctx = ctx;
        this.items = items;
        this.compact = compact;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, date, fav, menu;
        VH(View v) {
            super(v);
            img = v.findViewById(R.id.preview);
            name = v.findViewById(R.id.name);
            date = v.findViewById(R.id.date);
            fav = v.findViewById(R.id.fav);
            menu = v.findViewById(R.id.menu);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_project, parent, false);
        if (compact) {
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = Util.dp(ctx, 170);
            v.setLayoutParams(lp);
        }
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final Project p = items.get(pos);
        h.name.setText(p.name);
        h.date.setText(Util.fmtDate(p.updated));
        h.fav.setVisibility(p.favorite ? View.VISIBLE : View.GONE);
        h.menu.setVisibility(compact ? View.GONE : View.VISIBLE);
        final File pf = ProjectStore.previewFile(ctx, p.id);
        ImageLoader.load(h.img, "p:" + pf.getAbsolutePath() + ":" + pf.lastModified(), () -> {
            if (!pf.exists()) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 1;
            Bitmap b = BitmapFactory.decodeFile(pf.getAbsolutePath(), o);
            return b;
        });
        h.itemView.setOnClickListener(v -> listener.onOpen(p));
        h.menu.setOnClickListener(v -> listener.onMenu(v, p));
    }

    @Override public int getItemCount() { return items.size(); }
}

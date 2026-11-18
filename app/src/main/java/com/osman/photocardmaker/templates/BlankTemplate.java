package com.osman.photocardmaker.templates;

import android.content.Context;

import com.osman.photocardmaker.R;
import com.osman.photocardmaker.model.CardTemplate;

/** The empty starting card used by "Custom Photo Card" and "New template". */
public final class BlankTemplate {

    private BlankTemplate() {
    }

    public static CardTemplate create(Context ctx) {
        CardTemplate t = new CardTemplate();
        t.id = "custom_blank";
        t.name = "Custom Card";
        t.category = "Custom";
        t.headerText = ctx.getString(R.string.sample_label_news);
        t.headline = ctx.getString(R.string.sample_headline);
        t.subText = ctx.getString(R.string.sample_sub);
        return t;
    }
}

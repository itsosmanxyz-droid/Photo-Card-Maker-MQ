package com.osman.photocardmaker.editor;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.models.Element;

public class DesignRenderer {

    public static void draw(Canvas c, Design d, boolean placeholders) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (d.bg1 == d.bg2) {
            c.drawColor(d.bg1);
        } else {
            p.setShader(new LinearGradient(0, 0, 0, d.height, d.bg1, d.bg2, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, d.width, d.height, p);
            p.setShader(null);
        }
        for (Element e : d.elements) drawElement(c, e, placeholders);
        drawFrame(c, d);
    }

    public static void drawElement(Canvas c, Element e, boolean placeholders) {
        if (e.type == Element.IMAGE && !placeholders) {
            if (e.imagePath == null || e.imagePath.isEmpty() || BitmapStore.get(e.imagePath) == null) return;
        }
        int sc = e.alpha < 255 ? c.saveLayerAlpha(null, e.alpha) : c.save();
        c.translate(e.x, e.y);
        c.rotate(e.rotation);
        c.scale(e.flipH ? -e.scale : e.scale, e.scale);
        switch (e.type) {
            case Element.TEXT: drawText(c, e); break;
            case Element.IMAGE: drawImage(c, e, placeholders); break;
            default: drawShape(c, e); break;
        }
        c.restoreToCount(sc);
    }

    private static StaticLayout layout(Element e, boolean outlinePass) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(e.textSize);
        int style = (e.bold && e.italic) ? Typeface.BOLD_ITALIC : e.bold ? Typeface.BOLD
                : e.italic ? Typeface.ITALIC : Typeface.NORMAL;
        tp.setTypeface(Typeface.create(e.font, style));
        if (outlinePass) {
            tp.setStyle(Paint.Style.STROKE);
            tp.setStrokeWidth(e.textSize * 0.14f);
            tp.setStrokeJoin(Paint.Join.ROUND);
            tp.setColor(e.outlineColor);
        } else {
            tp.setColor(e.color);
            if (e.shadow) tp.setShadowLayer(e.textSize * 0.08f, e.textSize * 0.05f, e.textSize * 0.05f, 0xAA000000);
        }
        Layout.Alignment a = e.align == 0 ? Layout.Alignment.ALIGN_NORMAL
                : e.align == 2 ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_CENTER;
        int width = Math.max(10, (int) e.w);
        return StaticLayout.Builder.obtain(e.text, 0, e.text.length(), tp, width)
                .setAlignment(a).setIncludePad(false).build();
    }

    private static void drawText(Canvas c, Element e) {
        if (e.text == null || e.text.isEmpty()) return;
        StaticLayout fill = layout(e, false);
        e.h = fill.getHeight();
        c.translate(-e.w / 2f, -e.h / 2f);
        if (e.textBg != 0) {
            float pad = e.textSize * 0.3f;
            Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
            bp.setColor(e.textBg);
            c.drawRoundRect(-pad, -pad * 0.6f, e.w + pad, e.h + pad * 0.6f, pad, pad, bp);
        }
        if (e.outline) layout(e, true).draw(c);
        fill.draw(c);
    }

    private static Path maskPath(int mask, RectF r) {
        if (mask == 0) return null;
        Path p = new Path();
        if (mask == 2) p.addOval(r, Path.Direction.CW);
        else {
            float rad = Math.min(r.width(), r.height()) * 0.15f;
            p.addRoundRect(r, rad, rad, Path.Direction.CW);
        }
        return p;
    }

    private static void drawImage(Canvas c, Element e, boolean placeholders) {
        RectF r = new RectF(-e.w / 2f, -e.h / 2f, e.w / 2f, e.h / 2f);
        Bitmap b = BitmapStore.get(e.imagePath);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (b == null) {
            if (!placeholders) return;
            Path mp = maskPath(e.shape, r);
            p.setColor(0x33888888);
            if (mp != null) c.drawPath(mp, p); else c.drawRect(r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, Math.min(e.w, e.h) * 0.012f));
            p.setColor(0xAA888888);
            float dash = Math.min(e.w, e.h) * 0.05f;
            p.setPathEffect(new DashPathEffect(new float[]{dash, dash}, 0));
            if (mp != null) c.drawPath(mp, p); else c.drawRect(r, p);
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(0xCC888888);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTextSize(Math.min(e.w, e.h) * 0.3f);
            c.drawText("+", 0, Math.min(e.w, e.h) * 0.1f, tp);
            return;
        }
        Path clip = maskPath(e.shape, r);
        if (clip != null) c.clipPath(clip);
        float bw = b.getWidth(), bh = b.getHeight();
        float s = Math.max(r.width() / bw, r.height() / bh);
        float sw = r.width() / s, sh = r.height() / s;
        Rect src = new Rect((int) ((bw - sw) / 2), (int) ((bh - sh) / 2), (int) ((bw + sw) / 2), (int) ((bh + sh) / 2));
        c.drawBitmap(b, src, r, p);
    }

    private static void drawShape(Canvas c, Element e) {
        float hw = e.w / 2f, hh = e.h / 2f;
        Path p = new Path();
        switch (e.shape) {
            case Element.S_ROUND:
                float rad = Math.min(e.w, e.h) * 0.2f;
                p.addRoundRect(new RectF(-hw, -hh, hw, hh), rad, rad, Path.Direction.CW);
                break;
            case Element.S_CIRCLE:
                p.addOval(new RectF(-hw, -hh, hw, hh), Path.Direction.CW);
                break;
            case Element.S_TRIANGLE:
                p.moveTo(0, -hh); p.lineTo(hw, hh); p.lineTo(-hw, hh); p.close();
                break;
            case Element.S_STAR:
                for (int i = 0; i < 10; i++) {
                    double ang = Math.toRadians(-90 + i * 36);
                    float k = (i % 2 == 0) ? 1f : 0.45f;
                    float px = (float) Math.cos(ang) * hw * k, py = (float) Math.sin(ang) * hh * k;
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.close();
                break;
            case Element.S_HEART:
                p.moveTo(0, hh);
                p.cubicTo(-hw * 1.3f, hh * 0.1f, -hw * 0.9f, -hh, 0, -hh * 0.45f);
                p.cubicTo(hw * 0.9f, -hh, hw * 1.3f, hh * 0.1f, 0, hh);
                p.close();
                break;
            default:
                p.addRect(-hw, -hh, hw, hh, Path.Direction.CW);
        }
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (e.color2 != 0) paint.setShader(new LinearGradient(0, -hh, 0, hh, e.color, e.color2, Shader.TileMode.CLAMP));
        else paint.setColor(e.color);
        c.drawPath(p, paint);
    }

    private static void drawFrame(Canvas c, Design d) {
        if (d.frame == 0) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(d.frameColor);
        float fw = d.frameWidth, w = d.width, h = d.height;
        switch (d.frame) {
            case 1:
                p.setStrokeWidth(fw);
                c.drawRect(fw / 2, fw / 2, w - fw / 2, h - fw / 2, p);
                break;
            case 2:
                p.setStrokeWidth(fw * 0.4f);
                c.drawRect(fw * 0.2f, fw * 0.2f, w - fw * 0.2f, h - fw * 0.2f, p);
                p.setStrokeWidth(fw * 0.2f);
                c.drawRect(fw * 0.9f, fw * 0.9f, w - fw * 0.9f, h - fw * 0.9f, p);
                break;
            case 3:
                p.setStrokeWidth(fw * 0.6f);
                c.drawRoundRect(fw, fw, w - fw, h - fw, fw * 2, fw * 2, p);
                break;
            default:
                p.setStrokeWidth(fw * 0.4f);
                p.setPathEffect(new DashPathEffect(new float[]{fw * 1.2f, fw * 0.8f}, 0));
                c.drawRect(fw, fw, w - fw, h - fw, p);
        }
    }
}

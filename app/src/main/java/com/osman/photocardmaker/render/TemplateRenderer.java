package com.osman.photocardmaker.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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

import com.osman.photocardmaker.model.CardTemplate;
import com.osman.photocardmaker.model.Decoration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Draws a CardTemplate onto a Canvas / Bitmap.
 * Works in template pixels (t.width x t.height), then scales to the
 * requested output size, so previews and HD exports look identical.
 */
public final class TemplateRenderer {

    private static final Map<String, Typeface> FONTS = new HashMap<String, Typeface>();

    private TemplateRenderer() {
    }

    /** Renders the template to a new bitmap of the given width (height keeps the aspect ratio). */
    public static Bitmap render(CardTemplate t, int outW, Bitmap photo, Bitmap logo) {
        int outH = Math.max(1, Math.round(outW * (float) t.height / (float) t.width));
        Bitmap bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.scale(outW / (float) t.width, outH / (float) t.height);
        draw(c, t, photo, logo);
        return bmp;
    }

    /** Draws onto an existing canvas that is already scaled to template pixels. */
    public static void draw(Canvas c, CardTemplate t, Bitmap photo, Bitmap logo) {
        float w = t.width;
        float h = t.height;
        drawBackground(c, t, w, h);
        drawDecorations(c, t, w, h, false);
        drawPhoto(c, t, w, h, photo);
        drawFooter(c, t, w, h);
        drawHeader(c, t, w, h);
        drawLogo(c, t, w, h, logo);
        drawDate(c, t, w, h);
        drawDecorations(c, t, w, h, true);
        drawBorder(c, t, w, h);
    }

    // ------------------------------------------------------------------

    private static void drawBackground(Canvas c, CardTemplate t, float w, float h) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (t.bgColor1 == t.bgColor2) {
            p.setColor(t.bgColor1);
        } else {
            double a = Math.toRadians(t.gradientAngle);
            float dx = (float) (Math.cos(a) * w / 2.0);
            float dy = (float) (Math.sin(a) * h / 2.0);
            p.setShader(new LinearGradient(w / 2f - dx, h / 2f - dy, w / 2f + dx, h / 2f + dy,
                    t.bgColor1, t.bgColor2, Shader.TileMode.CLAMP));
        }
        c.drawRect(0, 0, w, h, p);
    }

    private static void drawDecorations(Canvas c, CardTemplate t, float w, float h, boolean top) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (Decoration d : t.decorations) {
            if (d.onTop != top) {
                continue;
            }
            p.setColor(d.color);
            float l = d.l * w;
            float tp = d.t * h;
            float r = d.r * w;
            float b = d.b * h;
            if (d.type == Decoration.CIRCLE) {
                c.drawOval(new RectF(l, tp, r, b), p);
            } else if (d.type == Decoration.SLANT) {
                float skew = Math.min((b - tp) * 0.4f, (r - l) * 0.5f);
                Path path = new Path();
                path.moveTo(l + skew, tp);
                path.lineTo(r, tp);
                path.lineTo(r - skew, b);
                path.lineTo(l, b);
                path.close();
                c.drawPath(path, p);
            } else {
                c.drawRect(l, tp, r, b, p);
            }
        }
    }

    private static Path shapePath(RectF r, int shape, float corner) {
        Path path = new Path();
        if (shape == CardTemplate.SHAPE_CIRCLE) {
            path.addOval(r, Path.Direction.CW);
        } else {
            path.addRoundRect(r, corner, corner, Path.Direction.CW);
        }
        return path;
    }

    private static float clamp(float v, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, v));
    }

    private static void drawPhoto(Canvas c, CardTemplate t, float w, float h, Bitmap photo) {
        RectF pr = new RectF(t.photoL * w, t.photoT * h, t.photoR * w, t.photoB * h);
        if (pr.width() <= 0 || pr.height() <= 0) {
            return;
        }
        Path path = shapePath(pr, t.photoShape, t.photoCorner);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);

        c.save();
        c.clipPath(path);
        if (photo != null && photo.getWidth() > 0 && photo.getHeight() > 0) {
            // center-crop with zoom and manual offset
            float bw = photo.getWidth();
            float bh = photo.getHeight();
            float scale = Math.max(pr.width() / bw, pr.height() / bh) * Math.max(1f, t.photoZoom);
            float srcW = pr.width() / scale;
            float srcH = pr.height() / scale;
            float cx = bw / 2f + t.photoOffX * (bw - srcW) / 2f;
            float cy = bh / 2f + t.photoOffY * (bh - srcH) / 2f;
            float left = clamp(cx - srcW / 2f, 0f, bw - srcW);
            float top = clamp(cy - srcH / 2f, 0f, bh - srcH);
            Rect src = new Rect(Math.round(left), Math.round(top),
                    Math.round(left + srcW), Math.round(top + srcH));
            c.drawBitmap(photo, src, pr, p);
        } else {
            // placeholder silhouette
            p.setColor(0xFF8C97A3);
            c.drawRect(pr, p);
            float cx = pr.centerX();
            float m = Math.min(pr.width(), pr.height());
            p.setColor(0xFFD0D7DE);
            c.drawCircle(cx, pr.top + pr.height() * 0.36f, m * 0.14f, p);
            c.drawOval(new RectF(cx - m * 0.30f, pr.top + pr.height() * 0.58f,
                    cx + m * 0.30f, pr.bottom + m * 0.25f), p);
        }
        c.restore();

        if (t.photoBorderW > 0) {
            Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
            bp.setStyle(Paint.Style.STROKE);
            bp.setStrokeWidth(t.photoBorderW);
            bp.setColor(t.photoBorderColor);
            c.drawPath(path, bp);
        }
    }

    private static void drawHeader(Canvas c, CardTemplate t, float w, float h) {
        if (!t.showHeader) {
            return;
        }
        float top = t.headerTopFrac * h;
        float hh = t.headerHeightFrac * h;
        float hw = t.headerWidthFrac * w;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(t.headerColor);
        c.drawRect(0, top, hw, top + hh, p);

        if (t.headerText == null || t.headerText.length() == 0) {
            return;
        }
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(typeface(t.headerFamily, t.headerBold, t.headerItalic, t.headerFontPath));
        tp.setColor(t.headerTextColor);

        float pad = w * 0.035f;
        float avail = hw - 2 * pad;
        float size = t.headerTextSize;
        tp.setTextSize(size);
        while (tp.measureText(t.headerText) > avail && size > 12) {
            size -= 2;
            tp.setTextSize(size);
        }
        float y = top + hh / 2f - (tp.ascent() + tp.descent()) / 2f;
        float x;
        if (t.headerAlign == CardTemplate.ALIGN_CENTER) {
            tp.setTextAlign(Paint.Align.CENTER);
            x = hw / 2f;
        } else if (t.headerAlign == CardTemplate.ALIGN_RIGHT) {
            tp.setTextAlign(Paint.Align.RIGHT);
            x = hw - pad;
        } else {
            tp.setTextAlign(Paint.Align.LEFT);
            x = pad;
        }
        c.drawText(t.headerText, x, y, tp);
    }

    private static StaticLayout buildLayout(String text, TextPaint tp, float size,
                                            int width, int align) {
        tp.setTextSize(size);
        Layout.Alignment a = Layout.Alignment.ALIGN_NORMAL;
        if (align == CardTemplate.ALIGN_CENTER) {
            a = Layout.Alignment.ALIGN_CENTER;
        } else if (align == CardTemplate.ALIGN_RIGHT) {
            a = Layout.Alignment.ALIGN_OPPOSITE;
        }
        return new StaticLayout(text, tp, width, a, 1.1f, 0f, false);
    }

    private static void drawFooter(Canvas c, CardTemplate t, float w, float h) {
        float top = t.footerTopFrac * h;
        float fw = w * t.footerWidthFrac;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(t.footerColor);
        c.drawRect(0, top, fw, h, p);

        float stripe = t.footerAccentHeightFrac * h;
        if (stripe > 0) {
            p.setColor(t.footerAccentColor);
            c.drawRect(0, top, fw, top + stripe, p);
        }

        float pad = w * t.footerPadFrac;
        float padV = h * 0.025f;
        float textW = fw - 2 * pad;
        boolean hasSub = t.subText != null && t.subText.length() > 0;
        float subSpace = hasSub ? t.subSize * 1.7f : 0f;
        float availH = (h - top) - stripe - 2 * padV - subSpace;

        if (t.headline != null && t.headline.length() > 0 && availH > 20 && textW > 20) {
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTypeface(typeface(t.headlineFamily, t.headlineBold, t.headlineItalic,
                    t.headlineFontPath));
            tp.setColor(t.headlineColor);
            float size = t.headlineSize;
            StaticLayout lay = buildLayout(t.headline, tp, size, (int) textW, t.headlineAlign);
            while (lay.getHeight() > availH && size > 20) {
                size -= 2;
                lay = buildLayout(t.headline, tp, size, (int) textW, t.headlineAlign);
            }
            float y = top + stripe + padV + Math.max(0f, (availH - lay.getHeight()) / 2f);
            c.save();
            c.translate(pad, y);
            lay.draw(c);
            c.restore();
        }

        if (hasSub && textW > 20) {
            TextPaint sp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            sp.setTypeface(typeface(t.headlineFamily, false, false, t.headlineFontPath));
            sp.setColor(t.subColor);
            float size = t.subSize;
            sp.setTextSize(size);
            while (sp.measureText(t.subText) > textW && size > 12) {
                size -= 2;
                sp.setTextSize(size);
            }
            float x;
            if (t.headlineAlign == CardTemplate.ALIGN_CENTER) {
                sp.setTextAlign(Paint.Align.CENTER);
                x = fw / 2f;
            } else if (t.headlineAlign == CardTemplate.ALIGN_RIGHT) {
                sp.setTextAlign(Paint.Align.RIGHT);
                x = fw - pad;
            } else {
                sp.setTextAlign(Paint.Align.LEFT);
                x = pad;
            }
            c.drawText(t.subText, x, h - padV - size * 0.35f, sp);
        }
    }

    /** Date badge: a rounded pill centred on the top edge of the footer. */
    private static void drawDate(Canvas c, CardTemplate t, float w, float h) {
        if (!t.showDate) {
            return;
        }
        String text = t.dateAuto ? BanglaDate.today() : t.dateText;
        if (text == null || text.length() == 0) {
            return;
        }
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(typeface(t.headlineFamily, true, false, t.headlineFontPath));
        tp.setColor(t.dateColor);
        tp.setTextSize(t.dateSize);
        tp.setTextAlign(Paint.Align.CENTER);

        float padX = t.dateSize * 0.7f;
        float padY = t.dateSize * 0.38f;
        float pw = tp.measureText(text) + 2 * padX;
        float ph = (tp.descent() - tp.ascent()) + 2 * padY;

        float footerTop = t.footerTopFrac * h;
        float fw = w * t.footerWidthFrac;
        float pad = Math.max(w * t.footerPadFrac, w * 0.03f);
        float left;
        if (t.dateAlign == CardTemplate.ALIGN_CENTER) {
            left = (fw - pw) / 2f;
        } else if (t.dateAlign == CardTemplate.ALIGN_RIGHT) {
            left = fw - pad - pw;
        } else {
            left = pad;
        }
        float top = footerTop - ph / 2f + t.dateOffsetY * h;
        RectF r = new RectF(left, top, left + pw, top + ph);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(t.dateBgColor);
        c.drawRoundRect(r, ph / 2f, ph / 2f, p);
        c.drawText(text, r.centerX(), r.centerY() - (tp.ascent() + tp.descent()) / 2f, tp);
    }

    private static void drawLogo(Canvas c, CardTemplate t, float w, float h, Bitmap logo) {
        if (!t.showLogo) {
            return;
        }
        RectF lr = new RectF(t.logoL * w, t.logoT * h, t.logoR * w, t.logoB * h);
        if (lr.width() <= 0 || lr.height() <= 0) {
            return;
        }
        Path path = shapePath(lr, t.logoShape, lr.height() * 0.2f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(t.logoBgColor);
        c.drawPath(path, p);

        c.save();
        c.clipPath(path);
        if (logo != null && logo.getWidth() > 0 && logo.getHeight() > 0) {
            float s = Math.min(lr.width() * 0.84f / logo.getWidth(),
                    lr.height() * 0.84f / logo.getHeight());
            float dw = logo.getWidth() * s;
            float dh = logo.getHeight() * s;
            RectF dst = new RectF(lr.centerX() - dw / 2f, lr.centerY() - dh / 2f,
                    lr.centerX() + dw / 2f, lr.centerY() + dh / 2f);
            p.setFilterBitmap(true);
            c.drawBitmap(logo, null, dst, p);
        } else {
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTypeface(Typeface.DEFAULT_BOLD);
            tp.setColor(contrast(t.logoBgColor));
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTextSize(Math.min(lr.height() * 0.34f, lr.width() * 0.26f));
            float y = lr.centerY() - (tp.ascent() + tp.descent()) / 2f;
            c.drawText("LOGO", lr.centerX(), y, tp);
        }
        c.restore();
    }

    private static void drawBorder(Canvas c, CardTemplate t, float w, float h) {
        if (t.borderWidth <= 0) {
            return;
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(t.borderWidth);
        p.setColor(t.borderColor);
        float half = t.borderWidth / 2f;
        c.drawRect(half, half, w - half, h - half, p);
    }

    private static int contrast(int color) {
        double lum = 0.299 * Color.red(color) + 0.587 * Color.green(color)
                + 0.114 * Color.blue(color);
        return lum > 150 ? 0xFF000000 : 0xFFFFFFFF;
    }

    private static Typeface typeface(int family, boolean bold, boolean italic, String fontPath) {
        Typeface base = null;
        if (fontPath != null && fontPath.length() > 0) {
            base = FONTS.get(fontPath);
            if (base == null) {
                try {
                    File f = new File(fontPath);
                    if (f.exists()) {
                        base = Typeface.createFromFile(f);
                        FONTS.put(fontPath, base);
                    }
                } catch (Exception e) {
                    base = null;
                }
            }
        }
        if (base == null) {
            if (family == CardTemplate.FONT_SERIF) {
                base = Typeface.SERIF;
            } else if (family == CardTemplate.FONT_MONO) {
                base = Typeface.MONOSPACE;
            } else {
                base = Typeface.SANS_SERIF;
            }
        }
        int style = Typeface.NORMAL;
        if (bold && italic) {
            style = Typeface.BOLD_ITALIC;
        } else if (bold) {
            style = Typeface.BOLD;
        } else if (italic) {
            style = Typeface.ITALIC;
        }
        return Typeface.create(base, style);
    }
}

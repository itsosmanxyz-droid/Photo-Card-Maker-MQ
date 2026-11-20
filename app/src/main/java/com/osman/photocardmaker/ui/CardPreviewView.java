package com.osman.photocardmaker.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.osman.photocardmaker.model.CardTemplate;
import com.osman.photocardmaker.render.TemplateRenderer;

/** Live preview of a card. Dragging inside the photo area pans the photo. */
public class CardPreviewView extends View {

    private CardTemplate template;
    private Bitmap photo;
    private Bitmap logo;
    private Runnable onEdited;

    private float scale = 1f;
    private float left;
    private float top;
    private float lastX;
    private float lastY;
    private boolean dragging;

    public CardPreviewView(Context context) {
        super(context);
    }

    public CardPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(CardTemplate t, Bitmap photo, Bitmap logo) {
        this.template = t;
        this.photo = photo;
        this.logo = logo;
        invalidate();
    }

    public void setBitmaps(Bitmap photo, Bitmap logo) {
        this.photo = photo;
        this.logo = logo;
        invalidate();
    }

    public void setOnEditedListener(Runnable r) {
        this.onEdited = r;
    }

    private void computeMetrics() {
        float margin = 16f * getResources().getDisplayMetrics().density;
        float aw = Math.max(1f, getWidth() - 2 * margin);
        float ah = Math.max(1f, getHeight() - 2 * margin);
        scale = Math.min(aw / template.width, ah / template.height);
        left = (getWidth() - template.width * scale) / 2f;
        top = (getHeight() - template.height * scale) / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (template == null) {
            return;
        }
        computeMetrics();
        Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadow.setColor(0x33000000);
        float off = 4f * getResources().getDisplayMetrics().density;
        canvas.drawRect(left + off, top + off,
                left + template.width * scale + off, top + template.height * scale + off, shadow);
        canvas.save();
        canvas.translate(left, top);
        canvas.scale(scale, scale);
        canvas.clipRect(0, 0, template.width, template.height);
        TemplateRenderer.draw(canvas, template, photo, logo);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (template == null || photo == null) {
            return false;
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                computeMetrics();
                float tx = (e.getX() - left) / scale;
                float ty = (e.getY() - top) / scale;
                boolean inside = tx >= template.photoL * template.width
                        && tx <= template.photoR * template.width
                        && ty >= template.photoT * template.height
                        && ty <= template.photoB * template.height;
                dragging = inside;
                lastX = e.getX();
                lastY = e.getY();
                return inside;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) {
                    return false;
                }
                float dx = (e.getX() - lastX) / scale;
                float dy = (e.getY() - lastY) / scale;
                lastX = e.getX();
                lastY = e.getY();
                float pw = Math.max(1f, (template.photoR - template.photoL) * template.width);
                float ph = Math.max(1f, (template.photoB - template.photoT) * template.height);
                template.photoOffX = clamp(template.photoOffX - dx / (pw / 2f));
                template.photoOffY = clamp(template.photoOffY - dy / (ph / 2f));
                invalidate();
                if (onEdited != null) {
                    onEdited.run();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return dragging;
        }
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }
}

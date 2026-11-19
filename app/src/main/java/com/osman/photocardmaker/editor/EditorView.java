package com.osman.photocardmaker.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.osman.photocardmaker.models.Design;
import com.osman.photocardmaker.models.Element;
import com.osman.photocardmaker.utils.Util;

import org.json.JSONObject;

import java.util.ArrayDeque;

public class EditorView extends View {

    public interface Listener {
        void onSelectionChanged(Element e);
        void onChanged();
        void onRequestImage(Element placeholder);
        void onEditText(Element e);
    }

    private Design design = new Design();
    private Element selected;
    private Listener listener;
    private float vs = 1f, offX, offY;
    private final Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayDeque<String> undo = new ArrayDeque<>();
    private final ArrayDeque<String> redo = new ArrayDeque<>();

    private float lastX, lastY, downX, downY;
    private boolean dragging, moved, pinching, wasSelectedOnDown;
    private Element downHit;
    private float startDist, startAngle, startScale, startRot;
    private final int slop;

    public EditorView(Context c) { this(c, null); }

    public EditorView(Context c, AttributeSet a) {
        super(c, a);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setColor(0xFF6C4DF6);
    }

    public void setListener(Listener l) { listener = l; }
    public Design getDesign() { return design; }
    public Element getSelected() { return selected; }
    public boolean canUndo() { return !undo.isEmpty(); }
    public boolean canRedo() { return !redo.isEmpty(); }

    public void setDesign(Design d) {
        design = d;
        selected = null;
        undo.clear();
        redo.clear();
        layoutCanvas();
        invalidate();
        if (listener != null) { listener.onSelectionChanged(null); }
    }

    public void select(Element e) {
        selected = e;
        invalidate();
        if (listener != null) listener.onSelectionChanged(e);
    }

    // ---- change tracking -------------------------------------------------
    private String snap() {
        try { return design.toJson().toString(); } catch (Exception e) { return null; }
    }

    public void beginChange() {
        String s = snap();
        if (s == null) return;
        undo.push(s);
        if (undo.size() > 60) undo.removeLast();
        redo.clear();
    }

    public void endChange() {
        invalidate();
        if (listener != null) listener.onChanged();
    }

    public void undo() {
        if (undo.isEmpty()) return;
        String cur = snap();
        String prev = undo.pop();
        if (cur != null) redo.push(cur);
        restore(prev);
    }

    public void redo() {
        if (redo.isEmpty()) return;
        String cur = snap();
        String next = redo.pop();
        if (cur != null) undo.push(cur);
        restore(next);
    }

    private void restore(String s) {
        try {
            String selId = selected != null ? selected.id : null;
            design = Design.fromJson(new JSONObject(s), 0, 0, false);
            selected = null;
            if (selId != null) for (Element e : design.elements) if (selId.equals(e.id)) selected = e;
            layoutCanvas();
            invalidate();
            if (listener != null) { listener.onSelectionChanged(selected); listener.onChanged(); }
        } catch (Exception ignored) { }
    }

    // ---- element operations ---------------------------------------------
    public void addElement(Element e) {
        beginChange();
        design.elements.add(e);
        selected = e;
        invalidate();
        if (listener != null) { listener.onSelectionChanged(e); listener.onChanged(); }
    }

    public void deleteSelected() {
        if (selected == null) return;
        beginChange();
        design.elements.remove(selected);
        selected = null;
        invalidate();
        if (listener != null) { listener.onSelectionChanged(null); listener.onChanged(); }
    }

    public void duplicateSelected() {
        if (selected == null) return;
        try {
            Element c = Element.fromJson(selected.toJson(), false);
            c.id = Element.newId();
            c.x += design.width * 0.04f;
            c.y += design.width * 0.04f;
            addElement(c);
        } catch (Exception ignored) { }
    }

    public void moveSelected(int dir) {
        if (selected == null) return;
        int i = design.elements.indexOf(selected);
        int j = i + dir;
        if (i < 0 || j < 0 || j >= design.elements.size()) return;
        beginChange();
        design.elements.remove(i);
        design.elements.add(j, selected);
        endChange();
    }

    // ---- geometry --------------------------------------------------------
    private void layoutCanvas() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || design.width == 0 || design.height == 0) return;
        float pad = Util.dp(getContext(), 16);
        vs = Math.min((w - 2 * pad) / design.width, (h - 2 * pad) / design.height);
        offX = (w - design.width * vs) / 2f;
        offY = (h - design.height * vs) / 2f;
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        layoutCanvas();
    }

    private float effH(Element e) {
        if (e.type == Element.TEXT) return e.h > 0 ? e.h : e.textSize * 1.3f;
        return e.h;
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFF3A3A40);
        canvas.save();
        canvas.translate(offX, offY);
        canvas.scale(vs, vs);
        canvas.clipRect(0, 0, design.width, design.height);
        DesignRenderer.draw(canvas, design, true);
        canvas.restore();
        if (selected != null) {
            canvas.save();
            canvas.translate(offX, offY);
            canvas.scale(vs, vs);
            canvas.translate(selected.x, selected.y);
            canvas.rotate(selected.rotation);
            float hw = selected.w * selected.scale / 2f, hh = effH(selected) * selected.scale / 2f;
            selPaint.setStrokeWidth(2.5f / vs);
            selPaint.setPathEffect(new DashPathEffect(new float[]{12f / vs, 8f / vs}, 0));
            canvas.drawRect(-hw, -hh, hw, hh, selPaint);
            canvas.restore();
        }
    }

    private float toCx(float vx) { return (vx - offX) / vs; }
    private float toCy(float vy) { return (vy - offY) / vs; }

    private Element hit(float cx, float cy) {
        for (int i = design.elements.size() - 1; i >= 0; i--) {
            Element e = design.elements.get(i);
            double r = Math.toRadians(-e.rotation);
            float dx = cx - e.x, dy = cy - e.y;
            float lx = (float) (dx * Math.cos(r) - dy * Math.sin(r)) / e.scale;
            float ly = (float) (dx * Math.sin(r) + dy * Math.cos(r)) / e.scale;
            float extra = Util.dp(getContext(), 14) / vs / e.scale;
            if (Math.abs(lx) <= e.w / 2f + extra && Math.abs(ly) <= effH(e) / 2f + extra) return e;
        }
        return null;
    }

    private static float dist(MotionEvent ev) {
        return (float) Math.hypot(ev.getX(1) - ev.getX(0), ev.getY(1) - ev.getY(0));
    }

    private static float angle(MotionEvent ev) {
        return (float) Math.toDegrees(Math.atan2(ev.getY(1) - ev.getY(0), ev.getX(1) - ev.getX(0)));
    }

    private void notifyChanged() { if (listener != null) listener.onChanged(); }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = ev.getX();
                downY = lastY = ev.getY();
                moved = false;
                pinching = false;
                downHit = hit(toCx(downX), toCy(downY));
                wasSelectedOnDown = downHit != null && downHit == selected;
                if (downHit != null && downHit != selected) select(downHit);
                dragging = downHit != null;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (ev.getPointerCount() == 2 && selected != null) {
                    pinching = true;
                    dragging = false;
                    startDist = dist(ev);
                    startAngle = angle(ev);
                    startScale = selected.scale;
                    startRot = selected.rotation;
                    beginChange();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pinching && ev.getPointerCount() >= 2 && selected != null) {
                    float d = dist(ev);
                    if (startDist > 0) selected.scale = Math.max(0.05f, Math.min(6f, startScale * d / startDist));
                    selected.rotation = startRot + (angle(ev) - startAngle);
                    moved = true;
                    invalidate();
                    notifyChanged();
                } else if (dragging && selected != null && ev.getPointerCount() == 1) {
                    if (!moved) {
                        if (Math.hypot(ev.getX() - downX, ev.getY() - downY) < slop) return true;
                        moved = true;
                        beginChange();
                        lastX = ev.getX();
                        lastY = ev.getY();
                        return true;
                    }
                    selected.x += (ev.getX() - lastX) / vs;
                    selected.y += (ev.getY() - lastY) / vs;
                    lastX = ev.getX();
                    lastY = ev.getY();
                    invalidate();
                    notifyChanged();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                pinching = false;
                dragging = false;
                moved = true;
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved && !pinching) {
                    if (downHit == null) {
                        select(null);
                    } else if (listener != null) {
                        boolean emptyImg = downHit.type == Element.IMAGE
                                && (downHit.imagePath == null || downHit.imagePath.isEmpty());
                        if (emptyImg) listener.onRequestImage(downHit);
                        else if (wasSelectedOnDown && downHit.type == Element.TEXT) listener.onEditText(downHit);
                    }
                }
                dragging = false;
                pinching = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                pinching = false;
                return true;
        }
        return super.onTouchEvent(ev);
    }
}

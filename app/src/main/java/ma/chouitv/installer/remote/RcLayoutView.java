package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.EnumMap;
import java.util.Map;

/**
 * A remote layout drawn on its grid: a miniature when it is only shown, a surface to arrange the keys when it is editable (touch a key, drag it, it snaps
 * to the grid; a place that is refused shows red and tells why). The rules are RemoteLayout's: this view only draws and passes the touches on.
 */
public final class RcLayoutView extends View {
    public interface Listener {
        void onChanged(RemoteLayout layout);
        void onRefused(RemoteLayout.Why why);
        void onSelected(RemoteLayout.Kind kind);
    }

    private static final int GOLD = 0xFFD6AF6B, FILL = 0xFF16372A, WELL = 0xFF0B1F17, EDGE = 0x66D6AF6B, TEXT = 0xFFF0E6C8;

    private RemoteLayout layout = RemoteLayout.template(RemoteLayout.Template.CLASSIC);
    private final Map<RemoteLayout.Kind, String> labels = new EnumMap<>(RemoteLayout.Kind.class);
    private boolean editable;
    private int maxRows = 24;
    private RemoteLayout.Kind selected;
    private Listener listener;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    // a drag in progress
    private RemoteLayout.Kind dragging;
    private float downX, downY, grabDx, grabDy;
    private boolean moved;
    private int ghostCol, ghostRow;
    private RemoteLayout.Why ghostWhy;

    public RcLayoutView(Context c) { super(c); }

    public void setListener(Listener l) { listener = l; }

    public void setLayout(RemoteLayout l) { layout = l; requestLayout(); invalidate(); }

    public RemoteLayout getLayout() { return layout; }

    public void setLabels(Map<RemoteLayout.Kind, String> l) { labels.clear(); labels.putAll(l); invalidate(); }

    public void setEditable(boolean e) { editable = e; requestLayout(); invalidate(); }

    /** The rows the screen offers (the surface of the editor is that tall, so that a key can be dropped anywhere the screen can show it). */
    public void setMaxRows(int rows) { maxRows = Math.max(1, Math.min(rows, 40)); requestLayout(); invalidate(); }

    public int maxRowsShown() { return maxRows; }

    public void setSelected(RemoteLayout.Kind k) { selected = k; invalidate(); }

    public RemoteLayout.Kind getSelected() { return selected; }

    private float unit() { return getWidth() / (float) RemoteLayout.COLS; }

    @Override protected void onMeasure(int w, int h) {
        int width = MeasureSpec.getSize(w);
        int rows = editable ? Math.max(maxRows, layout.rows()) : layout.rows();
        setMeasuredDimension(width, Math.round(width / (float) RemoteLayout.COLS * rows));
    }

    private static boolean isShortcut(RemoteLayout.Kind k) {
        switch (k) {
            case LISTEN: case CHANNELS: case APPS: case SAVER: case WRITE: case TVSET: case YOUTUBE: case NETFLIX: case KEYBOARD: return true;
            default: return false;
        }
    }

    @Override protected void onDraw(Canvas c) {
        float u = unit();
        if (editable) {   // the grid, as dots, and the limit of what the screen shows
            p.setStyle(Paint.Style.FILL);
            p.setColor(0x33D6AF6B);
            for (int rr = 0; rr <= maxRows; rr++) for (int cc = 0; cc <= RemoteLayout.COLS; cc++) c.drawCircle(cc * u, rr * u, u * 0.04f, p);
        }
        for (RemoteLayout.Block b : layout.blocks()) {
            if (b.kind == dragging && moved) continue;
            drawBlock(c, b.kind, b.col * u, b.row * u, b.w * u, b.h * u, b.kind == selected, 1f);
        }
        if (dragging != null && moved) {
            RemoteLayout.Block b = layout.get(dragging);
            boolean ok = ghostWhy == null;
            drawBlock(c, dragging, ghostCol * u, ghostRow * u, b.w * u, b.h * u, true, 0.85f);
            r.set(ghostCol * u, ghostRow * u, (ghostCol + b.w) * u, (ghostRow + b.h) * u);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(u * 0.12f);
            p.setColor(ok ? 0xFF4CD37F : 0xFFE8896B);
            c.drawRoundRect(r, u * 0.5f, u * 0.5f, p);
        }
    }

    private void drawBlock(Canvas c, RemoteLayout.Kind k, float x, float y, float w, float h, boolean sel, float alpha) {
        float u = unit(), pad = u * 0.12f;
        r.set(x + pad, y + pad, x + w - pad, y + h - pad);
        int a = Math.round(255 * alpha);
        p.setStyle(Paint.Style.FILL);
        p.setColor((FILL & 0x00FFFFFF) | (a << 24));
        if (k == RemoteLayout.Kind.PAD) {
            float cx = r.centerX(), cy = r.centerY(), rad = Math.min(r.width(), r.height()) / 2f;
            c.drawCircle(cx, cy, rad, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(u * (sel ? 0.14f : 0.07f));
            p.setColor(sel ? 0xFFFFE9A8 : GOLD);
            c.drawCircle(cx, cy, rad, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(GOLD);
            c.drawCircle(cx, cy, rad * 0.3f, p);
            p.setColor(WELL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(Math.max(8f, rad * 0.26f));
            c.drawText("OK", cx, cy + rad * 0.09f, p);
            lock(c, k, cx + rad * 0.55f, cy - rad * 0.8f, u);
            return;
        }
        float round = RemoteLayout.isVolume(k) ? Math.min(r.width(), r.height()) / 2f : u * 0.55f;
        c.drawRoundRect(r, round, round, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(u * (sel ? 0.14f : 0.07f));
        p.setColor(sel ? 0xFFFFE9A8 : (RemoteLayout.isVolume(k) ? GOLD : EDGE));
        c.drawRoundRect(r, round, round, p);
        String text = text(k);
        if (!text.isEmpty()) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(isShortcut(k) ? GOLD : TEXT);
            p.setTextAlign(Paint.Align.CENTER);
            float size = Math.min(u * 0.85f, 14f * getResources().getDisplayMetrics().density);
            p.setTextSize(size);
            while (size > 7f && p.measureText(text) > r.width() - u * 0.3f) { size -= 1f; p.setTextSize(size); }
            c.drawText(text, r.centerX(), r.centerY() + size * 0.35f, p);
        }
        if (RemoteLayout.isVolume(k)) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(GOLD);
            p.setTextSize(u * 1.2f);
            p.setTextAlign(Paint.Align.CENTER);
            boolean tall = r.height() > r.width();
            if (tall) { c.drawText("+", r.centerX(), r.top + u * 1.7f, p); c.drawText("−", r.centerX(), r.bottom - u * 0.6f, p); }
            else { c.drawText("−", r.left + u * 0.9f, r.centerY() + u * 0.4f, p); c.drawText("+", r.right - u * 0.9f, r.centerY() + u * 0.4f, p); }
        }
        lock(c, k, r.right - u * 0.45f, r.top + u * 0.45f, u);
    }

    private String text(RemoteLayout.Kind k) {
        String s = labels.get(k);
        if (s == null) return "";
        if (RemoteLayout.isVolume(k)) return "";   // its + and − say it
        return s;
    }

    /** A small padlock on the keys that stay. */
    private void lock(Canvas c, RemoteLayout.Kind k, float cx, float cy, float u) {
        if (!editable || !RemoteLayout.required(k)) return;
        p.setStyle(Paint.Style.FILL);
        p.setColor(GOLD);
        r.set(cx - u * 0.28f, cy - u * 0.05f, cx + u * 0.28f, cy + u * 0.4f);
        c.drawRoundRect(r, u * 0.06f, u * 0.06f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(u * 0.09f);
        r.set(cx - u * 0.17f, cy - u * 0.38f, cx + u * 0.17f, cy + u * 0.1f);
        c.drawArc(r, 180, 180, false, p);
    }

    private RemoteLayout.Block blockAt(float x, float y) {
        float u = unit();
        RemoteLayout.Block hit = null;
        for (RemoteLayout.Block b : layout.blocks()) {
            if (x >= b.col * u && x < (b.col + b.w) * u && y >= b.row * u && y < (b.row + b.h) * u) hit = b;
        }
        return hit;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (!editable) return false;
        float u = unit();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                RemoteLayout.Block b = blockAt(e.getX(), e.getY());
                downX = e.getX(); downY = e.getY();
                moved = false;
                dragging = b == null ? null : b.kind;
                if (b != null) { grabDx = e.getX() - b.col * u; grabDy = e.getY() - b.row * u; selected = b.kind; if (listener != null) listener.onSelected(b.kind); }
                else { selected = null; if (listener != null) listener.onSelected(null); invalidate(); return false; }   // an empty place: the page scrolls
                getParent().requestDisallowInterceptTouchEvent(true);
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (dragging == null) return true;
                if (!moved && Math.hypot(e.getX() - downX, e.getY() - downY) < ViewConfiguration.get(getContext()).getScaledTouchSlop()) return true;
                moved = true;
                ghostCol = Math.round((e.getX() - grabDx) / u);
                ghostRow = Math.round((e.getY() - grabDy) / u);
                RemoteLayout.Edit t = layout.move(dragging, ghostCol, ghostRow, maxRows);
                ghostWhy = t.ok() ? null : t.why;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (dragging != null && moved) {
                    RemoteLayout.Edit t = layout.move(dragging, ghostCol, ghostRow, maxRows);
                    if (t.ok()) { layout = t.layout; if (listener != null) listener.onChanged(layout); }
                    else if (listener != null) listener.onRefused(t.why);
                }
                dragging = null; moved = false;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                dragging = null; moved = false;
                invalidate();
                return true;
            default: return true;
        }
    }
}

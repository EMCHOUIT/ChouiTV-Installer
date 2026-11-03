package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * The pad of the remote, drawn like the one of a real remote: a dark well holding four separate wedges (up, right, down, left) around a big gold OK,
 * with a dark slot along each diagonal. The geometry is in {@link RingGeometry}. A finger kept on a wedge repeats the arrow (after 0.4 s, then every 0.12 s).
 */
public final class RcRing extends View {
    public interface Listener { void onKey(int code); }

    private static final int WELL = 0xFF081A10, FACE = 0xFF2B6648, PRESSED = 0xFF1B4631, EDGE = 0x33FFFFFF, GOLD = 0xFFD6AF6B, INK = 0xFF2A1F05;
    private static final float R_IN = 58f, R_OUT = 140f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF outer = new RectF(150f - R_OUT, 150f - R_OUT, 150f + R_OUT, 150f + R_OUT);
    private final RectF inner = new RectF(150f - R_IN, 150f - R_IN, 150f + R_IN, 150f + R_IN);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private Listener listener;
    private int pressed = RingGeometry.NONE;
    private float scale = 1f;

    private final Runnable again = new Runnable() {
        @Override public void run() {
            Integer k = RingGeometry.keyOf(pressed);
            if (k != null && pressed != RingGeometry.OK && listener != null) {
                listener.onKey(k);
                ui.postDelayed(this, 120);
            }
        }
    };

    public RcRing(Context c, Typeface okFace) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.BUTT);
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(INK);
        label.setTextSize(36f);
        if (okFace != null) label.setTypeface(okFace);
        setContentDescription("Remote pad");
    }

    public void setListener(Listener l) { listener = l; }

    @Override protected void onMeasure(int w, int h) {
        int side = MeasureSpec.getSize(w);
        setMeasuredDimension(side, side);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { scale = w / RingGeometry.SIZE; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int zone = RingGeometry.zoneAt(e.getX() / scale, e.getY() / scale);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = zone;
                if (zone != RingGeometry.NONE) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                if (zone != RingGeometry.NONE && zone != RingGeometry.OK && listener != null) {
                    listener.onKey(RingGeometry.keyOf(zone));
                    ui.postDelayed(again, 400);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (zone != pressed) { ui.removeCallbacks(again); pressed = RingGeometry.NONE; invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                ui.removeCallbacks(again);
                int was = pressed;
                pressed = RingGeometry.NONE;
                invalidate();
                if (was == RingGeometry.OK && zone == RingGeometry.OK && listener != null) listener.onKey(Remote.OK);
                return true;
            case MotionEvent.ACTION_CANCEL:
                ui.removeCallbacks(again);
                pressed = RingGeometry.NONE;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override protected void onDetachedFromWindow() {
        ui.removeCallbacks(again);
        super.onDetachedFromWindow();
    }

    private void wedge(Canvas c, int zone, float centerAngle) {
        float start = centerAngle - 45f;
        path.reset();
        path.arcTo(outer, start, 90f);
        path.arcTo(inner, start + 90f, -90f);
        path.close();
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(pressed == zone ? PRESSED : FACE);
        c.drawPath(path, fill);
        // a light edge on the outer rim, a dark one under the inner rim: the wedge looks raised
        line.setStrokeWidth(1.6f);
        line.setColor(pressed == zone ? 0x00000000 : EDGE);
        RectF rim = new RectF(outer);
        rim.inset(1.5f, 1.5f);
        c.drawArc(rim, start + 4f, 82f, false, line);
    }

    /** A chevron at (cx, cy) pointing the way (dx, dy). */
    private void chevron(Canvas c, float cx, float cy, float dx, float dy) {
        float px = -dy, py = dx;
        path.reset();
        path.moveTo(cx - dx * 8f + px * 13f, cy - dy * 8f + py * 13f);
        path.lineTo(cx + dx * 8f, cy + dy * 8f);
        path.lineTo(cx - dx * 8f - px * 13f, cy - dy * 8f - py * 13f);
        c.drawPath(path, line);
    }

    @Override protected void onDraw(Canvas c) {
        c.save();
        c.scale(scale, scale);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(WELL);
        c.drawCircle(150f, 150f, 149f, fill);
        line.setStrokeWidth(1.5f);
        line.setColor(0x55D6AF6B);
        c.drawCircle(150f, 150f, 148f, line);

        wedge(c, RingGeometry.UP, -90f);
        wedge(c, RingGeometry.RIGHT, 0f);
        wedge(c, RingGeometry.DOWN, 90f);
        wedge(c, RingGeometry.LEFT, 180f);

        // the slots between the wedges
        line.setStrokeWidth(RingGeometry.DEAD_BAND * 1.6f);
        line.setColor(WELL);
        c.drawLine(150f - 140f * 0.7071f, 150f - 140f * 0.7071f, 150f + 140f * 0.7071f, 150f + 140f * 0.7071f, line);
        c.drawLine(150f - 140f * 0.7071f, 150f + 140f * 0.7071f, 150f + 140f * 0.7071f, 150f - 140f * 0.7071f, line);

        // chevrons
        line.setStrokeWidth(5f);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(GOLD);
        chevron(c, 150f, 51f, 0f, -1f);
        chevron(c, 249f, 150f, 1f, 0f);
        chevron(c, 150f, 249f, 0f, 1f);
        chevron(c, 51f, 150f, -1f, 0f);
        line.setStrokeCap(Paint.Cap.BUTT);

        // OK: a dark socket, the gold dish with a ring, and the word
        fill.setColor(WELL);
        c.drawCircle(150f, 150f, 56f, fill);
        fill.setColor(pressed == RingGeometry.OK ? 0xFFB48C3A : 0xFFD0A84E);
        c.drawCircle(150f, 150f, 50f, fill);
        fill.setColor(pressed == RingGeometry.OK ? 0xFFC39A44 : 0xFFE2BE68);
        c.drawCircle(150f, 148f, 44f, fill);
        line.setStrokeWidth(1.2f);
        line.setColor(0x66402E0A);
        c.drawCircle(150f, 150f, 37f, line);
        c.drawText("OK", 150f, 162f, label);
        c.restore();
    }
}

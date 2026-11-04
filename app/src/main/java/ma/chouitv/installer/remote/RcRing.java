package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;

/**
 * The pad of the remote, drawn like a real one with layers (no flat fills): a dark plate holding four raised wedges (up, right, down, left) with parallel
 * gaps between them, around a big gold OK in a dish. Each wedge has a dark lip under it and a gradient face, and sinks 2 units when pressed.
 * The geometry (what is under a finger) is in {@link RingGeometry}. A finger kept on a wedge repeats the arrow (after 0.4 s, then every 0.12 s).
 */
public final class RcRing extends View {
    public interface Listener { void onKey(int code); }

    private static final float R_OUT = 139.6f, R_IN = 71.5f, GAP = 16.2f, LIP = 3.5f, SINK = 2.3f;
    private static final int PLATE = 0xFF06110B, CHEVRON = 0xFFE9D9A6;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
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
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(0xFF241A04);
        label.setTextSize(31f);
        if (okFace != null) label.setTypeface(okFace);
        label.setFakeBoldText(true);
        setContentDescription("Remote pad");
    }

    public void setListener(Listener l) { listener = l; }

    private int maxSide = Integer.MAX_VALUE;

    /** The pad is a square as wide as its room, but no bigger than this (px): the remote fits screens of any height. */
    public void setMaxSidePx(int px) { if (px != maxSide) { maxSide = px; requestLayout(); } }

    @Override protected void onMeasure(int w, int h) {
        int side = Math.min(MeasureSpec.getSize(w), maxSide);
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

    private static float px(float r, double deg) { return (float) (150f + r * Math.cos(Math.toRadians(deg))); }
    private static float py(float r, double deg) { return (float) (150f + r * Math.sin(Math.toRadians(deg))); }

    /** A wedge whose two sides are parallel to the diagonals and a constant {@link #GAP} from its neighbours. */
    private void wedgePath(float centerAngle) {
        double dR = Math.toDegrees(Math.asin(GAP / 2f / R_OUT)), dr = Math.toDegrees(Math.asin(GAP / 2f / R_IN));
        float a0 = centerAngle - 45f + (float) dR, sweep = 90f - 2f * (float) dR;
        float b0 = centerAngle + 45f - (float) dr, bsweep = -(90f - 2f * (float) dr);
        path.reset();
        path.arcTo(new RectF(150f - R_OUT, 150f - R_OUT, 150f + R_OUT, 150f + R_OUT), a0, sweep, true);
        path.lineTo(px(R_IN, b0), py(R_IN, b0));
        path.arcTo(new RectF(150f - R_IN, 150f - R_IN, 150f + R_IN, 150f + R_IN), b0, bsweep, false);
        path.close();
    }

    private void wedge(Canvas c, int zone, float angle) {
        boolean down = pressed == zone;
        wedgePath(angle);
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        // the lip under the wedge
        c.save();
        c.translate(0f, LIP);
        fill.setShader(null);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFF030A06);
        c.drawPath(path, fill);
        c.restore();
        // the face, which sinks when pressed
        c.save();
        if (down) c.translate(0f, SINK);
        fill.setShader(down ? null : new LinearGradient(0f, bounds.top, 0f, bounds.bottom, 0xFF31634C, 0xFF193F2E, Shader.TileMode.CLAMP));
        fill.setColor(0xFF173A2B);
        c.drawPath(path, fill);
        fill.setShader(null);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1.2f);
        line.setColor(0x38E9D9A6);
        c.drawPath(path, line);
        // chevron
        line.setStrokeWidth(3.6f);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(CHEVRON);
        float cx = px(106f, angle), cy = py(106f, angle);
        double a = Math.toRadians(angle + 90);
        float s = 10.4f;
        Path ch = new Path();
        float[][] pts = {{-s, s * 0.55f}, {0f, -s * 0.55f}, {s, s * 0.55f}};
        for (int i = 0; i < 3; i++) {
            float x = (float) (pts[i][0] * Math.cos(a) - pts[i][1] * Math.sin(a)) + cx, y = (float) (pts[i][0] * Math.sin(a) + pts[i][1] * Math.cos(a)) + cy;
            if (i == 0) ch.moveTo(x, y); else ch.lineTo(x, y);
        }
        c.drawPath(ch, line);
        line.setStrokeCap(Paint.Cap.BUTT);
        c.restore();
    }

    @Override protected void onDraw(Canvas c) {
        c.save();
        c.scale(scale, scale);
        // the plate
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(null);
        fill.setColor(PLATE);
        c.drawCircle(150f, 150f, 149f, fill);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1.2f);
        line.setColor(0x73C9A44C);
        c.drawCircle(150f, 150f, 148.4f, line);
        line.setStrokeWidth(3.6f);
        line.setColor(0x99000000);
        c.drawCircle(150f, 150f, 145f, line);

        wedge(c, RingGeometry.UP, -90f);
        wedge(c, RingGeometry.RIGHT, 0f);
        wedge(c, RingGeometry.DOWN, 90f);
        wedge(c, RingGeometry.LEFT, 180f);

        // OK: the socket, the gold cap with a dark edge, the dish, the word
        boolean down = pressed == RingGeometry.OK;
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(null);
        fill.setColor(0xFF030A06);
        c.drawCircle(150f, 150f + LIP, 56.5f, fill);
        c.save();
        if (down) c.translate(0f, SINK);
        fill.setShader(new LinearGradient(0f, 93.5f, 0f, 206.5f, down ? 0xFF9A7832 : 0xFFF0DC9E, down ? 0xFFC9A44C : 0xFFA5822F, Shader.TileMode.CLAMP));
        c.drawCircle(150f, 150f, 56.5f, fill);
        line.setStrokeWidth(1.2f);
        line.setColor(0x992A1D05);
        c.drawCircle(150f, 150f, 56.5f, line);
        fill.setShader(new LinearGradient(0f, 105f, 0f, 195f, new int[] {0xFFA8853A, 0xFFC9A44C, 0xFFE2C77A}, new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(150f, 150f, 45f, fill);
        fill.setShader(null);
        c.drawText("OK", 150f, 160.5f, label);
        c.restore();
        c.restore();
    }

    // ---- TalkBack: the pad is five buttons (four arrows and OK), not one picture ----

    private final String[] names = {"Up", "Right", "Down", "Left", "OK"};
    private int a11yFocus = View.NO_ID, hover = View.NO_ID;

    /** What a screen reader says for each button. */
    public void setNames(String up, String right, String down, String left, String ok) {
        names[0] = up; names[1] = right; names[2] = down; names[3] = left; names[4] = ok;
    }

    /** The area of a button, in the pixels of this view: a box around each wedge (on the 300-unit grid) and around the dish for OK. */
    private Rect boundsOf(int zone) {
        float[][] box = {{52f, 10f, 248f, 100f}, {200f, 52f, 290f, 248f}, {52f, 200f, 248f, 290f}, {10f, 52f, 100f, 248f}, {93.5f, 93.5f, 206.5f, 206.5f}};
        float[] b = box[zone];
        return new Rect((int) (b[0] * scale), (int) (b[1] * scale), (int) (b[2] * scale), (int) (b[3] * scale));
    }

    private void sendFor(int id, int type) {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled() || getParent() == null || id < 1 || id > 5) return;
        AccessibilityEvent e = AccessibilityEvent.obtain(type);
        e.setPackageName(getContext().getPackageName());
        e.setClassName("android.widget.Button");
        e.setSource(this, id);
        e.getText().add(names[id - 1]);
        getParent().requestSendAccessibilityEvent(this, e);
    }

    private final AccessibilityNodeProvider provider = new AccessibilityNodeProvider() {
        @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
            if (id == HOST_VIEW_ID) {
                AccessibilityNodeInfo host = AccessibilityNodeInfo.obtain(RcRing.this);
                onInitializeAccessibilityNodeInfo(host);
                for (int i = 1; i <= 5; i++) host.addChild(RcRing.this, i);
                return host;
            }
            if (id < 1 || id > 5) return null;
            AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
            info.setSource(RcRing.this, id);
            info.setParent(RcRing.this);
            info.setPackageName(getContext().getPackageName());
            info.setClassName("android.widget.Button");
            info.setContentDescription(names[id - 1]);
            info.setClickable(true);
            info.setEnabled(true);
            info.setVisibleToUser(true);
            Rect b = boundsOf(id - 1);
            info.setBoundsInParent(b);
            int[] at = new int[2];
            getLocationOnScreen(at);
            Rect screen = new Rect(b);
            screen.offset(at[0], at[1]);
            info.setBoundsInScreen(screen);
            info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            info.setAccessibilityFocused(a11yFocus == id);
            info.addAction(a11yFocus == id ? AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS : AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            return info;
        }

        @Override public boolean performAction(int id, int action, Bundle args) {
            if (id == HOST_VIEW_ID) return performAccessibilityAction(action, args);
            if (id < 1 || id > 5) return false;
            switch (action) {
                case AccessibilityNodeInfo.ACTION_CLICK: {
                    Integer k = RingGeometry.keyOf(id - 1);
                    if (k != null && listener != null) listener.onKey(k);
                    sendFor(id, AccessibilityEvent.TYPE_VIEW_CLICKED);
                    return true;
                }
                case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                    if (a11yFocus != id) { a11yFocus = id; sendFor(id, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED); }
                    return true;
                case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                    if (a11yFocus == id) { a11yFocus = View.NO_ID; sendFor(id, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED); }
                    return true;
                default:
                    return false;
            }
        }
    };

    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() { return provider; }

    /** With touch exploration on, tells the screen reader which button is under the finger, so that it can name it. */
    @Override public boolean onHoverEvent(MotionEvent e) {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled() || !am.isTouchExplorationEnabled()) return super.onHoverEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
            case MotionEvent.ACTION_HOVER_MOVE: {
                int zone = RingGeometry.zoneAt(e.getX() / scale, e.getY() / scale);
                int id = zone == RingGeometry.NONE ? View.NO_ID : zone + 1;
                if (id != hover) {
                    if (hover != View.NO_ID) sendFor(hover, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                    hover = id;
                    if (id != View.NO_ID) sendFor(id, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                }
                return true;
            }
            case MotionEvent.ACTION_HOVER_EXIT:
                if (hover != View.NO_ID) sendFor(hover, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
                hover = View.NO_ID;
                return true;
            default:
                return super.onHoverEvent(e);
        }
    }
}

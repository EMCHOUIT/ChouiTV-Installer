package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * The volume slider of the remote: a sunk rail, a gold fill, a knob that carries the level, the word VOL and the level on top. Dragging shows a bubble with
 * the level; one command goes when the finger lifts (the listener is called once). A tap on the rail jumps there. The part beyond the family's maximum
 * is hatched and the knob stops at the maximum. In Arabic the rail fills from right to left, like Android's sliders.
 */
public final class RcSlider extends View {
    public interface Listener { void onSet(int level); }

    private static final int GOLD = 0xFFD6AF6B, INK = 0xFF2A1F05;

    private final float d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private Listener listener;
    private int level = -1;        // as the TV says (-1: not known yet)
    private int shown = -1;        // under the finger while dragging
    private int limit = 0;         // 0: no maximum (a level of this TV)
    private int max = Volume.DEFAULT_MAX;   // the highest level of this TV
    private int step = 0;          // 0: a twentieth of the scale
    private boolean dragging, failed;
    private String word = "VOL", reading = "…", release = "", spoken = "Volume";
    private Typeface face;
    private int lastTick = -1;

    public RcSlider(Context c, Typeface numberFace) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        face = numberFace;
        setFocusable(true);
    }

    public void setListener(Listener l) { listener = l; }

    /** The words on the slider (VOL, the "reading" state, the hint under the bubble, the spoken name). */
    public void setWords(String vol, String readingWord, String releaseWord, String spokenName) {
        word = vol; reading = readingWord; release = releaseWord; spoken = spokenName;
        invalidate();
    }

    /** The level the TV reported (-1: not known), and whether the TV never answered. */
    public void setLevel(int l, boolean notAnswering) {
        if (dragging) return;   // never fight the finger
        level = l;
        failed = notAnswering && l < 0;
        invalidate();
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    public int getLevel() { return level; }

    public void setLimit(int l) { limit = l; invalidate(); }

    /** The number of levels of this TV (15, 30, 100...): the rail and every number follow it. */
    public void setMax(int m) {
        max = Math.max(2, m);
        if (level > max) level = max;
        invalidate();
    }

    public int getMax() { return max; }

    /** How many levels a TalkBack swipe moves (the step chosen for the + and - keys). */
    public void setStep(int s) { step = Math.max(1, s); }

    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Math.round(63 * d));
    }

    /** A tick of the phone every tenth of the scale (every level on a TV with 10 levels or fewer). */
    private int tickOf(int v) { return max <= 10 ? v : (int) (v * 10L / max); }

    private boolean rtl() { return getLayoutDirection() == LAYOUT_DIRECTION_RTL; }

    private float x0() { return 26f * d; }
    private float x1() { return getWidth() - 26f * d; }

    private float xOf(int lv) {
        float f = Math.max(0, Math.min(max, lv)) / (float) max;
        return rtl() ? x1() - (x1() - x0()) * f : x0() + (x1() - x0()) * f;
    }

    private int levelAt(float x) {
        float f = (x - x0()) / (x1() - x0());
        if (rtl()) f = 1f - f;
        int v = Math.round(Math.max(0f, Math.min(1f, f)) * max);
        return Volume.capped(v, limit, max);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragging = true;
                shown = levelAt(e.getX());
                lastTick = tickOf(shown);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    int v = levelAt(e.getX());
                    int tick = tickOf(v);
                    if (tick != lastTick) { lastTick = tick; performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); }
                    shown = v;
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    level = shown;
                    failed = false;
                    invalidate();
                    if (listener != null) listener.onSet(shown);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override protected void onDraw(Canvas c) {
        int lv = dragging ? shown : level;
        boolean known = lv >= 0;
        float w = getWidth();
        boolean right = rtl();

        // the word and the level on top, one at each end
        text.setStyle(Paint.Style.FILL);
        text.setShader(null);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(11f * d);
        text.setColor(0xFFE9D9A6);
        text.setLetterSpacing(0.1f);
        text.setTextAlign(right ? Paint.Align.RIGHT : Paint.Align.LEFT);
        c.drawText(word, right ? w - 8f * d : 8f * d, 14f * d, text);
        text.setLetterSpacing(0f);
        text.setTextAlign(right ? Paint.Align.LEFT : Paint.Align.RIGHT);
        text.setColor(known ? 0xFFF6EFE0 : 0xFFC6BBA4);
        String top = known ? lv + " / " + max : failed ? "?" : reading;
        c.drawText(top, right ? 8f * d : w - 8f * d, 14f * d, text);

        // the rail, sunk
        float cy = 41f * d, half = 9f * d;
        r.set(x0() - 12f * d, cy - half, x1() + 12f * d, cy + half);
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(0xFF06110B);
        c.drawRoundRect(r, half, half, p);
        p.setShader(new LinearGradient(0, r.top, 0, r.top + 7f * d, 0xCC000000, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, half, half, p);
        p.setShader(null);

        // the part beyond the family's maximum, hatched
        if (limit > 0 && limit < max) {
            float xl = xOf(limit), xe = right ? x0() - 12f * d : x1() + 12f * d;
            float a = Math.min(xl, xe), b = Math.max(xl, xe);
            c.save();
            c.clipRect(a, r.top, b, r.bottom);
            p.setColor(0x66D45B4B);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2f * d);
            for (float x = a - 20f * d; x < b + 20f * d; x += 7f * d) c.drawLine(x, r.bottom, x + 12f * d, r.top, p);
            c.restore();
            p.setStyle(Paint.Style.FILL);
        }

        float kx = known ? xOf(lv) : (right ? x1() : x0());
        if (known) {
            // the gold fill from the quiet end to the knob
            float fa = Math.min(kx, right ? x1() + 12f * d : x0() - 12f * d), fb = Math.max(kx, right ? x1() + 12f * d : x0() - 12f * d);
            RectF fill = new RectF(fa, cy - 6f * d, fb, cy + 6f * d);
            p.setShader(new LinearGradient(0, fill.top, 0, fill.bottom, 0xFFE2C77A, 0xFFA8853A, Shader.TileMode.CLAMP));
            c.drawRoundRect(fill, 6f * d, 6f * d, p);
            p.setShader(null);
        }

        // the knob: a gold cap with a dark lip, the level on it
        float kr = 20f * d;
        p.setColor(0xFF030A06);
        c.drawCircle(kx, cy + 2.5f * d, kr, p);
        p.setShader(new LinearGradient(0, cy - kr, 0, cy + kr, known ? 0xFFF0DC9E : 0xFF8C8A7E, known ? 0xFFA5822F : 0xFF4F4D45, Shader.TileMode.CLAMP));
        c.drawCircle(kx, cy, kr, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f * d);
        p.setColor(0x992A1D05);
        c.drawCircle(kx, cy, kr, p);
        p.setStyle(Paint.Style.FILL);
        text.setTypeface(face != null ? face : Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(15f * d);
        text.setColor(INK);
        c.drawText(known ? String.valueOf(lv) : "?", kx, cy + 5.5f * d, text);

        // while dragging: a bubble above the knob with the level and the hint
        if (dragging && known) {
            float bw = 104f * d, bh = 50f * d;
            float bx = Math.max(bw / 2f, Math.min(w - bw / 2f, kx)), by = cy - kr - 10f * d - bh;
            r.set(bx - bw / 2f, by, bx + bw / 2f, by + bh);
            p.setColor(limit > 0 && lv >= limit ? 0xFFF3C9C2 : 0xFFF6EFE0);
            c.drawRoundRect(r, 14f * d, 14f * d, p);
            text.setColor(INK);
            text.setTypeface(face != null ? face : Typeface.DEFAULT_BOLD);
            text.setTextSize(22f * d);
            c.drawText(String.valueOf(lv), bx, by + 26f * d, text);
            text.setTypeface(Typeface.DEFAULT);
            text.setTextSize(11f * d);
            text.setColor(0xFF4A4636);
            c.drawText(release, bx, by + 42f * d, text);
        }
    }

    // ---- TalkBack: a slider with a spoken value and a step of 5 ----

    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.SeekBar");
        info.setContentDescription(spoken);
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0f, max, Math.max(0, level)));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override public boolean performAccessibilityAction(int action, Bundle args) {
        int cur = Math.max(0, level);
        int to = -1;
        int step = this.step > 0 ? this.step : Math.max(1, max / 20);   // 5 on a TV with 100 levels, 1 on one with 15
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) to = cur + step;
        else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) to = cur - step;
        else if (action == android.R.id.accessibilityActionSetProgress && args != null) to = Math.round(args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, cur));
        if (to < 0 && !(action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return super.performAccessibilityAction(action, args);
        to = Volume.capped(to, limit, max);
        level = to;
        failed = false;
        invalidate();
        if (listener != null) listener.onSet(to);
        return true;
    }
}

package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * The remote drawn like a Google TV one: a pale oval body, a ring with the OK button in the middle, round Back and Home buttons,
 * a volume pill and Mute, and a red power button on top. Everything is drawn here (no images), on a 220 x 420 grid scaled to the width.
 * A press is sent when the finger is lifted on the button it went down on.
 */
public final class RemoteView extends View {
    public interface Listener { void onKey(int code); }

    private static final float W = 220f, H = 444f;
    private static final int BODY = 0xFFD8D3C8, FACE = 0xFFF3F0E8, INK = 0xFF444444, RED = 0xFFC0392B, PRESSED = 0xFFC9C3B6;
    private static final int NONE = -1, POWER = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4, OK = 5, BACK = 6, HOME = 7, VOL_UP = 8, VOL_DOWN = 9, MUTE = 10;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF box = new RectF();
    private Listener listener;
    private int pressed = NONE;
    private float scale = 1f;

    public RemoteView(Context c) {
        super(c);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        label.setTextAlign(Paint.Align.CENTER);
        label.setFakeBoldText(true);
        setContentDescription("Remote control");
    }

    public void setListener(Listener l) { listener = l; }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int max = Math.min(w, (int) (getResources().getDisplayMetrics().density * 300));   // not wider than a real remote looks good
        setMeasuredDimension(max, Math.round(max * H / W));
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { scale = w / W; }

    /** The key a button sends (null: none). */
    public static Integer keyOf(int zone) {
        switch (zone) {
            case POWER: return Remote.POWER;
            case UP: return Remote.UP;
            case DOWN: return Remote.DOWN;
            case LEFT: return Remote.LEFT;
            case RIGHT: return Remote.RIGHT;
            case OK: return Remote.OK;
            case BACK: return Remote.BACK;
            case HOME: return Remote.HOME;
            case VOL_UP: return Remote.VOLUME_UP;
            case VOL_DOWN: return Remote.VOLUME_DOWN;
            case MUTE: return Remote.MUTE;
            default: return null;
        }
    }

    /** Which button is under (x, y), in grid units. */
    static int zoneAt(float x, float y) {
        float dx = x - 110f, dy = y - 150f, d = (float) Math.hypot(dx, dy);
        if (d <= 34f) return OK;
        if (d <= 88f) return Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? LEFT : RIGHT) : (dy < 0 ? UP : DOWN);
        if (Math.hypot(x - 110f, y - 46f) <= 22f) return POWER;
        if (Math.hypot(x - 68f, y - 268f) <= 28f) return BACK;
        if (Math.hypot(x - 152f, y - 268f) <= 28f) return HOME;
        if (Math.hypot(x - 152f, y - 370f) <= 28f) return MUTE;
        if (x >= 38f && x <= 98f && y >= 316f && y <= 424f) return y < 370f ? VOL_UP : VOL_DOWN;
        return NONE;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int zone = zoneAt(e.getX() / scale, e.getY() / scale);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = zone;
                if (zone != NONE) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (zone != pressed) { pressed = NONE; invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                int sent = pressed;
                pressed = NONE;
                invalidate();
                if (sent != NONE && sent == zone && listener != null) listener.onKey(keyOf(sent));
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = NONE;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void disc(Canvas c, float cx, float cy, float r, int zone, int color) {
        fill.setColor(pressed == zone ? PRESSED : color);
        c.drawCircle(cx, cy, r, fill);
    }

    @Override protected void onDraw(Canvas c) {
        c.save();
        c.scale(scale, scale);
        line.setStrokeWidth(3f);
        line.setColor(INK);

        fill.setColor(BODY);
        box.set(0, 0, W, H);
        c.drawRoundRect(box, 110f, 110f, fill);

        disc(c, 110f, 46f, 17f, POWER, RED);
        line.setColor(0xFFFFFFFF);
        box.set(102f, 38f, 118f, 54f);
        c.drawArc(box, -60f, 300f, false, line);
        c.drawLine(110f, 36f, 110f, 46f, line);

        fill.setColor(FACE); c.drawCircle(110f, 150f, 88f, fill);
        if (pressed >= UP && pressed <= RIGHT) {
            fill.setColor(PRESSED);
            box.set(22f, 62f, 198f, 238f);
            float start = pressed == RIGHT ? -45f : pressed == DOWN ? 45f : pressed == LEFT ? 135f : 225f;
            c.drawArc(box, start, 90f, true, fill);
            fill.setColor(FACE);
            c.drawCircle(110f, 150f, 34f, fill);
        }
        disc(c, 110f, 150f, 34f, OK, BODY);
        label.setColor(INK);
        label.setTextSize(17f);
        c.drawText("OK", 110f, 156f, label);
        line.setColor(INK);
        chevron(c, 110f, 100f, 0f, -1f);
        chevron(c, 110f, 200f, 0f, 1f);
        chevron(c, 60f, 150f, -1f, 0f);
        chevron(c, 160f, 150f, 1f, 0f);

        disc(c, 68f, 268f, 24f, BACK, FACE);
        line.setColor(INK);
        path.reset();
        path.moveTo(62f, 262f); path.lineTo(56f, 268f); path.lineTo(62f, 274f);
        path.moveTo(56f, 268f); path.lineTo(70f, 268f); path.quadTo(80f, 268f, 80f, 278f);
        c.drawPath(path, line);

        disc(c, 152f, 268f, 24f, HOME, FACE);
        path.reset();
        path.moveTo(142f, 269f); path.lineTo(152f, 260f); path.lineTo(162f, 269f);
        path.moveTo(145f, 267f); path.lineTo(145f, 278f); path.lineTo(159f, 278f); path.lineTo(159f, 267f);
        c.drawPath(path, line);

        fill.setColor(FACE);
        box.set(38f, 316f, 98f, 424f);
        c.drawRoundRect(box, 30f, 30f, fill);
        if (pressed == VOL_UP || pressed == VOL_DOWN) {
            fill.setColor(PRESSED);
            c.save();
            c.clipRect(38f, pressed == VOL_UP ? 316f : 370f, 98f, pressed == VOL_UP ? 370f : 424f);
            c.drawRoundRect(box, 30f, 30f, fill);
            c.restore();
        }
        line.setColor(INK);
        c.drawLine(58f, 343f, 78f, 343f, line);
        c.drawLine(68f, 333f, 68f, 353f, line);
        c.drawLine(58f, 397f, 78f, 397f, line);
        fill.setColor(BODY);
        c.drawRect(44f, 369f, 92f, 371f, fill);

        disc(c, 152f, 370f, 24f, MUTE, FACE);
        line.setColor(INK);
        line.setStrokeWidth(2.5f);
        path.reset();
        path.moveTo(139f, 366f); path.lineTo(144f, 366f); path.lineTo(151f, 360f); path.lineTo(151f, 380f);
        path.lineTo(144f, 374f); path.lineTo(139f, 374f); path.close();
        c.drawPath(path, line);
        c.drawLine(156f, 365f, 166f, 375f, line);
        c.drawLine(166f, 365f, 156f, 375f, line);
        c.restore();
    }

    /** A chevron at (cx, cy) pointing the way (dx, dy). */
    private void chevron(Canvas c, float cx, float cy, float dx, float dy) {
        line.setStrokeWidth(3.5f);
        path.reset();
        float px = -dy, py = dx;   // perpendicular
        path.moveTo(cx - dx * 6f + px * 9f, cy - dy * 6f + py * 9f);
        path.lineTo(cx + dx * 6f, cy + dy * 6f);
        path.lineTo(cx - dx * 6f - px * 9f, cy - dy * 6f - py * 9f);
        c.drawPath(path, line);
        line.setStrokeWidth(3f);
    }
}

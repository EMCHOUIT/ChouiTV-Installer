package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** The small line icons of the remote, drawn here on a 24 x 24 grid (no image files): the same stroke everywhere so that they read as one family. */
public final class RcIcon extends View {
    public static final int POWER = 0, BACK = 1, HOME = 2, MUTE = 3, VOL_DOWN = 4, VOL_UP = 5, BOOK = 6, TV = 7, APPS = 8, MOON = 9, KEYBOARD = 10, GEAR = 11,
        UP = 12, DOWN = 13, LEFT = 14, RIGHT = 15, MORE = 16;

    private final int kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF box = new RectF();

    public RcIcon(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(2f);
        p.setColor(color);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void speaker(Canvas c) {
        path.reset();
        path.moveTo(3f, 9.5f); path.lineTo(7f, 9.5f); path.lineTo(12f, 5f); path.lineTo(12f, 19f); path.lineTo(7f, 14.5f); path.lineTo(3f, 14.5f); path.close();
        c.drawPath(path, p);
    }

    @Override protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        c.save();
        c.translate((getWidth() - 24f * s) / 2f, (getHeight() - 24f * s) / 2f);
        c.scale(s, s);
        path.reset();
        switch (kind) {
            case POWER:
                box.set(5f, 5f, 19f, 19f);
                c.drawArc(box, -60f, 300f, false, p);
                c.drawLine(12f, 3f, 12f, 11f, p);
                break;
            case BACK:
                path.moveTo(9f, 7f); path.lineTo(4f, 12f); path.lineTo(9f, 17f);
                path.moveTo(4f, 12f); path.lineTo(14f, 12f); path.quadTo(20f, 12f, 20f, 18f);
                c.drawPath(path, p);
                break;
            case HOME:
                path.moveTo(3f, 12f); path.lineTo(12f, 4f); path.lineTo(21f, 12f);
                path.moveTo(6f, 10f); path.lineTo(6f, 20f); path.lineTo(18f, 20f); path.lineTo(18f, 10f);
                c.drawPath(path, p);
                break;
            case MUTE:
                speaker(c);
                c.drawLine(15f, 9f, 21f, 15f, p);
                c.drawLine(21f, 9f, 15f, 15f, p);
                break;
            case VOL_DOWN:
                speaker(c);
                c.drawLine(16f, 12f, 21f, 12f, p);
                break;
            case VOL_UP:
                speaker(c);
                c.drawLine(15f, 12f, 21f, 12f, p);
                c.drawLine(18f, 9f, 18f, 15f, p);
                break;
            case BOOK:
                box.set(4f, 5f, 20f, 19f);
                c.drawRoundRect(box, 2f, 2f, p);
                c.drawLine(12f, 5f, 12f, 19f, p);
                break;
            case TV:
                box.set(3f, 8f, 21f, 20f);
                c.drawRoundRect(box, 2.5f, 2.5f, p);
                path.moveTo(8f, 3.5f); path.lineTo(12f, 8f); path.lineTo(16f, 3.5f);
                c.drawPath(path, p);
                break;
            case APPS:
                box.set(4f, 4f, 10.5f, 10.5f); c.drawRoundRect(box, 1.5f, 1.5f, p);
                box.set(13.5f, 4f, 20f, 10.5f); c.drawRoundRect(box, 1.5f, 1.5f, p);
                box.set(4f, 13.5f, 10.5f, 20f); c.drawRoundRect(box, 1.5f, 1.5f, p);
                box.set(13.5f, 13.5f, 20f, 20f); c.drawRoundRect(box, 1.5f, 1.5f, p);
                break;
            case MOON:
                box.set(4f, 4f, 20f, 20f);
                path.arcTo(box, 70f, 230f);
                box.set(9f, 3f, 23f, 17f);
                path.arcTo(box, 300f, -150f);
                c.drawPath(path, p);
                break;
            case KEYBOARD:
                box.set(3f, 6.5f, 21f, 17.5f);
                c.drawRoundRect(box, 2f, 2f, p);
                c.drawPoint(7f, 10f, p); c.drawPoint(10.5f, 10f, p); c.drawPoint(14f, 10f, p); c.drawPoint(17f, 10f, p);
                c.drawLine(8f, 14f, 16f, 14f, p);
                break;
            case GEAR:
                c.drawCircle(12f, 12f, 3f, p);
                c.drawCircle(12f, 12f, 7f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    c.drawLine(12f + (float) Math.cos(a) * 7f, 12f + (float) Math.sin(a) * 7f, 12f + (float) Math.cos(a) * 9.5f, 12f + (float) Math.sin(a) * 9.5f, p);
                }
                break;
            case UP: chevron(c, 0f, -1f); break;
            case DOWN: chevron(c, 0f, 1f); break;
            case LEFT: chevron(c, -1f, 0f); break;
            case RIGHT: chevron(c, 1f, 0f); break;
            case MORE:
                p.setStrokeWidth(3.2f);
                c.drawPoint(12f, 5f, p); c.drawPoint(12f, 12f, p); c.drawPoint(12f, 19f, p);
                break;
            default:
        }
        c.restore();
    }

    private void chevron(Canvas c, float dx, float dy) {
        p.setStrokeWidth(2.8f);
        float px = -dy, py = dx;
        path.reset();
        path.moveTo(12f - dx * 4f + px * 7f, 12f - dy * 4f + py * 7f);
        path.lineTo(12f + dx * 4f, 12f + dy * 4f);
        path.lineTo(12f - dx * 4f - px * 7f, 12f - dy * 4f - py * 7f);
        c.drawPath(path, p);
    }
}

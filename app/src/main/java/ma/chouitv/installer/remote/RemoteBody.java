package ma.chouitv.installer.remote;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** The body of the remote: a deep green shell with a vertical gradient, a gold edge, and a fine inner ring set 5 dp inside, like the bezel of a real remote. */
public final class RemoteBody extends Drawable {
    private final float d;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RemoteBody(float density) { this.d = density; }

    @Override public void draw(Canvas c) {
        RectF r = new RectF(getBounds());
        float rad = 44f * d;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, new int[] {0xFF1B4331, 0xFF12301F, 0xFF0D2318}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
        // light on the top edge
        c.save();
        c.clipRect(r.left, r.top, r.right, r.top + 4f * d);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f * d);
        p.setColor(0x33FFFFFF);
        RectF e = new RectF(r);
        e.inset(1f * d, 1f * d);
        c.drawRoundRect(e, rad - d, rad - d, p);
        c.restore();
        // the gold edge
        p.setStrokeWidth(2f * d);
        p.setColor(0xFFC9A44C);
        e = new RectF(r);
        e.inset(1f * d, 1f * d);
        c.drawRoundRect(e, rad - d, rad - d, p);
        // the dark band and the fine ring inside it
        p.setStrokeWidth(3.5f * d);
        p.setColor(0xFF0D2318);
        e = new RectF(r);
        e.inset(4.5f * d, 4.5f * d);
        c.drawRoundRect(e, rad - 4.5f * d, rad - 4.5f * d, p);
        p.setStrokeWidth(1f * d);
        p.setColor(0x3AE9D9A6);
        e = new RectF(r);
        e.inset(7f * d, 7f * d);
        c.drawRoundRect(e, rad - 7f * d, rad - 7f * d, p);
        p.setStyle(Paint.Style.FILL);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}

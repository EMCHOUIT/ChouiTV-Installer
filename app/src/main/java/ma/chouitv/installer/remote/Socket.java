package ma.chouitv.installer.remote;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** The well a key sits in: dark, with a shadow cast along its top edge and a faint light line along its bottom edge, so the key looks sunk into the body. */
public final class Socket extends Drawable {
    private final float d, radiusDp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Socket(float density, float radiusDp) {
        this.d = density;
        this.radiusDp = radiusDp;
    }

    @Override public void draw(Canvas c) {
        RectF r = new RectF(getBounds());
        float rad = Math.min(radiusDp * d, Math.min(r.width(), r.height()) / 2f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF06110B);
        c.drawRoundRect(r, rad, rad, p);
        Path clip = new Path();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        p.setShader(new LinearGradient(0, r.top, 0, r.top + 9f * d, 0xCC000000, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(r, p);
        p.setShader(null);
        c.restore();
        // light line along the bottom edge
        c.save();
        c.clipRect(r.left, r.bottom - 2.5f * d, r.right, r.bottom);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.4f * d);
        p.setColor(0x33E9D9A6);
        RectF e = new RectF(r);
        e.inset(0.7f * d, 0.7f * d);
        c.drawRoundRect(e, Math.max(0, rad - 0.7f * d), Math.max(0, rad - 0.7f * d), p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

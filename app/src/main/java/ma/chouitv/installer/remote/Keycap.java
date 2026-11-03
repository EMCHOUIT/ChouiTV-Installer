package ma.chouitv.installer.remote;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * The look of a key of a real remote, drawn with layers instead of a flat fill: a dark lip under the key (its thickness), a face with a vertical gradient,
 * a thin dark rim, a light edge along the top, a soft shade toward the bottom and a fine gold line inside. Pressed, the face sinks 2 dp into the lip and
 * gets a shade at the top. No blur is used, so it draws the same on every phone. The bottom {@code lipDp} of the bounds is kept for the lip.
 */
public final class Keycap extends Drawable {
    private final float d;
    private final float radiusDp;
    private final int top, bottom, pressTop, pressBottom, lipColor;
    private final float lipDp;
    private final boolean radial;
    private final int hairline;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean pressed;

    private Keycap(float density, float radiusDp, int top, int bottom, int lipColor, float lipDp, boolean radial, int hairline) {
        this.d = density;
        this.radiusDp = radiusDp;
        this.top = top;
        this.bottom = bottom;
        this.pressTop = shade(top, 0.78f);
        this.pressBottom = shade(bottom, 0.88f);
        this.lipColor = lipColor;
        this.lipDp = lipDp;
        this.radial = radial;
        this.hairline = hairline;
    }

    /** A hardware-style key (round keys, volume bar): green, light on top. */
    public static Keycap hard(float density, float radiusDp) {
        return new Keycap(density, radiusDp, 0xFF2A5844, 0xFF1A3F2F, 0xFF040C07, 3f, false, 0x00000000);
    }

    /** A printed app key: dark, with a gold line inside. */
    public static Keycap tile(float density, float radiusDp) {
        return new Keycap(density, radiusDp, 0xFF132B1F, 0xFF0D2016, 0xFF030A06, 3f, false, 0x2AC9A44C);
    }

    /** The red power key (round, lit from above). */
    public static Keycap power(float density) {
        return new Keycap(density, 100f, 0xFFE0675A, 0xFF7A1F18, 0xFF2A0805, 3f, true, 0x00000000);
    }

    private static int shade(int c, float f) {
        int r = (int) (((c >> 16) & 0xFF) * f), g = (int) (((c >> 8) & 0xFF) * f), b = (int) ((c & 0xFF) * f);
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    @Override public boolean isStateful() { return true; }

    @Override protected boolean onStateChange(int[] state) {
        boolean now = false;
        for (int s : state) if (s == android.R.attr.state_pressed) now = true;
        if (now == pressed) return false;
        pressed = now;
        invalidateSelf();
        return true;
    }

    @Override public void draw(Canvas c) {
        android.graphics.Rect b = getBounds();
        float lip = lipDp * d;
        RectF base = new RectF(b.left, b.top, b.right, b.bottom - lip);
        float r = Math.min(radiusDp * d, Math.min(base.width(), base.height()) / 2f);
        RectF lipRect = new RectF(base);
        lipRect.offset(0, lip);
        RectF face = new RectF(base);
        if (pressed) face.offset(0, 2f * d);

        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(lipColor);
        c.drawRoundRect(lipRect, r, r, p);

        int t = pressed ? pressTop : top, bt = pressed ? pressBottom : bottom;
        if (radial) {
            float rad = Math.max(face.width(), face.height()) * 0.8f;
            p.setShader(new RadialGradient(face.centerX(), face.top + face.height() * 0.28f, rad, new int[] {t, shade(t, 0.78f), bt}, new float[] {0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        } else {
            p.setShader(new LinearGradient(0, face.top, 0, face.bottom, t, bt, Shader.TileMode.CLAMP));
        }
        c.drawRoundRect(face, r, r, p);
        p.setShader(null);

        c.save();
        android.graphics.Path clip = new android.graphics.Path();
        clip.addRoundRect(face, r, r, android.graphics.Path.Direction.CW);
        c.clipPath(clip);
        // shade toward the bottom (a soft roll-off, as on a rubber key)
        p.setShader(new LinearGradient(0, face.top + face.height() * 0.55f, 0, face.bottom, 0x00000000, pressed ? 0x22000000 : 0x55000000, Shader.TileMode.CLAMP));
        c.drawRect(face, p);
        p.setShader(null);
        if (pressed) {
            p.setShader(new LinearGradient(0, face.top, 0, face.top + 9f * d, 0x99000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawRect(face, p);
            p.setShader(null);
        }
        c.restore();

        p.setStyle(Paint.Style.STROKE);
        if (!pressed) {
            // light along the top edge only
            c.save();
            c.clipRect(face.left, face.top, face.right, face.top + 3.2f * d);
            p.setStrokeWidth(2.2f * d);
            p.setColor(0x44F6EFE0);
            RectF e = new RectF(face);
            e.inset(1.1f * d, 1.1f * d);
            c.drawRoundRect(e, Math.max(0, r - 1.1f * d), Math.max(0, r - 1.1f * d), p);
            c.restore();
        }
        if ((hairline >>> 24) != 0) {
            p.setStrokeWidth(1f * d);
            p.setColor(hairline);
            RectF e = new RectF(face);
            e.inset(1.5f * d, 1.5f * d);
            c.drawRoundRect(e, Math.max(0, r - 1.5f * d), Math.max(0, r - 1.5f * d), p);
        }
        p.setStrokeWidth(1f * d);
        p.setColor(0xFF061109);
        c.drawRoundRect(face, r, r, p);   // the thin dark rim
        p.setStyle(Paint.Style.FILL);
    }

    @Override public void setAlpha(int a) { p.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

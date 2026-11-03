package ma.chouitv.installer.remote;

import android.content.Context;
import android.graphics.PorterDuff;
import android.widget.ImageView;

import ma.chouitv.installer.R;

/** The line icons of the remote (Tabler, MIT licence): one family, one stroke, tinted at will. They are vector drawables of the app, so they stay sharp at any size. */
public final class RcIcon extends ImageView {
    public static final int POWER = 0, BACK = 1, HOME = 2, MUTE = 3, VOL_DOWN = 4, VOL_UP = 5, BOOK = 6, TV = 7, APPS = 8, MOON = 9, KEYBOARD = 10, GEAR = 11,
        UP = 12, DOWN = 13, LEFT = 14, RIGHT = 15, MORE = 16, REMOTE = 17, DOTS = 18;

    private static final int[] RES = {
        R.drawable.ti_power, R.drawable.ti_arrow_back_up, R.drawable.ti_home, R.drawable.ti_volume_off, R.drawable.ti_minus, R.drawable.ti_plus,
        R.drawable.ti_book, R.drawable.ti_device_tv, R.drawable.ti_apps, R.drawable.ti_moon, R.drawable.ti_keyboard, R.drawable.ti_settings,
        R.drawable.ti_chevron_up, R.drawable.ti_chevron_down, R.drawable.ti_chevron_left, R.drawable.ti_chevron_right, R.drawable.ti_dots_vertical,
        R.drawable.ti_device_remote, R.drawable.ti_dots};

    public RcIcon(Context c, int kind, int color) {
        super(c);
        setImageResource(RES[kind]);
        setColorFilter(color, PorterDuff.Mode.SRC_IN);
        setScaleType(ScaleType.FIT_CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
}

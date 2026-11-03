import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Runs on the TV as the shell user (app_process), pushed by the phone app: tells the name and the icon of apps, as the TV shows them
 * (so a package like com.drama.mp4 appears as «Oscar TV»). One line per package: package, name (UTF-8, base64) and a 96 px PNG icon (base64).
 *
 *   CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin AppInfo pkg1 pkg2 ...
 *
 * Built into the same jar as Clip by helper/build.sh.
 */
public class AppInfo {
    public static void main(String[] a) throws Exception {
        Looper.prepareMainLooper();
        Class<?> at = Class.forName("android.app.ActivityThread");
        Object thread = at.getMethod("systemMain").invoke(null);
        Context ctx = (Context) at.getMethod("getSystemContext").invoke(thread);
        PackageManager pm = ctx.getPackageManager();
        Base64.Encoder enc = Base64.getEncoder();
        for (String pkg : a) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                String label = String.valueOf(pm.getApplicationLabel(ai));
                String icon = "";
                try {
                    Drawable d = pm.getApplicationIcon(ai);
                    int s = 96;
                    Bitmap bm = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bm);
                    d.setBounds(0, 0, s, s);
                    d.draw(c);
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.PNG, 100, o);
                    icon = enc.encodeToString(o.toByteArray());
                } catch (Throwable ignored) { }
                System.out.println(pkg + "|" + enc.encodeToString(label.getBytes(StandardCharsets.UTF_8)) + "|" + icon);
            } catch (Throwable t) {
                System.out.println(pkg + "||");
            }
        }
        System.exit(0);
    }
}

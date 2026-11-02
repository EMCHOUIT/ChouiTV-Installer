import android.content.ClipData;
import android.os.IBinder;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Runs on the TV as the shell user (app_process), pushed by the phone app: puts UTF-8 text (given as base64) on the TV's clipboard,
 * so that the Paste key can type any alphabet (Arabic included) into the field that has the focus. «input text» can only type ASCII.
 *
 *   CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin Clip set BASE64
 *   CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin Clip get     (what is on the clipboard now, for tests)
 *
 * Built by helper/build.sh into app/src/main/assets/helper/clip.jar. Tried on an Android 11 TV (MiTV-MSSP3); the three- and four-argument
 * forms of setPrimaryClip (Android 11 and 12+) are both handled.
 */
public class Clip {
    public static void main(String[] a) throws Exception {
        IBinder b = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "clipboard");
        Object cb = Class.forName("android.content.IClipboard$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
        String pkg = "com.android.shell";
        if (a.length > 1 && a[0].equals("set")) {
            String text = new String(Base64.getDecoder().decode(a[1]), StandardCharsets.UTF_8);
            ClipData cd = ClipData.newPlainText("t", text);
            for (Method m : cb.getClass().getMethods()) {
                if (!m.getName().equals("setPrimaryClip")) continue;
                Class<?>[] p = m.getParameterTypes();
                Object[] args = new Object[p.length];
                args[0] = cd;
                args[1] = pkg;
                if (p.length == 3) args[2] = 0;
                if (p.length == 4) { args[2] = null; args[3] = 0; }
                m.invoke(cb, args);
                System.out.println("set via " + p.length + " params");
                return;
            }
            System.out.println("no setPrimaryClip");
        } else {
            for (Method m : cb.getClass().getMethods()) {
                if (!m.getName().equals("getPrimaryClip")) continue;
                Class<?>[] p = m.getParameterTypes();
                Object[] args = new Object[p.length];
                args[0] = pkg;
                if (p.length == 2) args[1] = 0;
                if (p.length == 3) { args[1] = null; args[2] = 0; }
                ClipData cd = (ClipData) m.invoke(cb, args);
                System.out.println("clip=" + (cd == null ? "null" : cd.getItemAt(0).getText()));
                return;
            }
            System.out.println("no getPrimaryClip");
        }
    }
}

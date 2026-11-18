import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

/**
 * Runs on the TV as the shell user (app_process), kept alive by the phone app: reads one Android key code per line on its standard input
 * and injects the key (a press, then a release), the way «input keyevent» does, but without starting a new program for each key.
 * That start costs about a second on a TV; here a key costs a few milliseconds.
 *
 *   CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin Keys
 *
 * It prints «ready» when it can inject, then «ok» (or «err …») for every line it reads. It stops when its input ends, that is when the phone
 * closes the link or goes away, so it never stays behind. Built by helper/build.sh into app/src/main/assets/helper/clip.jar.
 * Tried on an Android 11 TV (MiTV-MSSP3).
 */
public class Keys {
    public static void main(String[] a) throws Exception {
        InputManager im;
        Method inject;
        try {
            im = (InputManager) InputManager.class.getMethod("getInstance").invoke(null);
            inject = InputManager.class.getMethod("injectInputEvent", InputEvent.class, int.class);
        } catch (Throwable e) {
            System.out.println("err no input manager: " + e);
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("ready");
        System.out.flush();
        String line;
        while ((line = r.readLine()) != null) {
            String answer;
            try {
                int code = Integer.parseInt(line.trim());
                long now = SystemClock.uptimeMillis();
                KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
                KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
                // 0 = INJECT_INPUT_EVENT_MODE_ASYNC: the answer does not wait for the app that gets the key
                boolean d = (Boolean) inject.invoke(im, down, 0);
                boolean u = (Boolean) inject.invoke(im, up, 0);
                answer = d && u ? "ok" : "err refused";
            } catch (Throwable e) {
                answer = "err " + e;
            }
            System.out.println(answer);
            System.out.flush();
        }
    }
}

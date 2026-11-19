import android.app.UiAutomation;
import android.content.ClipData;
import android.hardware.input.InputManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Runs on the TV as the shell user (app_process), kept alive by the phone app. It reads one command per line on its standard input:
 *
 *   19              an Android key code: the key is injected (a press, then a release), the way «input keyevent» does, but without starting
 *                   a new program for each key. That start costs about a second on a TV; here a key costs a few milliseconds.
 *   T BASE64        a text (UTF-8, in base64): put on the TV's clipboard, then the Paste key is pressed, so any alphabet (Arabic included)
 *                   is typed into the field that has the focus. «input text» can only type ASCII.
 *   A               Select all (Ctrl+A), then Delete: the whole field that has the focus is emptied, whoever typed what is in it.
 *   R               Read the text of the field that has the input focus: «ok text BASE64», «ok text» (empty) or «ok none». About 0.35 s the first
 *                   time (the accessibility connection is made), then about 0.12 s; «uiautomator dump» needs about 5 s.
 *
 *   CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin Keys
 *
 * It prints «ready» when it can inject, then «ok» (or «err …») for every line it reads. It stops when its input ends, that is when the phone
 * closes the link or goes away, so it never stays behind. Built by helper/build.sh into app/src/main/assets/helper/clip.jar.
 * Tried on an Android 11 TV (MiTV-MSSP3).
 */
public class Keys {
    private static InputManager im;
    private static Method inject;
    private static Object clipboard;
    private static Method setClip;
    private static UiAutomation automation;   // the accessibility connection, made on the first read and kept: a read then takes about a tenth of a second

    /**
     * @param mode 0 = INJECT_INPUT_EVENT_MODE_ASYNC (the answer does not wait for the app that gets the key), 2 = WAIT_FOR_FINISH (it waits until the app has handled it)
     * @param meta the modifier keys held (0x3000 = Ctrl)
     */
    private static boolean key(int code, int meta, int mode) throws Exception {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, meta, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        boolean d = (Boolean) inject.invoke(im, down, mode);
        boolean u = (Boolean) inject.invoke(im, up, mode);
        return d && u;
    }

    /** The clipboard, the way Clip.java reaches it; null when this TV does not allow it (the keys still work). */
    private static void findClipboard() {
        try {
            IBinder b = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "clipboard");
            clipboard = Class.forName("android.content.IClipboard$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
            for (Method m : clipboard.getClass().getMethods()) if (m.getName().equals("setPrimaryClip")) setClip = m;
        } catch (Throwable e) {
            setClip = null;
        }
    }

    private static String type(String base64) throws Exception {
        if (setClip == null) return "err no clipboard";
        String text = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        Class<?>[] p = setClip.getParameterTypes();   // 3 parameters on Android 11, 4 on 12 and later
        Object[] args = new Object[p.length];
        args[0] = ClipData.newPlainText("t", text);
        args[1] = "com.android.shell";
        if (p.length == 3) args[2] = 0;
        if (p.length == 4) { args[2] = null; args[3] = 0; }
        setClip.invoke(clipboard, args);
        // 279 = the Paste key. The answer waits until the app has pasted: the next text replaces the clipboard, and an app that had not read it yet
        // would paste the wrong one (tried: typed letter by letter, «hello world» came out as «hloodddddd» with the asynchronous mode)
        return key(279, 0, 2) ? "ok" : "err refused";
    }

    /** The text of the field that has the input focus: «ok text BASE64» (or «ok text» when empty), «ok none» when there is none, it is a password, or this TV does not allow it. Never an error: a read must not take the link down. */
    private static String field() {
        try {
            if (automation == null) {
                HandlerThread ht = new HandlerThread("accessibility");
                ht.start();
                Object connection = Class.forName("android.app.UiAutomationConnection").newInstance();
                Constructor<UiAutomation> c = UiAutomation.class.getDeclaredConstructor(android.os.Looper.class, Class.forName("android.app.IUiAutomationConnection"));
                c.setAccessible(true);
                UiAutomation ua = c.newInstance(ht.getLooper(), connection);
                Method connect = UiAutomation.class.getDeclaredMethod("connect");
                connect.setAccessible(true);
                connect.invoke(ua);
                automation = ua;
            }
            AccessibilityNodeInfo root = automation.getRootInActiveWindow();
            AccessibilityNodeInfo f = root == null ? null : root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (f == null || !f.isEditable() || f.isPassword()) return "ok none";   // the view with the focus may be a button: only a text field counts
            CharSequence t = f.isShowingHintText() ? "" : f.getText();   // the hint of an empty field is not its text
            String s = t == null ? "" : t.toString();
            return s.isEmpty() ? "ok text" : "ok text " + Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            automation = null;   // tried again next time
            return "ok none";
        }
    }

    public static void main(String[] a) throws Exception {
        try {
            im = (InputManager) InputManager.class.getMethod("getInstance").invoke(null);
            inject = InputManager.class.getMethod("injectInputEvent", InputEvent.class, int.class);
        } catch (Throwable e) {
            System.out.println("err no input manager: " + e);
            return;
        }
        findClipboard();
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("ready");
        System.out.flush();
        String line;
        while ((line = r.readLine()) != null) {
            String answer;
            try {
                if (line.startsWith("T ")) answer = type(line.substring(2).trim());
                else if (line.trim().equals("R")) answer = field();
                else if (line.trim().equals("A")) answer = key(29, 0x3000, 2) && key(67, 0, 0) ? "ok" : "err refused";   // Ctrl+A, then Delete: the whole field
                else answer = key(Integer.parseInt(line.trim()), 0, 0) ? "ok" : "err refused";
            } catch (Throwable e) {
                answer = "err " + e;
            }
            System.out.println(answer);
            System.out.flush();
        }
    }
}

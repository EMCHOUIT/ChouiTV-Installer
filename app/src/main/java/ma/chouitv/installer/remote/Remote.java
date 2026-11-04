package ma.chouitv.installer.remote;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import ma.chouitv.installer.cleanup.Guard;
import ma.chouitv.installer.cleanup.Shell;

/**
 * The remote control: the commands the phone sends to the TV, built and checked here so that they can be tested without a TV.
 * Keys go through «input keyevent»; the buttons that open something start a known component; text of any alphabet goes onto the TV's
 * clipboard (a small program pushed by the phone, see helper/Clip.java) and is typed with the Paste key, because «input text» can only
 * type ASCII.
 */
public final class Remote {
    private Remote() {}

    public static final int UP = 19, DOWN = 20, LEFT = 21, RIGHT = 22, OK = 23, BACK = 4, HOME = 3, MENU = 82, MUTE = 164,
        VOLUME_UP = 24, VOLUME_DOWN = 25, POWER = 26, PASTE = 279;
    private static final int[] KNOWN = {UP, DOWN, LEFT, RIGHT, OK, BACK, HOME, MENU, MUTE, VOLUME_UP, VOLUME_DOWN, POWER, PASTE};

    /** Where the helper is put on the TV, and what it is called inside the phone app. */
    public static final String HELPER_REMOTE = "/data/local/tmp/chouitv-clip.jar";
    public static final String HELPER_ASSET = "helper/clip.jar";
    public static final int MAX_TEXT = 500;

    public enum Target {
        HOME("ma.chouitv.home/.MainActivity"),
        LISTEN("ma.chouitv.home/.tilawah.MainActivity"),
        CHANNELS("com.majnioui.tv.channels/.MainActivity"),
        SAVER("com.android.systemui/.Somnambulator");   // the system starts the screensaver the TV is set to use

        public final String component;
        Target(String component) { this.component = component; }

        /** The package to look for before offering to open it (null for the system). */
        public String pkg() { return this == SAVER ? null : component.substring(0, component.indexOf('/')); }
    }

    public enum Started { OK, NOT_OPENABLE }

    /** Opens the component and reads the answer: a TV whose ChouiTV does not open that screen to other apps (before 4.2) says so, and nothing else is claimed. */
    public static Started start(Shell tv, Target t) throws IOException {
        String out = tv.run(open(t));
        if (out != null && (out.contains("SecurityException") || out.contains("Error:") || out.contains("does not exist"))) return Started.NOT_OPENABLE;
        return Started.OK;
    }

    public static String key(int code) {
        for (int k : KNOWN) if (k == code) return "input keyevent " + code;
        throw new IllegalArgumentException("not a remote key: " + code);
    }

    public static String open(Target t) { return "am start -n " + t.component; }

    /** Starts an app of the TV from its package, through its TV launcher entry (no need to know the activity). */
    public static String launch(String pkg) {
        if (!Guard.validPackage(pkg)) throw new IllegalArgumentException("not a package name: " + pkg);
        return "monkey -p " + pkg + " -c android.intent.category.LEANBACK_LAUNCHER 1";
    }

    /** The helper's command that tells the real name and the icon of these apps, as the TV shows them. */
    public static String appInfo(java.util.List<String> pkgs) {
        StringBuilder b = new StringBuilder("CLASSPATH=" + HELPER_REMOTE + " app_process /system/bin AppInfo");
        for (String p : pkgs) {
            if (!Guard.validPackage(p)) throw new IllegalArgumentException("not a package name: " + p);
            b.append(' ').append(p);
        }
        return b.toString();
    }

    /** What a TV prints for its serial number (or, failing that, the identifier of its system), to recognise it again if its address changes. */
    public static String serialCommand() { return "getprop ro.serialno; getprop ro.boot.serialno; settings get secure android_id"; }

    /** The first usable line of {@link #serialCommand()}: not empty, not «unknown», not «null», not the placeholder some TVs print. */
    public static String parseSerial(String out) {
        if (out == null) return "";
        for (String line : out.split("\\r?\\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.equalsIgnoreCase("unknown") || l.equalsIgnoreCase("null") || l.equals("0") || l.equalsIgnoreCase("0123456789ABCDEF")) continue;
            return l;
        }
        return "";
    }

    /** The settings of the TV. */
    public static String settings() { return "am start -a android.settings.SETTINGS"; }

    /** Types {@code text} into the field that has the focus on the TV. Throws when the TV refuses the clipboard: nothing is pasted then. */
    public static void sendText(Shell tv, String text) throws IOException {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException("nothing to send");
        if (text.length() > MAX_TEXT) throw new IllegalArgumentException("too long: " + text.length() + " characters (at most " + MAX_TEXT + ")");
        String b64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        String answer = tv.run("CLASSPATH=" + HELPER_REMOTE + " app_process /system/bin Clip set " + b64);
        if (answer == null || !answer.contains("set via")) throw new IOException("the TV did not take the text: " + (answer == null ? "" : answer.trim()));
        tv.run(key(PASTE));
    }

    /** True when «pm list packages» lists exactly this package (not one that merely starts with its name). */
    public static boolean installed(Shell tv, String pkg) throws IOException {
        if (!Guard.validPackage(pkg)) throw new IllegalArgumentException("not a package name: " + pkg);
        return hasPackage(tv.run("pm list packages " + pkg), pkg);
    }

    /** True when the answer of «pm list packages» holds exactly this package (not one that merely starts with its name). */
    public static boolean hasPackage(String out, String pkg) {
        if (out == null || pkg == null) return false;
        for (String line : out.split("\\r?\\n")) if (line.trim().equals("package:" + pkg)) return true;
        return false;
    }

    /** The shortcut opens an app the TV does not have (an empty answer gives no verdict, and the system screens are always there). */
    public static boolean missing(String pmListOutput, Target t) {
        String pkg = t.pkg();
        if (pkg == null || pmListOutput == null || pmListOutput.trim().isEmpty()) return false;
        return !hasPackage(pmListOutput, pkg);
    }
}

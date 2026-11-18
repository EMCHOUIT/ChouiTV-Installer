package ma.chouitv.installer.remote;

/**
 * What the phone and the small program kept running on the TV (helper/Keys.java) say to each other. The phone starts it once
 * ({@link #command()}); it prints «ready», then for each line it reads, a key code, it injects the key and answers «ok» (or «err …»).
 * Starting «input keyevent» for every key costs about a second on a TV; this costs a few milliseconds.
 */
public final class KeyProtocol {
    private KeyProtocol() {}

    /** The line to send for a key: only the keys of the remote. */
    public static String line(int code) {
        if (!Remote.isKnown(code)) throw new IllegalArgumentException("not a remote key: " + code);
        return code + "\n";
    }

    /** True for «ok» alone or followed by details («ok 3ms»); not for «okay», nor for an error. */
    public static boolean ok(String answer) {
        if (answer == null) return false;
        String a = answer.trim();
        return a.equals("ok") || a.startsWith("ok ");
    }

    /** True when the program says it can inject keys. */
    public static boolean ready(String firstLine) {
        return firstLine != null && firstLine.trim().equals("ready");
    }

    /** The command that starts the program on the TV (the helper jar is pushed there first). */
    public static String command() {
        return "CLASSPATH=" + Remote.HELPER_REMOTE + " app_process /system/bin Keys";
    }
}

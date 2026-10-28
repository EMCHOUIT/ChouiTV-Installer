package ma.chouitv.installer.cleanup;

import java.io.IOException;
import java.util.List;

/**
 * The safety net on the TV for a cleanup. A small script runs ON the TV: unless the phone confirms within the time limit (Wi-Fi
 * lost, phone off), it turns every package of the cleanup back on. The script text is fixed; only the package list and the token change,
 * and each package goes through {@link Guard}.
 */
public final class Deadman {
    private Deadman() {}

    public static final String DIR = "/data/local/tmp";
    public static final int LIMIT_SECONDS = 300;

    public static String scriptPath(String token) { return DIR + "/chouitv-cleanup-deadman-" + token + ".sh"; }
    public static String okPath(String token) { return DIR + "/chouitv-cleanup-ok-" + token; }

    /** The script: waits for the confirmation file, then re-enables the packages. */
    public static String script(String token, List<String> packages, int limit) {
        if (!token.matches("[0-9a-f]{16}")) throw new IllegalArgumentException("bad token");
        StringBuilder b = new StringBuilder("#!/system/bin/sh\n");
        b.append("i=0\n");
        b.append("while [ $i -lt ").append(limit).append(" ]; do\n");
        b.append("  [ -f ").append(okPath(token)).append(" ] && exit 0\n");
        b.append("  sleep 1\n  i=$((i + 1))\ndone\n");
        for (String p : packages) b.append(Guard.enable(p)).append("\n");
        b.append("input keyevent KEYCODE_HOME\n");
        return b.toString();
    }

    /** The one command that starts the script in the background. */
    public static String launch(String token, int limit) {
        if (!token.matches("[0-9a-f]{16}")) throw new IllegalArgumentException("bad token");
        return "nohup sh " + scriptPath(token) + " > /dev/null 2>&1 &";
    }

    /** Puts the script on the TV (one printf per line: no file transfer needed) and starts it. */
    public static void arm(Shell tv, String token, List<String> packages) throws IOException {
        String path = scriptPath(token);
        tv.run("rm -f " + path + " " + okPath(token));
        for (String line : script(token, packages, LIMIT_SECONDS).split("\n")) {
            if (line.indexOf('\'') >= 0) throw new IllegalStateException("quote in the script");
            tv.run("echo '" + line + "' >> " + path);
        }
        tv.run(launch(token, LIMIT_SECONDS));
    }

    /** The phone is fine: the script ends without touching anything. */
    public static void confirm(Shell tv, String token) throws IOException {
        tv.run("touch " + okPath(token));
    }
}

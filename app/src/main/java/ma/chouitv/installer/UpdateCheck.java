package ma.chouitv.installer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Is there a newer ChouiTV Companion? The latest release of the project says; the versions are compared as numbers (1.10 is newer than 1.9). */
final class UpdateCheck {
    private UpdateCheck() {}

    static final String LATEST_URL = "https://api.github.com/repos/EMCHOUIT/ChouiTV-Installer/releases/latest";
    static final String DOWNLOAD_URL = "https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk";

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VERSION = Pattern.compile("^v?(\\d+(?:\\.\\d+)*)$");

    /** The tag of the release in the answer of GitHub, or null. */
    static String tagOf(String json) {
        if (json == null) return null;
        Matcher m = TAG.matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** True when {@code latest} is a readable version higher than {@code current}. */
    static boolean isNewer(String current, String latest) {
        int[] a = parts(current), b = parts(latest);
        if (a == null || b == null) return false;
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (y != x) return y > x;
        }
        return false;
    }

    private static int[] parts(String v) {
        if (v == null) return null;
        Matcher m = VERSION.matcher(v.trim());
        if (!m.matches()) return null;
        String[] p = m.group(1).split("\\.");
        int[] r = new int[p.length];
        for (int i = 0; i < p.length; i++) {
            try { r[i] = Integer.parseInt(p[i]); } catch (NumberFormatException e) { return null; }
        }
        return r;
    }
}

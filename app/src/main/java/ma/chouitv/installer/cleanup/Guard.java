package ma.chouitv.installer.cleanup;

import java.util.regex.Pattern;

/**
 * The one door for every command that changes packages on the TV. A package name is checked against a strict pattern and a
 * command that changes packages may not share a line with another command: whatever a list or a bug hands in, the shell gets
 * exactly one «pm» call, never a chain.
 */
public final class Guard {
    private Guard() {}

    /** A reverse-domain package name: letters, digits and underscores in dotted parts, at least two parts. */
    private static final Pattern PKG = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$");
    private static final Pattern PM_WRITE = Pattern.compile(
        "(?:^|[\\s;&|(])(?:pm|cmd\\s+package)\\s+(?:disable-user|disable|enable|default-state|install\\S*|uninstall|clear|grant|revoke|set-home-activity)\\b");
    private static final Pattern CHAIN = Pattern.compile("[;|&\\n\\r`]|\\$\\(");

    public static boolean validPackage(String p) {
        return p != null && p.length() <= 200 && PKG.matcher(p).matches();
    }

    private static String checked(String p) {
        if (!validPackage(p)) throw new IllegalArgumentException("not a package name: " + p);
        return p;
    }

    /** The command that turns a package off for the main user (it stays installed). */
    public static String disable(String pkg) { return "pm disable-user --user 0 " + checked(pkg); }

    /** The command that turns it back on. */
    public static String enable(String pkg) { return "pm enable " + checked(pkg); }

    /** Why a line may not run, or null when it may. */
    public static String refusal(String line) {
        if (line == null) return "empty command";
        String plain = line.replace("'", "");
        if (!PM_WRITE.matcher(plain).find()) return null;
        if (CHAIN.matcher(line).find()) return "a package change may not share a line with another command";
        return null;
    }
}

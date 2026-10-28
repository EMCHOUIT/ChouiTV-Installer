package ma.chouitv.installer.cleanup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turning apps back on, from what the TV says is off right now (not from a file on one phone: any phone can do it). The apps a cleanup
 * turned off come first; the others are offered with a warning. Home screens and setup wizards are never offered: turning them back on
 * would undo the switch to ChouiTV's home.
 */
public final class Reactivation {
    private Reactivation() {}

    public static final class Plan {
        /** Off now and named by a restore point: what a ChouiTV cleanup turned off. */
        public final List<String> ours = new ArrayList<>();
        /** Off now for another reason: the manufacturer, the person, another tool. */
        public final List<String> others = new ArrayList<>();
        public int total() { return ours.size() + others.size(); }
    }

    private static final Pattern RESTORE_LINE = Pattern.compile("^\\s*pm enable\\s+(\\S+)\\s*$");

    /** The packages of a restore point's plain-text copy (the lines «pm enable pkg» kept on the TV). */
    public static Set<String> parseRestoreText(String text) {
        Set<String> s = new HashSet<>();
        if (text == null) return s;
        for (String line : text.split("\n")) {
            Matcher m = RESTORE_LINE.matcher(line.replace("\r", ""));
            if (m.matches() && Guard.validPackage(m.group(1))) s.add(m.group(1));
        }
        return s;
    }

    /** Which of the apps that are off may be offered, and in which list. */
    public static Plan plan(Set<String> disabled, Set<String> ours, CleanupData d) {
        Plan p = new Plan();
        for (String pkg : disabled) {
            if (!Guard.validPackage(pkg)) continue;
            if (d != null && d.homes.contains(pkg)) continue;
            if (Planner.NOT_FOR_US.matcher(pkg).find()) continue;
            (ours.contains(pkg) ? p.ours : p.others).add(pkg);
        }
        Collections.sort(p.ours);
        Collections.sort(p.others);
        return p;
    }

    /** The packages that are off right now («pm list packages -d»). */
    public static Set<String> disabledNow(Shell tv) throws IOException {
        return Facts.packages(tv.run("pm list packages -d"));
    }

    /** Turns the given apps back on and answers with the ones that really are on now: each one is checked, not assumed. */
    public static List<String> apply(Shell tv, List<String> pkgs) throws IOException {
        for (String p : pkgs) tv.run(Guard.enable(p));
        Set<String> off = disabledNow(tv);
        List<String> back = new ArrayList<>();
        for (String p : pkgs) if (!off.contains(p)) back.add(p);
        return back;
    }
}

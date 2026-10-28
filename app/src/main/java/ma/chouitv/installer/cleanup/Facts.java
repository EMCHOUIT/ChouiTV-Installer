package ma.chouitv.installer.cleanup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the TV says about the apps its owner installed: which ones start with the TV, how much memory they hold, when they were last
 * used. Pure parsers of «adb shell» outputs, so they are tested on real outputs; any of them may be missing on a given TV, and the
 * plan then simply recommends less.
 */
public final class Facts {
    /** The apps the person installed («pm list packages -3»), including the ones already off. */
    public final Set<String> user = new HashSet<>();
    /** Apps with a receiver for «boot completed»: they wake up with the TV. */
    public final Set<String> autostart = new HashSet<>();
    /** Memory held right now, in MB, by package (all its processes). */
    public final Map<String, Integer> ramMb = new HashMap<>();
    /** Days since the app was last on screen, by package, from the usage statistics; absent when the app has no entry. */
    public final Map<String, Integer> daysSinceUse = new HashMap<>();
    /** True when the TV gave usable usage statistics: only then is «not used lately» a fact and not a guess. */
    public boolean usageKnown;
    /** Apps the TV needs as they are: the keyboard, the accessibility services, the home screens. */
    public final Set<String> sensitive = new HashSet<>();

    public static final int LONG_UNUSED_DAYS = 30;

    private static final Pattern PKG_LINE = Pattern.compile("^package:(\\S+)$");
    private static final Pattern RECEIVER = Pattern.compile("^\\s+([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)/\\S+\\s*$");
    private static final Pattern PSS = Pattern.compile("^\\s+([\\d.,]+)K:\\s+(\\S+?)(?::\\S+)?\\s+\\(pid\\b");
    private static final Pattern USAGE = Pattern.compile("package=(\\S+)\\s.*?lastTimeUsed=\"(\\d{4})-(\\d\\d)-(\\d\\d)[^\"]*\".*?lastTimeVisible=\"(\\d{4})-(\\d\\d)-(\\d\\d)");

    public static Set<String> packages(String out) {
        Set<String> s = new HashSet<>();
        if (out == null) return s;
        for (String line : out.split("\n")) {
            Matcher m = PKG_LINE.matcher(line.trim());
            if (m.matches()) s.add(m.group(1));
        }
        return s;
    }

    /** Packages named by lines like «com.pkg/.Receiver» (the result of «cmd package query-receivers --brief»). */
    public static Set<String> componentPackages(String out) {
        Set<String> s = new HashSet<>();
        if (out == null) return s;
        for (String line : out.split("\n")) {
            Matcher m = RECEIVER.matcher(line);
            if (m.matches()) s.add(m.group(1));
        }
        return s;
    }

    /** «ime list -s» prints «pkg/.Service» per line; the accessibility setting is «pkg/.A:pkg2/.B» or «null». */
    public static Set<String> componentList(String out) {
        Set<String> s = new HashSet<>();
        if (out == null) return s;
        for (String part : out.trim().split("[\\n:]")) {
            int i = part.indexOf('/');
            String p = i > 0 ? part.substring(0, i).trim() : "";
            if (!p.isEmpty() && Guard.validPackage(p)) s.add(p);
        }
        return s;
    }

    /** The «Total PSS by process» section of «dumpsys meminfo»: «  137,872K: pkg (pid 1 / activities)». */
    public static Map<String, Integer> memory(String out) {
        Map<String, Integer> kb = new HashMap<>();
        if (out == null) return new HashMap<>();
        boolean in = false;
        for (String line : out.split("\n")) {
            if (line.startsWith("Total PSS by process")) { in = true; continue; }
            if (in && line.trim().isEmpty()) break;
            if (!in) continue;
            Matcher m = PSS.matcher(line);
            if (!m.find()) continue;
            int k = Integer.parseInt(m.group(1).replaceAll("[.,]", ""));
            kb.merge(m.group(2), k, Integer::sum);
        }
        Map<String, Integer> mb = new HashMap<>();
        for (Map.Entry<String, Integer> e : kb.entrySet()) mb.put(e.getKey(), Math.round(e.getValue() / 1024f));
        return mb;
    }

    /**
     * The usage statistics: lines «package=pkg totalTimeUsed=".." lastTimeUsed="2026-10-27 16:01:38" ... lastTimeVisible="…"». The
     * latest date of an app wins; the epoch (1970) means never. {@code today} is the TV's own date, as days since 1970.
     */
    public void usage(String out, long todayDays) {
        daysSinceUse.clear();
        usageKnown = false;
        if (out == null) return;
        Map<String, Long> last = new HashMap<>();
        for (String line : out.split("\n")) {
            Matcher m = USAGE.matcher(line);
            if (!m.find()) continue;
            long used = days(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
            long seen = days(Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)), Integer.parseInt(m.group(7)));
            long d = Math.max(used, seen);
            if (d > days(2000, 1, 1)) { usageKnown = true; last.merge(m.group(1), d, Math::max); }
            else last.putIfAbsent(m.group(1), 0L);
        }
        if (!usageKnown) return;
        for (Map.Entry<String, Long> e : last.entrySet()) {
            if (e.getValue() > 0) daysSinceUse.put(e.getKey(), (int) Math.max(0, todayDays - e.getValue()));
        }
    }

    /** Days since 1970-01-01 for a civil date (proleptic Gregorian; no time zone). */
    public static long days(int y, int m, int d) {
        y -= m <= 2 ? 1 : 0;
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (m + (m > 2 ? -3 : 9)) + 2) / 5 + d - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }
}

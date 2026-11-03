package ma.chouitv.installer.remote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The volume of the TV, built and read as commands so that it can be tested without a TV. TVs have very different scales (15, 30, 100 levels...), so the
 * maximum is read from the TV and every level is between 0 and that maximum. A key press over adb takes more than a second, so the level is set directly
 * with «cmd media_session» (about 0.1 s); when the TV does not follow that command, the keys go in one batch.
 */
public final class Volume {
    private Volume() {}

    /** The scale used until the TV has told its own, and the longest batch of keys. */
    public static final int DEFAULT_MAX = 100, MAX_KEYS = 30;

    /** The level of the music stream, as the TV keeps it. Falls back on the dump when the setting is empty. */
    public static final String READ = "settings get system volume_music_speaker";
    public static final String READ_DUMP = "dumpsys audio | grep -A14 STREAM_MUSIC | grep streamVolume | head -1";
    /** The highest level of the music stream (the number of steps of this TV). */
    public static final String READ_MAX = "dumpsys audio | grep -A14 STREAM_MUSIC | grep 'Max:' | head -1";

    private static final Pattern LEVEL = Pattern.compile("(?:streamVolume:)?\\s*(\\d{1,3})\\s*$");
    private static final Pattern MAXIMUM = Pattern.compile("Max:\\s*(\\d{1,3})");

    public static String set(int level, int max) {
        return "cmd media_session volume --stream 3 --set " + Math.max(0, Math.min(max, level));
    }

    public static String adj(boolean up) {
        return "cmd media_session volume --stream 3 --adj " + (up ? "raise" : "lower");
    }

    /** The level in the TV's answer, or -1 when there is none (null, «null», an error, a number above the maximum). */
    public static int parseLevel(String out, int max) {
        if (out == null) return -1;
        Matcher m = LEVEL.matcher(out.trim());
        if (!m.find()) return -1;
        int v = Integer.parseInt(m.group(1));
        return v <= max ? v : -1;
    }

    /** The maximum level in the TV's answer; the default scale when it is missing or absurd. */
    public static int parseMax(String out) {
        if (out == null) return DEFAULT_MAX;
        Matcher m = MAXIMUM.matcher(out);
        if (!m.find()) return DEFAULT_MAX;
        int v = Integer.parseInt(m.group(1));
        return v >= 2 && v <= 250 ? v : DEFAULT_MAX;
    }

    /** The level the family's limit stands for on this TV: {@code percent} of the maximum (0: no limit). */
    public static int limitLevel(int percent, int max) {
        if (percent <= 0) return 0;
        return Math.max(1, Math.min(max, Math.round(max * percent / 100f)));
    }

    /** {@code limit} is a level (0: none). */
    public static int capped(int level, int limit, int max) {
        int v = Math.max(0, Math.min(max, level));
        return limit > 0 ? Math.min(v, limit) : v;
    }

    public static boolean overLimit(int level, int limit) { return limit > 0 && level > limit; }

    /** True when the answer says the TV does not know the command (or printed its usage instead of doing it). */
    public static boolean failed(String out) {
        if (out == null) return false;
        String o = out.toLowerCase(java.util.Locale.ROOT);
        return o.contains("inaccessible or not found") || o.contains("no shell command") || o.contains("unknown command") || o.contains("exception occurred")
            || o.contains("usage:") || o.contains("error:") || o.contains("permission denied") || o.contains("securityexception");
    }

    /** The keys that bring the volume from {@code from} to {@code to} in one command (null: nothing to do or the start is unknown). */
    public static String keys(int from, int to) {
        if (from < 0 || from == to) return null;
        int n = Math.min(MAX_KEYS, Math.abs(to - from));
        StringBuilder b = new StringBuilder("input keyevent");
        for (int i = 0; i < n; i++) b.append(to > from ? " 24" : " 25");
        return b.toString();
    }

    /** What the mute key does: silent when there is sound, the remembered level (or a gentle one, a fifth of the scale) when it is silent. */
    public static int muteTarget(int current, int previous, int max) {
        if (current > 0) return 0;
        return previous > 0 ? previous : Math.max(1, max / 5);
    }

    /** Whether the direct command did something: the level moved, or it could not move (already at an end). */
    public static boolean moved(int before, int after, boolean up, int max) {
        if (before < 0 || after < 0) return false;
        if (after != before) return true;
        return up ? before >= max : before <= 0;
    }
}

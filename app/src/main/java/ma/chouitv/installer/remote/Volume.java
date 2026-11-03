package ma.chouitv.installer.remote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The volume of the TV, built and read as commands so that it can be tested without a TV. A TV has up to 100 levels and a key press over adb takes
 * more than a second, so the level is set directly with «cmd media_session» (about 0.1 s); if the TV does not know that command, the keys go in one batch.
 */
public final class Volume {
    private Volume() {}

    public static final int MAX = 100, DEFAULT_RETURN = 20, MAX_KEYS = 30;

    /** The level of the music stream, as the TV keeps it. Falls back on the dump when the setting is empty. */
    public static final String READ = "settings get system volume_music_speaker";
    public static final String READ_DUMP = "dumpsys audio | grep -A14 STREAM_MUSIC | grep streamVolume | head -1";

    private static final Pattern LEVEL = Pattern.compile("(?:streamVolume:)?\\s*(\\d{1,3})\\s*$");

    public static String set(int level) {
        return "cmd media_session volume --stream 3 --set " + Math.max(0, Math.min(MAX, level));
    }

    public static String adj(boolean up) {
        return "cmd media_session volume --stream 3 --adj " + (up ? "raise" : "lower");
    }

    /** The level in the TV's answer, or -1 when there is none (null, «null», an error, a number that is not a level). */
    public static int parseLevel(String out) {
        if (out == null) return -1;
        Matcher m = LEVEL.matcher(out.trim());
        if (!m.find()) return -1;
        int v = Integer.parseInt(m.group(1));
        return v <= MAX ? v : -1;
    }

    /** {@code limit} is the maximum level the family chose (0: none). */
    public static int capped(int level, int limit) {
        int v = Math.max(0, Math.min(MAX, level));
        return limit > 0 ? Math.min(v, limit) : v;
    }

    public static boolean overLimit(int level, int limit) { return limit > 0 && level > limit; }

    /** True when the answer says the TV does not know the command. */
    public static boolean failed(String out) {
        if (out == null) return false;
        return out.contains("inaccessible or not found") || out.contains("No shell command") || out.contains("Unknown command") || out.contains("Exception occurred");
    }

    /** The keys that bring the volume from {@code from} to {@code to} in one command (null: nothing to do or the start is unknown). */
    public static String keys(int from, int to) {
        if (from < 0 || from == to) return null;
        int n = Math.min(MAX_KEYS, Math.abs(to - from));
        StringBuilder b = new StringBuilder("input keyevent");
        for (int i = 0; i < n; i++) b.append(to > from ? " 24" : " 25");
        return b.toString();
    }

    /** What the mute key does: silent when there is sound, the remembered level when it is silent. */
    public static int muteTarget(int current, int previous) {
        if (current > 0) return 0;
        return previous > 0 ? previous : DEFAULT_RETURN;
    }
}

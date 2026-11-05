package ma.chouitv.installer.remote;

/** The comfort settings of the remote (repeat speed of a held key, size of the words on the keys). Pure, tested on the JVM (ComfortTest). */
public final class Comfort {
    private Comfort() {}

    public static final int DEFAULT_REPEAT = 1, DEFAULT_TEXT = 0;
    public static final int REPEAT_LEVELS = 3, TEXT_LEVELS = 3;
    /** A held key sends its first repeat after this long. */
    public static final int FIRST_REPEAT_MS = 400;

    /** A saved level, or {@code usual} when it is missing, unreadable or out of {@code 0..count-1}. */
    public static int level(String saved, int usual, int count) {
        if (saved == null || saved.isEmpty()) return usual;
        try {
            int v = Integer.parseInt(saved.trim());
            return v >= 0 && v < count ? v : usual;
        } catch (NumberFormatException e) { return usual; }
    }

    public static int next(int level, int count) { return (level + 1) % count; }

    /** The wait between two repeats of a held key. */
    public static int repeatMs(int level) { return level == 0 ? 200 : level == 2 ? 80 : 120; }

    public static float textScale(int level) { return level == 1 ? 1.25f : level == 2 ? 1.5f : 1.0f; }

    /** With bigger words the shortcut keys keep only the words (the icon would take the room). */
    public static boolean textOnly(int level) { return level == 1 || level == 2; }
}

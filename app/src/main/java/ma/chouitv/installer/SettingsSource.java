package ma.chouitv.installer;

/** Which settings a set-up sends: saved ones first (they are what the family already made), the city and the prayer by hand, or a file. Pure, tested on the JVM (SettingsSourceTest). */
final class SettingsSource {
    private SettingsSource() {}

    enum Mode { PROFILE, MANUAL, FILE }

    static Mode defaultMode(int savedCount, boolean fileChosen) {
        if (fileChosen) return Mode.FILE;
        return savedCount > 0 ? Mode.PROFILE : Mode.MANUAL;
    }

    static boolean showsFields(Mode m) { return m == Mode.MANUAL; }

    /** The saved setting chosen, else the newest (the first of the list); null when there is none. */
    static String pickProfile(String[] idsNewestFirst, String chosen) {
        if (idsNewestFirst == null || idsNewestFirst.length == 0) return null;
        for (String id : idsNewestFirst) if (id.equals(chosen)) return id;
        return idsNewestFirst[0];
    }
}

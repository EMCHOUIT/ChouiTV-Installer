package ma.chouitv.installer;

import java.util.ArrayList;
import java.util.List;

/** The success screen at the end of a set-up: what was done (the install, the settings sent, the apps turned off) and what can be undone. Pure, tested on the JVM (SetupSummaryTest). */
final class SetupSummary {
    private SetupSummary() {}

    enum Kind { INSTALLED, SETTINGS, APPS_OFF }

    static final class Line {
        final Kind kind;
        final String arg;
        Line(Kind kind, String arg) { this.kind = kind; this.arg = arg; }
    }

    /** {@code settingsSource}: what was sent (a saved setting's name, a city, a file), or null; {@code appsOff}: how many apps were turned off in this set-up. */
    static List<Line> lines(String settingsSource, int appsOff) {
        List<Line> l = new ArrayList<>();
        l.add(new Line(Kind.INSTALLED, ""));
        if (settingsSource != null && !settingsSource.trim().isEmpty()) l.add(new Line(Kind.SETTINGS, settingsSource.trim()));
        if (appsOff > 0) l.add(new Line(Kind.APPS_OFF, String.valueOf(appsOff)));
        return l;
    }

    static boolean canUndoSettings(String settingsSource) { return settingsSource != null && !settingsSource.trim().isEmpty(); }

    static boolean canUndoApps(int appsOff) { return appsOff > 0; }
}

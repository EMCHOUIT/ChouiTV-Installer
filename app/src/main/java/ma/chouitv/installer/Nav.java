package ma.chouitv.installer;

/** Where «back» goes from each screen. One mapping for the system Back key and for the visible «Retour» control. Pure, tested on the JVM (NavTest). */
final class Nav {
    private Nav() {}

    /** Back does nothing now (something runs). */
    static final int BLOCKED = -2;
    /** Back leaves the app (after a double press). */
    static final int EXIT = -1;
    static final int DOUBLE_PRESS_MS = 2000;

    /** The screen Back goes to, or BLOCKED / EXIT. */
    static int backTarget(int step, boolean busy, boolean guided, boolean cleanFromDone) {
        if (busy || step == 23) return BLOCKED;
        int list = guided ? 19 : 12;
        switch (step) {
            case 12: return 18;
            case 18: return 0;
            case 19: return 18;
            case 20: case 21: return 19;
            case 22: return 18;
            case 8: case 9: case 10: return cleanFromDone ? 5 : list;
            case 11: case 13: case 14: case 17: return list;
            case 15: return 17;
            case 24: return 18;
            case 1: return list;
            case 2: return 1;
            default: return EXIT;
        }
    }

    /** The visible control shows where Back goes somewhere safe: not on the first screen, not over a running install or cleanup, not where Back would leave the app. */
    static boolean showsBack(int step, boolean busy, boolean guided, boolean cleanFromDone) {
        if (step == 3 || step == 9) return false;
        return backTarget(step, busy, guided, cleanFromDone) >= 0;
    }

    /** A task opened from the home goes straight to the TV used last (no preparation screen, no scan) when the user is not on a guided path and a TV is known. */
    static boolean startsDirect(boolean guided, boolean lastTvKnown) { return !guided && lastTvKnown; }

    /** A second press of Back within two seconds of the first leaves the app. */
    static boolean exitNow(long lastPress, long now) {
        return lastPress > 0 && now - lastPress <= DOUBLE_PRESS_MS;
    }
}

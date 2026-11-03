package ma.chouitv.installer;

/** Where «back» goes from each screen. One mapping for the system Back key and for the visible «Retour» control. Pure, tested on the JVM (NavTest). */
final class Nav {
    private Nav() {}

    /** Back does nothing now (something runs). */
    static final int BLOCKED = -2;
    /** Back leaves the app (after a double press). */
    static final int EXIT = -1;
    static final int DOUBLE_PRESS_MS = 2000;
    static final int TAB_REMOTE = 0, TAB_TV = 1, TAB_MORE = 2;

    /** The tab a screen belongs to (the screens of a task in progress belong to none, and show no tab bar). */
    static int tabOf(int step) {
        switch (step) {
            case 25: return TAB_REMOTE;
            case 12: return TAB_TV;
            case 26: return TAB_MORE;
            default: return -1;
        }
    }

    static int tabScreen(int tab) { return tab == TAB_REMOTE ? 25 : tab == TAB_TV ? 12 : 26; }

    /** The screen Back goes to, or BLOCKED / EXIT. */
    static int backTarget(int step, boolean busy, boolean guided, boolean cleanFromDone) {
        if (busy || step == 23) return BLOCKED;
        int list = guided ? 19 : 12;
        switch (step) {
            case 12: return EXIT;   // a tab: the top of the app
            case 18: return 26;
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

    /** The language is asked on the very first use only; after that it is changed from «More». */
    static boolean asksLanguage(boolean languageSaved) { return !languageSaved; }

    /** The first screen when the app opens: the language (first use), «My TV» (a TV is known), else how to work (guided or free). */
    static int firstScreen(boolean languageSaved, boolean tvKnown) {
        if (asksLanguage(languageSaved)) return 0;
        return tvKnown ? 12 : 18;
    }

    /** A second press of Back within two seconds of the first leaves the app. */
    static boolean exitNow(long lastPress, long now) {
        return lastPress > 0 && now - lastPress <= DOUBLE_PRESS_MS;
    }
}

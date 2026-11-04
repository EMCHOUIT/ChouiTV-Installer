package ma.chouitv.installer;

/** Where «back» goes from each screen. One mapping for the system Back key and for the visible «Retour» control. Pure, tested on the JVM (NavTest). */
final class Nav {
    private Nav() {}

    /** Back does nothing now (something runs). */
    static final int BLOCKED = -2;
    /** Back leaves the app (after a double press). */
    static final int EXIT = -1;
    static final int DOUBLE_PRESS_MS = 2000;

    /** The screen Back goes to, or BLOCKED / EXIT. The remote (25) is the home of the app. */
    static int backTarget(int step, boolean busy, boolean guided, boolean cleanFromDone) {
        if (busy || step == 23) return BLOCKED;
        int list = guided ? 19 : 12;
        switch (step) {
            case 12: return 27;   // a TV's page goes back to «My TVs»
            case 27: return 25;   // «My TVs» and «Options» go back to the remote
            case 26: return 25;
            case 31: return 25;   // the preview of a set-up goes back to the remote
            case 32: return 26;   // the choice of layout goes back to «More»
            case 33: return 32;   // the editor goes back to the choice
            case 34: return 26;   // the comfort settings go back to «More»
            case 18: return 12;   // the old mode screen is no longer reachable; if it ever shows, it goes back to the TV's page
            case 29: case 30: return 12;   // the journal and the fix screen
            case 19: return 12;
            case 20: case 21: return 19;
            case 22: return 18;
            case 8: case 9: case 10: return cleanFromDone ? 5 : list;
            case 11: case 13: case 14: case 17: return list;
            case 15: return 17;
            case 24: return 18;
            case 1: return guided ? 19 : 25;   // the three steps: back to the guided list, else back to the remote (a demo when there is no TV)
            case 2: return guided ? 1 : 25;
            default: return EXIT;
        }
    }

    /** The visible control shows where Back goes somewhere safe: not on the first screen, not over a running install or cleanup, not where Back would leave the app. */
    static boolean showsBack(int step, boolean busy, boolean guided, boolean cleanFromDone) {
        if (step == 3 || step == 9) return false;
        if (step == 26 || step == 27) return false;   // roots of a tab: the bar is the way around
        return backTarget(step, busy, guided, cleanFromDone) >= 0;
    }

    /** A task chosen on a TV's page goes straight to that TV (no preparation screen, no scan) when the user is not on a guided path and a TV is known. */
    static boolean startsDirect(boolean guided, boolean lastTvKnown) { return !guided && lastTvKnown; }

    /** The language is asked on the very first use only; after that it is changed from «Options». */
    static boolean asksLanguage(boolean languageSaved) { return !languageSaved; }

    /** The first screen when the app opens: the language (first use), else always the remote (the TV's, or a demo when there is none). Never a waiting or preparation screen. */
    static int firstScreen(boolean languageSaved, boolean tvKnown) {
        return asksLanguage(languageSaved) ? 0 : 25;
    }

    /** The screens that can be visited with no TV at all: the language, the three steps and the search, the remote (demo), Options, «My TVs», the short opening screen. */
    static boolean opensWithoutTv(int step) {
        switch (step) {
            case 0: case 1: case 2: case 25: case 26: case 27: case 28: case 32: case 33: case 34: return true;
            default: return false;
        }
    }

    /** The tab a screen belongs to (0 the remote, 1 «My TVs», 2 «More»), or -1 where there is no bar (a set-up, a clean-up, the language). */
    static int tabOf(int step) {
        switch (step) {
            case 25: return 0;
            case 27: case 12: case 29: case 30: return 1;
            case 26: case 32: case 34: return 2;
            default: return -1;
        }
    }

    static boolean showsTabs(int step) { return tabOf(step) >= 0; }

    /** The first screen of a tab. */
    static int tabRoot(int tab) { return tab == 0 ? 25 : tab == 1 ? 27 : 26; }

    /** A second press of Back within two seconds of the first leaves the app. */
    static boolean exitNow(long lastPress, long now) {
        return lastPress > 0 && now - lastPress <= DOUBLE_PRESS_MS;
    }
}

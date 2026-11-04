package ma.chouitv.installer.remote;

/**
 * What the remote shows when the app opens. With no TV it is a demo (the keys are shown, dimmed, and explain themselves); with a known TV it opens at once
 * and reconnects in the background. One banner says what is wrong, if anything. Pure, tested on the JVM (HomeStateTest).
 */
public final class HomeState {
    private HomeState() {}

    /** The short search for TVs when the app opens with none known: it never blocks and it is cut after this time. */
    public static final int SEARCH_BUDGET_MS = 4000;

    public enum Link { ONLINE, RECONNECTING, OFFLINE, ASKING }

    /** What the banner says when the known TV does not answer. */
    public enum Offline { NO_WIFI, OTHER_NETWORK, BOOTING, DEBUG_OFF, SILENT, AUTH }

    /** A refusal that lasts less than this is a TV that is starting; after it, its network debugging is off. */
    public static final long BOOT_WINDOW_MS = 30_000;

    public static Offline offline(Cause c, long failingForMs) {
        if (c == null) return Offline.SILENT;
        switch (c) {
            case NO_WIFI: return Offline.NO_WIFI;
            case OTHER_NETWORK: return Offline.OTHER_NETWORK;
            case AUTH: return Offline.AUTH;
            case REFUSED: return failingForMs < BOOT_WINDOW_MS ? Offline.BOOTING : Offline.DEBUG_OFF;
            default: return Offline.SILENT;
        }
    }

    public enum Banner { HIDDEN, SEARCHING, FOUND, NONE_FOUND, OFFLINE, AUTH, MOVED, ASLEEP }

    /** The banner under the name: what the search is doing or found when there is no TV; only an unanswered TV when there is one. */
    public static Banner banner(boolean tvKnown, Link link, boolean searching, int found) { return banner(tvKnown, link, searching, found, false); }

    /** {@code moved}: the TV was just found again at a new address (told for a moment, when it answers). */
    public static Banner banner(boolean tvKnown, Link link, boolean searching, int found, boolean moved) { return banner(tvKnown, link, searching, found, moved, false); }

    /** {@code asleep}: the TV answers but its screen sleeps (standby with the network on): the banner offers to wake it. */
    public static Banner banner(boolean tvKnown, Link link, boolean searching, int found, boolean moved, boolean asleep) {
        if (tvKnown) {
            if (link == Link.OFFLINE) return Banner.OFFLINE;
            if (link == Link.ASKING) return Banner.AUTH;
            if (link == Link.ONLINE && asleep) return Banner.ASLEEP;
            return moved && link == Link.ONLINE ? Banner.MOVED : Banner.HIDDEN;
        }
        if (found > 0) return Banner.FOUND;
        return searching ? Banner.SEARCHING : Banner.NONE_FOUND;
    }

    /** With no TV known the app looks for some at every opening (a known TV is reconnected instead). */
    public static boolean searchesOnOpen(boolean tvKnown) { return !tvKnown; }

    /** The keys act on a TV: without one they explain why and offer the search. */
    public static boolean keysNeedTv(boolean tvKnown) { return !tvKnown; }

    /** The link a known TV really has (an open link that answered); with no TV there is nothing to be offline from. */
    public static Link linkOf(boolean tvKnown, boolean linkOpen) {
        if (!tvKnown) return Link.ONLINE;
        return linkOpen ? Link.ONLINE : Link.OFFLINE;
    }
}

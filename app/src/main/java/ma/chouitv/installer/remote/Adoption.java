package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The search for TVs when the app is adopted (or when a TV is added): the screen never waits in silence, every TV appears as soon as it answers with its own
 * state, and a verdict is given after four seconds while the search goes on. Pure, tested on the JVM (AdoptionTest).
 */
public final class Adoption {
    private Adoption() {}

    /** The time after which the search gives its verdict (and the card for a missing TV shows). */
    public static final int VERDICT_MS = 4000;

    public enum Headline { SEARCHING, FOUND, NONE }

    /** What a TV found on the network says about itself. */
    public enum TvState { CHECKING, READY, ASK }

    public static Headline headline(int found, long elapsedMs) {
        if (found > 0) return Headline.FOUND;
        return elapsedMs < VERDICT_MS ? Headline.SEARCHING : Headline.NONE;
    }

    /** «A TV is missing?»: off, on another Wi-Fi, network debugging off. It shows with the verdict, even when some TVs were found. */
    public static boolean showMissingCard(long elapsedMs) { return elapsedMs >= VERDICT_MS; }

    /** The search looks again while the screen is open: every 10 s for two minutes, then every 30 s. */
    public static long rescanDelayMs(long sinceStartMs) { return sinceStartMs < 120_000 ? 10_000 : 30_000; }

    /** A TV that is ready never goes back to asking or checking (a second look finds it the same); an asking TV becomes ready when the person accepted. */
    public static TvState merge(TvState old, TvState now) {
        if (old == null) return now;
        if (old == TvState.READY) return TvState.READY;
        if (now == TvState.CHECKING) return old;
        return now;
    }

    public static int count(Collection<String> ips) {
        if (ips == null) return 0;
        return new LinkedHashSet<>(ips).size();
    }

    /** By address (the last number), as a person reads a list of TVs; the ones the app already knows first. */
    public static List<String> sorted(Collection<String> ips, final Set<String> known) {
        List<String> l = new ArrayList<>(new LinkedHashSet<>(ips));
        Collections.sort(l, new Comparator<String>() {
            @Override public int compare(String a, String b) {
                boolean ka = known != null && known.contains(a), kb = known != null && known.contains(b);
                if (ka != kb) return ka ? -1 : 1;
                return last(a) - last(b);
            }
        });
        return l;
    }

    public static List<String> sorted(Collection<String> ips) { return sorted(ips, null); }

    private static int last(String ip) {
        try { return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1)); } catch (Exception e) { return 0; }
    }
}

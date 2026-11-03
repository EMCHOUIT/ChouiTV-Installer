package ma.chouitv.installer.remote;

import java.io.IOException;

/** When and how often the phone tries to get the TV back after the link dropped: waits grow, and it stops after a few tries (a TV that sleeps would only drain the battery). */
public final class Reconnect {
    private Reconnect() {}

    public static final int MAX_TRIES = 5;
    private static final long[] DELAYS = {1000, 2000, 4000, 8000, 8000};

    /** The wait before try number {@code attempt} (1 is the first). */
    public static long delayMs(int attempt) {
        int i = Math.max(1, Math.min(attempt, DELAYS.length)) - 1;
        return DELAYS[i];
    }

    public static boolean giveUp(int attempt) { return attempt > MAX_TRIES; }

    /** A broken link or a timeout is worth a new connection; a refused command or a bad key is not. */
    public static boolean worthRetrying(Exception e) {
        return e instanceof IOException;
    }
}

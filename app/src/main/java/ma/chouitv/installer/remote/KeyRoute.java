package ma.chouitv.installer.remote;

/**
 * Which way a key goes. The link kept open to the TV when it works (fast); otherwise «input keyevent» (slow, about a second, but it needs
 * nothing). After a failure the link is not tried again at once: first after 5 s, then 15 s, then every minute, so that a TV that
 * refuses it is not hammered. Pure: the caller gives the time.
 */
public final class KeyRoute {
    public enum Via { CHANNEL, SHELL }

    private static final long[] WAITS = {5_000, 15_000, 60_000};

    private boolean up;
    private boolean stopped;
    private int failures;
    private long retryAt;
    private long lastWait = WAITS[0];

    public synchronized Via via() { return up && !stopped ? Via.CHANNEL : Via.SHELL; }

    /** A link is working. */
    public synchronized void up() { up = true; failures = 0; retryAt = 0; }

    /** The link failed (or could not be made) at {@code now}. */
    public synchronized void down(long now) {
        up = false;
        lastWait = WAITS[Math.min(failures, WAITS.length - 1)];
        failures++;
        retryAt = now + lastWait;
    }

    /** The wait the last failure set before the next try (5 s, then 15 s, then 60 s at most). */
    public synchronized long waitMs() { return lastWait; }

    /** True when a new link may be tried: none works, none was tried too recently, and the app is not closed. */
    public synchronized boolean mayTry(long now) { return !up && !stopped && now >= retryAt; }

    /** The app is left: no more tries. */
    public synchronized void stop() { stopped = true; up = false; }

    /** The app is back: a fresh start, with no memory of the earlier failures. */
    public synchronized void resume() { stopped = false; up = false; failures = 0; retryAt = 0; lastWait = WAITS[0]; }
}

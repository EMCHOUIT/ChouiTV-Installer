package ma.chouitv.installer.remote;

import java.util.ArrayDeque;

/**
 * The keys waiting to go to the TV. At most {@link #MAX}: the TV takes them one after the other, and a finger held on an arrow sends them
 * faster than that, so a longer queue would only make the TV keep scrolling long after the release. A repeat of the key that is already
 * last in the queue is dropped when it is full; a different key (a real tap) pushes the oldest one out instead of being lost.
 */
public final class KeyQueue {
    public static final int MAX = 3;

    private final ArrayDeque<Integer> keys = new ArrayDeque<>();

    public synchronized void offer(int code) {
        if (keys.size() >= MAX) {
            if (keys.peekLast() == code) return;   // a repeat: the key is already waiting
            keys.pollFirst();                       // a different key: the oldest one makes room
        }
        keys.addLast(code);
        notifyAll();
    }

    /** The next key, or null when none is waiting. */
    public synchronized Integer poll() { return keys.pollFirst(); }

    /** The next key; waits for one. Throws when the thread is interrupted (the link was closed). */
    public synchronized int take() throws InterruptedException {
        while (keys.isEmpty()) wait();
        return keys.pollFirst();
    }

    public synchronized int size() { return keys.size(); }

    public synchronized void clear() { keys.clear(); }
}

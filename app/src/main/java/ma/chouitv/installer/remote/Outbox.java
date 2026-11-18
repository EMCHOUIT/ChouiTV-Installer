package ma.chouitv.installer.remote;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * What waits to go to the TV, in the order it was given. Key presses: at most {@link #MAX_KEYS}, because a finger held on an arrow sends them
 * faster than the TV takes them and a longer queue would only make the TV keep scrolling after the release (a repeat of the last key waiting is
 * dropped; a different key makes the oldest one leave). Lines of typed text are never dropped: each one is something somebody typed.
 */
public final class Outbox {
    public static final int MAX_KEYS = 3;

    public static final class Item {
        /** The line to send. */
        public final String line;
        /** The key code, or -1 for a line of text (reported when it fails). */
        public final int code;
        /** What to do when the TV confirms it (may be null). */
        public final Runnable onConfirmed;
        Item(String line, int code, Runnable onConfirmed) { this.line = line; this.code = code; this.onConfirmed = onConfirmed; }
    }

    private final ArrayDeque<Item> items = new ArrayDeque<>();

    public synchronized void offerKey(int code) {
        String line = KeyProtocol.line(code);   // refuses a key that is not the remote's
        int keys = 0;
        Item lastKey = null;
        for (Item i : items) if (i.code >= 0) { keys++; lastKey = i; }
        if (keys >= MAX_KEYS) {
            if (lastKey.code == code) return;                  // a repeat: the key is already waiting
            for (Iterator<Item> it = items.iterator(); it.hasNext(); ) {   // a different key: the oldest key makes room
                if (it.next().code >= 0) { it.remove(); break; }
            }
        }
        items.addLast(new Item(line, code, null));
        notifyAll();
    }

    public synchronized void offerLine(String line, Runnable onConfirmed) {
        items.addLast(new Item(line, -1, onConfirmed));
        notifyAll();
    }

    /** The next item, or null when none is waiting. */
    public synchronized Item poll() { return items.pollFirst(); }

    /** The next item; waits for one. Throws when the thread is interrupted (the link was closed). */
    public synchronized Item take() throws InterruptedException {
        while (items.isEmpty()) wait();
        return items.pollFirst();
    }

    public synchronized int size() { return items.size(); }

    public synchronized void clear() { items.clear(); }
}

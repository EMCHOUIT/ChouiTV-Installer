package ma.chouitv.installer.remote;

import java.io.IOException;

/**
 * The link kept open to the small program running on the TV (helper/Keys.java): one writer thread takes the keys from a short queue, sends
 * each as a line and waits for the answer, so a key costs the network trip and not the second that «input keyevent» needs to start.
 * It never retries a key by itself: when the TV does not answer, answers an error or the connection breaks, the link goes down, the key
 * that could not be confirmed is handed back (the owner sends it the slow way) and later keys are refused (they go the slow way too).
 */
public final class KeyLink implements AutoCloseable {
    /** A line-based two-way stream to the program: a write, and a read that throws on a timeout or a closed connection. */
    public interface Duplex extends AutoCloseable {
        void write(String s) throws IOException;
        /** The next line the program printed; throws {@link java.net.SocketTimeoutException} when it takes too long. */
        String readLine() throws IOException;
        @Override void close();
    }

    public interface Listener {
        /**
         * The link went down. {@code code} is the key that was not confirmed (-1 when there was none). {@code mayHaveReachedTheTv} is false when
         * nothing was sent or the TV said it did not inject it (the owner may send it the slow way), true when it was sent and the TV said
         * nothing (it may already have been injected: sending it again would press it twice).
         */
        void failed(int code, boolean mayHaveReachedTheTv, Exception why);
    }

    private final Duplex tv;
    private final Listener listener;
    private final KeyQueue queue = new KeyQueue();
    private final Thread writer;
    private volatile boolean up = true;

    public KeyLink(Duplex tv, Listener listener) {
        this.tv = tv;
        this.listener = listener;
        writer = new Thread(this::loop, "rc-keys");
        writer.setDaemon(true);
        writer.start();
    }

    public boolean isUp() { return up; }

    /** Queues a key. False when the link is down: the caller sends it the slow way. Never blocks. */
    public boolean offer(int code) {
        if (!up) return false;
        queue.offer(code);
        return true;
    }

    private void loop() {
        int code = -1;
        boolean sent = false;
        try {
            while (up) {
                sent = false;
                code = queue.take();
                tv.write(KeyProtocol.line(code));
                sent = true;
                String answer = tv.readLine();
                if (!KeyProtocol.ok(answer)) { sent = false; throw new IOException("the TV answered: " + answer); }   // it did not inject it
                code = -1;
            }
        } catch (InterruptedException closed) {
            // close(): nothing to report
        } catch (Exception e) {
            boolean wasUp = up;
            up = false;
            queue.clear();
            tv.close();
            if (wasUp) listener.failed(code, sent, e);
        }
    }

    @Override public void close() {
        up = false;
        queue.clear();
        writer.interrupt();
        tv.close();
    }
}

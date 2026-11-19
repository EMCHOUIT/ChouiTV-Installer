package ma.chouitv.installer.remote;

import java.io.IOException;

/**
 * The link kept open to the small program running on the TV (helper/Keys.java): one writer thread takes what waits in the outbox (key presses
 * and lines of typed text), sends each as a line and waits for the answer, so a key costs the network trip and not the second that
 * «input keyevent» needs to start.
 * It never retries anything by itself: when the TV does not answer, answers an error or the connection breaks, the link goes down, the key
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
         * The link went down. {@code code} is the key that was not confirmed (-1 when there was none, or when it was a line of text).
         * {@code mayHaveReachedTheTv} is false when nothing was sent or the TV said it did not inject it (the owner may send it the slow way),
         * true when it was sent and the TV said nothing (it may already have been injected: sending it again would press it twice).
         * {@code why} is a {@link Refused} when the TV answered an error.
         */
        void failed(int code, boolean mayHaveReachedTheTv, Exception why);
    }

    /** The TV answered an error to a line: it did not do it (it is not the silence of a dead link). */
    public static final class Refused extends IOException {
        public Refused(String message) { super(message); }
    }

    private final Duplex tv;
    private final Listener listener;
    private final Outbox outbox = new Outbox();
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
        outbox.offerKey(code);
        return true;
    }

    /** Queues a line (typed text, or a delete); {@code onConfirmed} runs when the TV answers «ok». False when the link is down. Never blocks, never drops it. */
    public boolean offerLine(String line, Runnable onConfirmed) {
        if (!up) return false;
        outbox.offerLine(line, onConfirmed);
        return true;
    }

    /** Queues a request; the TV's answer line is given to {@code onAnswer} (not at all when the link fails). False when the link is down. Never blocks. */
    public boolean offerQuery(String line, java.util.function.Consumer<String> onAnswer) {
        if (!up) return false;
        outbox.offerQuery(line, onAnswer);
        return true;
    }

    private void loop() {
        int code = -1;
        boolean sent = false;
        try {
            while (up) {
                sent = false;
                Outbox.Item item = outbox.take();
                code = item.code;
                tv.write(item.line);
                sent = true;
                String answer = tv.readLine();
                if (!KeyProtocol.ok(answer)) {
                    if (KeyProtocol.isError(answer)) { sent = false; throw new Refused("the TV answered: " + answer); }   // it did not do it
                    throw new IOException("the TV's program said: " + answer);   // not a protocol line: the program is gone (killed)
                }
                code = -1;
                if (item.onConfirmed != null) item.onConfirmed.run();
                if (item.onAnswer != null) item.onAnswer.accept(answer);
            }
        } catch (InterruptedException closed) {
            // close(): nothing to report
        } catch (Exception e) {
            boolean wasUp = up;
            up = false;
            outbox.clear();
            tv.close();
            if (wasUp) listener.failed(code, sent, e);
        }
    }

    @Override public void close() {
        up = false;
        outbox.clear();
        writer.interrupt();
        tv.close();
    }
}

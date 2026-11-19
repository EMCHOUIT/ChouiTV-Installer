package ma.chouitv.installer.remote;

/**
 * The live keyboard of the phone: what goes to the TV as the person types, and what happens when the fast link fails. It never loses the text:
 * what the phone shows stays, and what the TV is not known to have is sent when the link is back, or by the slow way when the person asks.
 *
 * Modes: READY (nothing typed yet), DIRECT (each change goes to the TV at once), RECONNECTING (the link is down; the text waits; after
 * {@link #GIVE_UP_MS} it goes to SLOW), SLOW (the person taps Send, as before), ERROR (the TV refused what was sent).
 * The caller (the screen) calls {@link #typed} on every change of the field and {@link #tick} about twice a second.
 */
public final class LiveKeyboard {
    public enum Mode { READY, DIRECT, RECONNECTING, SLOW, ERROR }

    /** The fast link to the TV. Every method returns false when it cannot take the line (the link is down); {@code ok} runs when the TV confirms it. */
    public interface Link {
        boolean up();
        boolean del(int n, Runnable ok);
        boolean text(String s, Runnable ok);
        boolean enter();
        /** Select all, then Delete: the whole field of the TV is emptied, whoever typed what is in it. */
        boolean clearAll(Runnable ok);
        /** The End key: the TV's cursor goes to the end of the field. */
        boolean moveToEnd();
        /** Asks the text of the field that has the input focus on the TV; {@code onText} gets it («» for an empty field, null when there is none). */
        boolean readField(java.util.function.Consumer<String> onText);
    }

    /** How long the keyboard waits for the link before it offers the slow way. */
    public static final long GIVE_UP_MS = 6_000;

    private final Link link;
    private final LiveText lt = new LiveText();
    private Mode mode;
    private String shown = "";
    private long reconnectSince = -1;
    private long epoch;   // changes at each loss: a confirmation from before is ignored

    public LiveKeyboard(Link link) {
        this.link = link;
        this.mode = link.up() ? Mode.READY : Mode.RECONNECTING;
    }

    public synchronized Mode mode() { return mode; }

    /** The number of characters of the field. */
    public synchronized int chars() { return LiveText.chars(shown); }

    /** The characters the TV is not known to have yet. */
    public synchronized int waiting() {
        LiveText.Op op = lt.pending(shown);
        return op == null ? 0 : LiveText.chars(op.text);
    }

    /** The field of the phone changed. */
    public synchronized void typed(String text) {
        shown = text == null ? "" : text;
        if (mode == Mode.SLOW || mode == Mode.ERROR) return;   // kept; it goes when the person chooses
        if (!link.up()) { toReconnecting(); return; }
        if (mode == Mode.RECONNECTING) { flush(); return; }
        send();
    }

    /** «Clear all»: the whole field of the TV is emptied, also what was typed with the TV's own keyboard. */
    public synchronized void clearAll() {
        shown = "";
        if ((mode != Mode.DIRECT && mode != Mode.READY) || !link.up()) return;   // without a live link what waits is simply emptied
        LiveText.Op op = lt.reset();
        final long e = epoch;
        if (link.clearAll(() -> confirm(op, e))) mode = Mode.DIRECT;
        else { lt.lost(); epoch++; mode = Mode.RECONNECTING; reconnectSince = -1; }
    }

    /** A backspace on the empty field of the phone: it deletes one character of the TV's field (text that was not typed on the phone). */
    public synchronized void deleteOnTv() {
        if (mode != Mode.DIRECT && mode != Mode.READY) return;
        if (!link.up() || !link.del(1, null)) linkLost();
    }

    /**
     * The TV's field already held {@code text} (typed with its own keyboard): it becomes the starting point, so that the phone shows it and what
     * is typed or deleted on the phone is applied to it. Only while the phone's field is still empty and the link is live; false otherwise.
     * The caller then puts the text in the phone's field.
     */
    public synchronized boolean preloaded(String text) {
        if (text == null || text.isEmpty() || !shown.isEmpty()) return false;
        if ((mode != Mode.DIRECT && mode != Mode.READY) || !link.up()) return false;
        if (!link.moveToEnd()) { linkLost(); return false; }   // what is typed comes after it
        lt.slowSent(text);
        shown = text;
        mode = Mode.DIRECT;
        return true;
    }

    /** Asks the TV for the text its field already holds (about a tenth of a second); false when there is no live link. */
    public synchronized boolean readExisting(java.util.function.Consumer<String> onText) {
        return link.up() && link.readField(onText);
    }

    /** The Enter / Search key. Only in the live modes. */
    public synchronized void enter() {
        if (mode != Mode.DIRECT && mode != Mode.READY) return;
        if (!link.up() || !link.enter()) linkLost();
    }

    /** The link broke (the owner of the link says so): what was not confirmed is gone. */
    public synchronized void linkLost() {
        lt.lost();
        epoch++;
        if (mode == Mode.DIRECT || mode == Mode.READY) { mode = Mode.RECONNECTING; reconnectSince = -1; }
    }

    /** The TV answered an error to what was sent. */
    public synchronized void linkRefused() {
        lt.lost();
        epoch++;
        mode = Mode.ERROR;
    }

    /** «Try live again». */
    public synchronized void retryLive(long now) {
        lt.lost();
        epoch++;
        mode = Mode.RECONNECTING;
        reconnectSince = now;
    }

    /** «Slow mode». */
    public synchronized void chooseSlow() {
        lt.lost();
        epoch++;
        mode = Mode.SLOW;
    }

    /** Called about twice a second: gets back to direct when the link is back, gives up after a few seconds. */
    public synchronized void tick(long now) {
        if (mode != Mode.RECONNECTING) return;
        if (reconnectSince < 0) reconnectSince = now;
        if (link.up()) { flush(); return; }
        if (now - reconnectSince >= GIVE_UP_MS) mode = Mode.SLOW;
    }

    /** What the Send button of the slow mode sends (null when nothing waits). */
    public synchronized LiveText.Op pendingForSlowSend() { return lt.pending(shown); }

    /** The slow way sent it all. */
    public synchronized void slowSent() { lt.slowSent(shown); }

    // ---- inside

    private void toReconnecting() {
        if (mode != Mode.RECONNECTING) { lt.lost(); epoch++; mode = Mode.RECONNECTING; reconnectSince = -1; }
    }

    /** The link is back: send what the TV does not have. */
    private void flush() {
        lt.lost();
        epoch++;
        send();
    }

    private void send() {
        LiveText.Op op = lt.edit(shown);
        if (op == null) { if (mode == Mode.RECONNECTING) mode = Mode.DIRECT; return; }
        final long e = epoch;
        final Runnable ok = () -> confirm(op, e);
        java.util.List<String> parts = KeyProtocol.chunks(op.text);
        boolean sent = true;
        if (op.dels > 0) sent = link.del(op.dels, parts.isEmpty() ? ok : null);
        for (int i = 0; sent && i < parts.size(); i++) sent = link.text(parts.get(i), i == parts.size() - 1 ? ok : null);
        if (sent) mode = Mode.DIRECT;
        else { lt.lost(); epoch++; mode = Mode.RECONNECTING; reconnectSince = -1; }
    }

    private synchronized void confirm(LiveText.Op op, long e) {
        if (e == epoch) lt.confirmed(op);
    }
}

package ma.chouitv.installer.remote;

/**
 * Keeps the field of the TV the same as the field of the phone while somebody types, with what a TV can do: delete from the end (Delete key)
 * and type (the text is put on the clipboard and pasted). So a change of the phone's text is «delete n characters, then type this»: the cursor of
 * the TV is taken to be at the end of what was typed. It remembers three texts: what was sent, what the TV confirmed, and (given by the caller)
 * what the phone shows.
 */
public final class LiveText {
    /** What to send for one change: delete {@code dels} characters, then type {@code text}; {@code result} is the whole text afterwards. */
    public static final class Op {
        public final int dels;
        public final String text;
        public final String result;
        Op(int dels, String text, String result) { this.dels = dels; this.text = text; this.result = result; }
    }

    private String sent = "";    // what the TV will have when everything given so far has been done
    private String acked = "";   // what the TV is known to have

    /** The number of characters (a character made of two UTF-16 units counts for one). */
    public static int chars(String s) { return s == null ? 0 : s.codePointCount(0, s.length()); }

    /** The change that turns {@code from} into {@code to} when the cursor is at the end of {@code from}; null when they are the same. */
    static Op diff(String from, String to) {
        if (from.equals(to)) return null;
        int i = 0, f = 0, t = 0;   // i: characters in common, f and t: their length in UTF-16 units in each text
        while (f < from.length() && t < to.length()) {
            int a = from.codePointAt(f), b = to.codePointAt(t);
            if (a != b) break;
            f += Character.charCount(a);
            t += Character.charCount(b);
            i++;
        }
        return new Op(chars(from) - i, to.substring(t), to);
    }

    /** The phone's text changed: what to send. Null when there is nothing to send. */
    public synchronized Op edit(String shown) {
        Op op = diff(sent, shown);
        sent = shown;
        return op;
    }

    /** The whole field of the TV is emptied (Select all, Delete), whatever it held: nothing is left to delete later. */
    public synchronized Op reset() {
        sent = "";
        return new Op(0, "", "");
    }

    /** The TV confirmed this change (and every earlier one). */
    public synchronized void confirmed(Op op) { acked = op.result; }

    /** The link went down: what was given and not confirmed is gone. */
    public synchronized void lost() { sent = acked; }

    /** What the TV is known to have. */
    public synchronized String onTv() { return acked; }

    /** What is left to send so that the TV has {@code shown}, from what it is known to have; null when nothing. Changes nothing. */
    public synchronized Op pending(String shown) { return diff(acked, shown); }

    /** The slow way sent everything up to {@code shown}. */
    public synchronized void slowSent(String shown) { acked = shown; sent = shown; }
}

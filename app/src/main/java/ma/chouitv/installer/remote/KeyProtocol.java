package ma.chouitv.installer.remote;

/**
 * What the phone and the small program kept running on the TV (helper/Keys.java) say to each other. The phone starts it once
 * ({@link #command()}); it prints «ready», then for each line it reads, a key code, it injects the key and answers «ok» (or «err …»).
 * Starting «input keyevent» for every key costs about a second on a TV; this costs a few milliseconds.
 */
public final class KeyProtocol {
    private KeyProtocol() {}

    /** The line to send for a key: only the keys of the remote. */
    public static String line(int code) {
        if (!Remote.isKnown(code)) throw new IllegalArgumentException("not a remote key: " + code);
        return code + "\n";
    }

    /** The line that asks for the text of the field that has the input focus on the TV. */
    public static String fieldLine() { return "R\n"; }

    /**
     * The text in the answer to {@link #fieldLine()}: «ok text BASE64» (the field's text; «ok text» alone for an empty field), «ok none» when no field
     * has the input focus or it is a password. Null for anything else.
     */
    public static String parseField(String answer) {
        if (answer == null) return null;
        String a = answer.trim();
        if (a.equals("ok text")) return "";
        if (!a.startsWith("ok text ")) return null;
        try {
            return new String(java.util.Base64.getDecoder().decode(a.substring(8).trim()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException notBase64) {
            return null;
        }
    }

    /** The line that empties the whole field that has the focus, whatever it holds (Select all, then Delete). */
    public static String clearLine() { return "A\n"; }

    /** The most characters of text in one line: long pastes go in pieces, in order. */
    public static final int CHUNK = 200;

    /** The line that makes the TV type {@code text}: «T», then the text in base64 (so it is one line whatever it holds). The program puts it on the clipboard and presses Paste. */
    public static String textLine(String text) {
        return "T " + java.util.Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\n";
    }

    /** {@code text} in pieces of at most {@link #CHUNK} characters; a character made of two UTF-16 units is never cut. */
    public static java.util.List<String> chunks(String text) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = text.offsetByCodePoints(i, Math.min(CHUNK, text.codePointCount(i, text.length())));
            out.add(text.substring(i, end));
            i = end;
        }
        return out;
    }

    /** True for «ok» alone or followed by details («ok 3ms»); not for «okay», nor for an error. */
    public static boolean ok(String answer) {
        if (answer == null) return false;
        String a = answer.trim();
        return a.equals("ok") || a.startsWith("ok ");
    }

    /** True for an answer that says the TV did not do it («err …»); any other line that is not «ok» is the sign of a broken link. */
    public static boolean isError(String answer) {
        return answer != null && answer.trim().startsWith("err");
    }

    /** True when the program says it can inject keys. */
    public static boolean ready(String firstLine) {
        return firstLine != null && firstLine.trim().equals("ready");
    }

    /** The command that starts the program on the TV (the helper jar is pushed there first). */
    public static String command() {
        return "CLASSPATH=" + Remote.HELPER_REMOTE + " app_process /system/bin Keys";
    }
}

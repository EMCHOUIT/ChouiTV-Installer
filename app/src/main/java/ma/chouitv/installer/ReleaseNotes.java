package ma.chouitv.installer;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * What is new in a ChouiTV release, if the signed version file says it: a «notes» field, either one text or one text per language (fr, en, ar).
 * It is shown before an update starts. A version file without notes (the case today) shows nothing.
 */
final class ReleaseNotes {
    static final int MAX = 600;
    private final Map<String, String> byLang = new HashMap<>();

    boolean isEmpty() { return byLang.isEmpty(); }

    static ReleaseNotes from(JSONObject manifest) {
        ReleaseNotes r = new ReleaseNotes();
        Object o = manifest == null ? null : manifest.opt("notes");
        if (o instanceof String) {
            String t = ((String) o).trim();
            if (!t.isEmpty()) r.byLang.put("*", t);
        } else if (o instanceof JSONObject) {
            JSONObject j = (JSONObject) o;
            for (String lang : new String[] {"fr", "en", "ar"}) {
                Object v = j.opt(lang);
                if (v instanceof String && !((String) v).trim().isEmpty()) r.byLang.put(lang, ((String) v).trim());
            }
        }
        return r;
    }

    /** The text for this language (else English, else French, else the single text), cut to {@link #MAX} characters. Empty when there is none. */
    static String pick(ReleaseNotes n, String lang) {
        if (n == null) return "";
        String t = n.byLang.get(lang);
        if (t == null) t = n.byLang.get("en");
        if (t == null) t = n.byLang.get("fr");
        if (t == null) t = n.byLang.get("*");
        if (t == null) return "";
        return t.length() <= MAX ? t : t.substring(0, MAX - 1) + "\u2026";
    }
}

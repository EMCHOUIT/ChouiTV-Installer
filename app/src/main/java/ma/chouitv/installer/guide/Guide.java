package ma.chouitv.installer.guide;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Where a person is in a guided path: which path, which step, and for the copy path the name of the settings in play. Pure: it is
 * kept as one small JSON text so the app can resume after it was closed.
 */
public final class Guide {
    public enum Path {
        EQUIP(3), COPY(3), SEND(2), REACT(1);
        public final int steps;
        Path(int steps) { this.steps = steps; }
        public String key() { return name().toLowerCase(Locale.ROOT); }
    }

    public final Path path;
    public int step;
    public String profile = "";

    public Guide(Path path) { this.path = path; }

    public boolean last() { return step >= path.steps - 1; }

    /** The next step, never past the last. */
    public void advance() { if (!last()) step++; }

    /** The key of a text of the current step: «gn_copy_1», «gd_equip_0»… (n = now title, d = now text, l = label, x = next). */
    public String key(char kind) { return "g" + kind + "_" + path.key() + "_" + step; }

    public String toJson() {
        try {
            return new JSONObject().put("path", path.name()).put("step", step).put("profile", profile).toString();
        } catch (Exception e) { return "{}"; }
    }

    /** null when the text is empty, damaged or from a path that does not exist any more. */
    public static Guide fromJson(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(s);
            Path p = Path.valueOf(o.getString("path"));
            Guide g = new Guide(p);
            g.step = Math.max(0, Math.min(o.optInt("step", 0), p.steps - 1));
            g.profile = o.optString("profile", "");
            return g;
        } catch (Exception e) { return null; }
    }
}

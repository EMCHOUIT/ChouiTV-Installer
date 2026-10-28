package ma.chouitv.installer.cleanup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The cleanup lists (optimize/lists/cleanup.json of the ChouiTV repository), parsed. Immutable once built. */
public final class CleanupData {
    public enum Group { TRACKING, ADS, PREINSTALLED, OPTIONAL, USER;
        static Group of(String s) {
            switch (s) {
                case "tracking": return TRACKING;
                case "ads": return ADS;
                case "optional": return OPTIONAL;
                default: return PREINSTALLED;
            }
        }
    }

    public static final class Entry {
        public final String id, source;
        public final Set<String> brands = new HashSet<>(), models = new HashSet<>(), devices = new HashSet<>();
        public final List<String> packages = new ArrayList<>();
        Entry(String id, String source) { this.id = id; this.source = source; }
    }

    public final Set<String> keep = new HashSet<>(), homes = new HashSet<>(), genericModels = new HashSet<>(), genericDevices = new HashSet<>();
    public final Map<String, Group> group = new HashMap<>();
    public final Map<String, String> namesAr = new HashMap<>(), namesFr = new HashMap<>(), display = new HashMap<>();
    public final List<Entry> lists = new ArrayList<>();

    public static CleanupData parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        if (o.getInt("schema") != 1) throw new IllegalArgumentException("unknown schema " + o.getInt("schema"));
        CleanupData d = new CleanupData();
        fill(d.keep, o.getJSONArray("keep"));
        fill(d.homes, o.getJSONArray("homes"));
        fill(d.genericModels, o.getJSONArray("genericModels"));
        fill(d.genericDevices, o.getJSONArray("genericDevices"));
        JSONArray pk = o.getJSONArray("packages"), gr = o.getJSONArray("groups");
        String[] table = new String[pk.length()];
        for (int i = 0; i < table.length; i++) {
            table[i] = pk.getString(i);
            d.group.put(table[i], Group.of(gr.getString(i)));
        }
        JSONObject names = o.optJSONObject("names");
        if (names != null && names.optJSONObject("ar") != null) {
            JSONObject ar = names.getJSONObject("ar");
            for (java.util.Iterator<String> it = ar.keys(); it.hasNext(); ) { String k = it.next(); d.namesAr.put(k, ar.getString(k)); }
        }
        if (names != null && names.optJSONObject("fr") != null) {
            JSONObject fr = names.getJSONObject("fr");
            for (java.util.Iterator<String> it = fr.keys(); it.hasNext(); ) { String k = it.next(); d.namesFr.put(k, fr.getString(k)); }
        }
        JSONObject disp = o.optJSONObject("display");
        if (disp != null) for (java.util.Iterator<String> it = disp.keys(); it.hasNext(); ) { String k = it.next(); d.display.put(k, disp.getString(k)); }
        JSONArray ls = o.getJSONArray("lists");
        for (int i = 0; i < ls.length(); i++) {
            JSONObject l = ls.getJSONObject(i);
            Entry e = new Entry(l.getString("id"), l.getString("source"));
            fill(e.brands, l.getJSONArray("brands"));
            fill(e.models, l.getJSONArray("models"));
            fill(e.devices, l.getJSONArray("devices"));
            JSONArray p = l.getJSONArray("p");
            for (int k = 0; k < p.length(); k++) e.packages.add(table[p.getInt(k)]);
            d.lists.add(e);
        }
        return d;
    }

    private static void fill(Set<String> into, JSONArray a) throws Exception {
        for (int i = 0; i < a.length(); i++) into.add(a.getString(i));
    }

    /** The name to show: ours in the person's language, else the brand name, else the package made readable. Never the raw package. */
    public String label(String pkg, String lang) {
        if ("ar".equals(lang) && namesAr.containsKey(pkg)) return namesAr.get(pkg);
        if ("fr".equals(lang) && namesFr.containsKey(pkg)) return namesFr.get(pkg);
        return display.containsKey(pkg) ? display.get(pkg) : pretty(pkg);
    }

    private static final java.util.Set<String> GENERIC = new HashSet<>(java.util.Arrays.asList("tv", "app", "apps", "android", "manager", "service", "services", "api",
        "mobile", "client", "main", "res", "off", "on", "ui", "sdk", "core", "lib", "base", "free", "pro", "lite", "release", "prerelease", "beta"));

    /** «com.tdm.manager» → «Tdm Manager»: the last word of the package, with the one before it when the last is too generic to mean anything. */
    public static String pretty(String pkg) {
        String[] seg = pkg.split("\\.");
        int i = seg.length - 1;
        StringBuilder out = new StringBuilder(words(seg[i]));
        while (i > 1 && (GENERIC.contains(seg[i].toLowerCase(java.util.Locale.ROOT)) || seg[i].length() < 5)) {
            i--;
            out.insert(0, words(seg[i]) + " ");
            if (out.length() > 28) break;
        }
        return out.toString().trim();
    }

    private static String words(String seg) {
        String spaced = seg.replace('_', ' ').replace('-', ' ').replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ").trim();
        StringBuilder b = new StringBuilder();
        for (String w : spaced.split("\\s+")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    public Entry list(String id) {
        for (Entry e : lists) if (e.id.equals(id)) return e;
        return null;
    }
}

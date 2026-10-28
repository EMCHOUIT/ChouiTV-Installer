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
    public final Map<String, String> namesAr = new HashMap<>(), display = new HashMap<>();
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

    /** The name to show: the Arabic one in Arabic, else the brand name, else the package itself. */
    public String label(String pkg, String lang) {
        if ("ar".equals(lang) && namesAr.containsKey(pkg)) return namesAr.get(pkg);
        return display.containsKey(pkg) ? display.get(pkg) : pkg;
    }

    public Entry list(String id) {
        for (Entry e : lists) if (e.id.equals(id)) return e;
        return null;
    }
}

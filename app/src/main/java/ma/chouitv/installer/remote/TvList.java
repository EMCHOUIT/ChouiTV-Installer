package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** The TVs of the family, most recently used first: each has an address, the model the TV told, and a name the family may have chosen («Salon»). Kept as lines «ip|name|ownName» in the same store as {@link LastTv}. */
public final class TvList {
    private TvList() {}

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    public static final class Tv {
        public final String ip, model, own;
        Tv(String ip, String model, String own) { this.ip = ip; this.model = model; this.own = own; }
        /** What the family sees: the name it chose, else the model the TV told, else the address. */
        public String name = null;
        Tv named() { name = !own.isEmpty() ? own : !model.isEmpty() ? model : ip; return this; }
    }

    private static String clean(String s) { return s == null ? "" : s.replaceAll("[|\\r\\n]", " ").replaceAll("\\s+", " ").trim(); }

    public static List<Tv> load(LastTv.Store s) {
        List<Tv> r = new ArrayList<>();
        String raw = s.get("tvs");
        if (raw != null) {
            for (String line : raw.split("\n")) {
                String[] p = line.split("\\|", -1);
                if (p.length < 3 || !IPV4.matcher(p[0]).matches()) continue;
                r.add(new Tv(p[0], p[1], p[2]).named());
            }
        }
        if (r.isEmpty()) {   // a version that remembered one TV only
            LastTv one = LastTv.load(s);
            if (one != null) r.add(new Tv(one.ip, one.name.equals(one.ip) ? "" : one.name, "").named());
        }
        return r;
    }

    private static void save(LastTv.Store s, List<Tv> l) {
        StringBuilder b = new StringBuilder();
        for (Tv t : l) { if (b.length() > 0) b.append('\n'); b.append(t.ip).append('|').append(t.model).append('|').append(t.own); }
        s.put("tvs", b.toString());
        if (l.isEmpty()) LastTv.forget(s); else LastTv.save(s, l.get(0).ip, l.get(0).name);
    }

    /** This TV was just used: it goes to the front (added if new); the name chosen by the family is kept. */
    public static void use(LastTv.Store s, String ip, String model) {
        List<Tv> l = load(s);
        String own = "";
        for (int i = 0; i < l.size(); i++) if (l.get(i).ip.equals(ip)) { own = l.get(i).own; l.remove(i); break; }
        l.add(0, new Tv(ip, clean(model), own).named());
        save(s, l);
    }

    /** A name chosen by the family (empty: back to the model). */
    public static void rename(LastTv.Store s, String ip, String name) {
        List<Tv> l = load(s);
        for (int i = 0; i < l.size(); i++) if (l.get(i).ip.equals(ip)) { Tv t = l.get(i); l.set(i, new Tv(t.ip, t.model, clean(name)).named()); break; }
        save(s, l);
    }

    public static void forget(LastTv.Store s, String ip) {
        List<Tv> l = load(s);
        for (int i = 0; i < l.size(); i++) if (l.get(i).ip.equals(ip)) { l.remove(i); break; }
        save(s, l);
    }
}

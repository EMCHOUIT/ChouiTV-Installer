package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.List;

/** What the app did to each TV (a clean-up, an install, settings saved or sent, apps put back), kept on the phone, newest first, at most {@link #MAX} lines per TV. Nothing leaves the phone. */
public final class TvLog {
    private TvLog() {}

    public static final String CLEANUP = "cleanup", INSTALL = "install", SETTINGS_SAVED = "saved", SETTINGS_SENT = "sent", REACTIVATE = "reactivate";
    public static final int MAX = 30;

    public static final class Entry {
        public final long time;
        public final String code, detail;
        Entry(long time, String code, String detail) { this.time = time; this.code = code; this.detail = detail; }
    }

    private static String clean(String s) { return s == null ? "" : s.replaceAll("[|\\r\\n]", " ").replaceAll("\\s+", " ").trim(); }

    public static void add(LastTv.Store s, String ip, long time, String code, String detail) {
        if (ip == null || ip.isEmpty()) return;
        List<Entry> l = load(s, ip);
        l.add(0, new Entry(time, clean(code), clean(detail)));
        while (l.size() > MAX) l.remove(l.size() - 1);
        StringBuilder b = new StringBuilder();
        for (Entry e : l) { if (b.length() > 0) b.append('\n'); b.append(e.time).append('|').append(e.code).append('|').append(e.detail); }
        s.put("log_" + ip, b.toString());
    }

    public static List<Entry> load(LastTv.Store s, String ip) {
        List<Entry> r = new ArrayList<>();
        if (ip == null || ip.isEmpty()) return r;
        String raw = s.get("log_" + ip);
        if (raw == null) return r;
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 3) continue;
            try { r.add(new Entry(Long.parseLong(p[0]), p[1], p[2])); } catch (NumberFormatException ignored) { }
        }
        return r;
    }
}

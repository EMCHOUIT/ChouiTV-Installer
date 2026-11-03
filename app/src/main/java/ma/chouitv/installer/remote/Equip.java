package ma.chouitv.installer.remote;

/** Has this TV been set up by the app (cleaned, ChouiTV installed, settings done)? And was the family already asked what to do with it? Kept per address in the same store as the TVs. */
public final class Equip {
    private Equip() {}

    private static boolean ok(String ip) { return ip != null && !ip.isEmpty(); }

    /** A TV seen for the first time is asked «What to do with this TV?» once (set it up, or only the remote). */
    public static boolean shouldAsk(LastTv.Store s, String ip) {
        return ok(ip) && !isEquipped(s, ip) && !"1".equals(s.get("asked_" + ip));
    }

    public static void markAsked(LastTv.Store s, String ip) { if (ok(ip)) s.put("asked_" + ip, "1"); }

    public static boolean isEquipped(LastTv.Store s, String ip) { return ok(ip) && "1".equals(s.get("equipped_" + ip)); }

    public static void markEquipped(LastTv.Store s, String ip) { if (ok(ip)) s.put("equipped_" + ip, "1"); }

    /** The page of a TV offers «Set up this TV» until it was done. */
    public static boolean offers(LastTv.Store s, String ip) { return ok(ip) && !isEquipped(s, ip); }
}

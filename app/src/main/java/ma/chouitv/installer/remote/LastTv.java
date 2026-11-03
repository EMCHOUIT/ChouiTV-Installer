package ma.chouitv.installer.remote;

import java.util.regex.Pattern;

/** The TV the phone talked to last, so that the app can open straight on its remote. Stored through a tiny interface so that it can be tested without Android. */
public final class LastTv {
    public interface Store { String get(String key); void put(String key, String value); }

    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    public final String ip;
    public final String name;

    private LastTv(String ip, String name) { this.ip = ip; this.name = name; }

    public static void save(Store s, String ip, String name) {
        s.put("last_ip", ip);
        s.put("last_name", name == null ? "" : name);
    }

    public static void forget(Store s) {
        s.put("last_ip", "");
        s.put("last_name", "");
    }

    /** The remembered TV, or null when there is none (or what is stored is not an address). */
    public static LastTv load(Store s) {
        String ip = s.get("last_ip");
        if (ip == null || !IPV4.matcher(ip).matches()) return null;
        String name = s.get("last_name");
        return new LastTv(ip, name == null || name.trim().isEmpty() ? ip : name.trim());
    }
}

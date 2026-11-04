package ma.chouitv.installer;

import java.util.List;

/**
 * Which network the search for TVs looks at. A phone can have several private addresses at once (the Wi-Fi of the home and, on some phones, the mobile data
 * too, like 10.x.x.x on a "rmnet" interface): the search must take the Wi-Fi, not whichever came last. Pure, tested on the JVM (SubnetTest).
 */
final class Subnet {
    private Subnet() {}

    private static boolean mobile(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("pdp") || n.startsWith("ppp") || n.startsWith("tun") || n.startsWith("dummy")
            || n.startsWith("v4-") || n.startsWith("lo");
    }

    private static boolean home(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.startsWith("wlan") || n.startsWith("eth") || n.startsWith("en") || n.startsWith("wifi") || n.startsWith("wl");
    }

    /** A private IPv4 address (10/8, 172.16/12, 192.168/16): the only ones a home network uses. */
    static boolean isPrivate(String ip) {
        String[] p = ip == null ? new String[0] : ip.split("\\.");
        if (p.length != 4) return false;
        int a, b;
        try {
            a = Integer.parseInt(p[0]);
            b = Integer.parseInt(p[1]);
            Integer.parseInt(p[2]);
            Integer.parseInt(p[3]);
        } catch (NumberFormatException e) { return false; }
        return a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168);
    }

    /** {own address, "a.b.c."} of the network to search, from the pairs {interface name, IPv4 address}; null when there is none to search. Wi-Fi and Ethernet first. */
    static String[] pick(List<String[]> ifaces) {
        String[] fallback = null;
        if (ifaces == null) return null;
        for (String[] i : ifaces) {
            if (i == null || i.length < 2 || !isPrivate(i[1])) continue;
            String[] r = {i[1], i[1].substring(0, i[1].lastIndexOf('.') + 1)};
            if (home(i[0])) return r;
            if (fallback == null && !mobile(i[0])) fallback = r;
        }
        return fallback;
    }
}

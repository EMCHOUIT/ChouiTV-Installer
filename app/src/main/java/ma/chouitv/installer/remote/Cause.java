package ma.chouitv.installer.remote;

/**
 * Why the TV does not answer, told from what the phone knows (its Wi-Fi, its network, whether the TV asked for the key) and from how the connection failed.
 * The words of the failure are the ones Android gives ("Connection refused", "No route to host", a timeout). Pure, tested on the JVM (CauseTest).
 */
public enum Cause {
    /** The phone is not on a Wi-Fi network. */
    NO_WIFI,
    /** The phone is on a Wi-Fi network, but not the one of the TV. */
    OTHER_NETWORK,
    /** The TV is there but refuses the connection: its network debugging is off, or it is starting. */
    REFUSED,
    /** Nothing answers: the TV is off or asleep. */
    SILENT,
    /** The TV asks the person to accept this phone, and waits. */
    AUTH;

    public static Cause of(Throwable failure, boolean wifiActive, boolean sameNetwork, boolean keyAsked) {
        if (!wifiActive) return NO_WIFI;
        if (!sameNetwork) return OTHER_NETWORK;
        if (keyAsked) return AUTH;
        return refused(failure) ? REFUSED : SILENT;
    }

    private static boolean refused(Throwable e) {
        for (int depth = 0; e != null && depth < 6; depth++, e = e.getCause()) {
            String m = e.getMessage();
            if (m != null) {
                String l = m.toLowerCase(java.util.Locale.ROOT);
                if (l.contains("refused") || l.contains("econnrefused")) return true;
            }
        }
        return false;
    }
}

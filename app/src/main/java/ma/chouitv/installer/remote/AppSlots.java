package ma.chouitv.installer.remote;

import ma.chouitv.installer.cleanup.Guard;

/** The six buttons of the Applications pop-up: each one holds the package of an app of the TV (or nothing). Immutable; kept on the phone, one set per TV. */
public final class AppSlots {
    public static final int COUNT = 6;
    private final String[] pk;

    private AppSlots(String[] pk) { this.pk = pk; }

    public String get(int i) { return pk[i]; }

    public AppSlots with(int i, String pkg) {
        if (i < 0 || i >= COUNT) throw new IndexOutOfBoundsException("slot " + i);
        if (pkg != null && !Guard.validPackage(pkg)) throw new IllegalArgumentException("not a package name: " + pkg);
        String[] c = pk.clone();
        c[i] = pkg;
        return new AppSlots(c);
    }

    /** The slot that holds this package, or -1. */
    public int slotOf(String pkg) {
        for (int i = 0; i < COUNT; i++) if (pkg != null && pkg.equals(pk[i])) return i;
        return -1;
    }

    public String serialize() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < COUNT; i++) { if (i > 0) b.append(','); if (pk[i] != null) b.append(pk[i]); }
        return b.toString();
    }

    /** Reads what {@link #serialize} wrote; anything that is not a package name becomes an empty slot. */
    public static AppSlots parse(String s) {
        String[] c = new String[COUNT];
        if (s != null) {
            String[] parts = s.split(",", -1);
            for (int i = 0; i < COUNT && i < parts.length; i++) c[i] = Guard.validPackage(parts[i]) ? parts[i] : null;
        }
        return new AppSlots(c);
    }

    public static AppSlots load(LastTv.Store st, String tv) { return parse(st.get("slots_" + tv)); }

    public static void save(LastTv.Store st, String tv, AppSlots a) { st.put("slots_" + tv, a.serialize()); }
}

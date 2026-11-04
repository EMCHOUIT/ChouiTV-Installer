package ma.chouitv.installer.cleanup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The last look before a clean-up: the names of the apps that will be turned off, one per line, the long lists cut with the number of the others. */
public final class Review {
    private Review() {}

    /** {@code others} is a format with one %d, for example «%d autres». */
    public static String lines(List<String> names, int limit, String others) {
        if (names == null) return "";
        Set<String> seen = new LinkedHashSet<>();
        for (String n : names) if (n != null && !n.trim().isEmpty()) seen.add(n.trim());
        List<String> all = new ArrayList<>(seen);
        int shown = Math.max(1, limit);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < all.size() && i < shown; i++) { if (b.length() > 0) b.append('\n'); b.append("• ").append(all.get(i)); }
        if (all.size() > shown) b.append("\n+ ").append(String.format(Locale.ENGLISH, others, all.size() - shown));
        return b.toString();
    }
}

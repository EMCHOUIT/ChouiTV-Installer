package ma.chouitv.installer.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** What the TV answers to an import: the preview of the changes (nothing applied yet), or the result of the apply. */
public final class Preview {
    public final List<String> changes = new ArrayList<>();
    public String error;
    public boolean nothing;
    public int count;

    private static final Pattern N = Pattern.compile("(\\d+) change");

    /** «preview: 9 change(s); …\n- a\n- b» · «preview: nothing would change» · «error: …». */
    public static Preview parse(String out) {
        Preview p = new Preview();
        String s = out == null ? "" : out.trim();
        if (s.startsWith("error:")) { p.error = s.substring(6).trim(); return p; }
        if (s.startsWith("preview: nothing would change")) { p.nothing = true; return p; }
        if (s.startsWith("preview:") || s.startsWith("applied:")) {
            Matcher m = N.matcher(s);
            if (m.find()) p.count = Integer.parseInt(m.group(1));
            for (String line : s.split("\n")) {
                line = line.trim();
                if (line.startsWith("- ")) p.changes.add(line.substring(2).trim());
            }
            if (p.count == 0) p.count = p.changes.size();
            return p;
        }
        p.error = s.isEmpty() ? "no answer" : s;
        return p;
    }
}

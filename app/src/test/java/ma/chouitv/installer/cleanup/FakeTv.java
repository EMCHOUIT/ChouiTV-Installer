package ma.chouitv.installer.cleanup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A TV in memory for the tests: it keeps the packages that are off, refuses what it is told to, and can lose its network. */
final class FakeTv implements Shell {
    final Set<String> disabled = new HashSet<>();
    final Set<String> refuse = new HashSet<>();      // «pm disable-user» of these answers «illegal» and changes nothing
    final List<String> log = new ArrayList<>();
    boolean homeOk = true, networkOk = true, answers = true;
    int breakAfterDisables = -1;                      // the network drops once this many packages were turned off
    int disables;
    final Set<String> files = new HashSet<>();

    @Override public String run(String c) throws IOException {
        log.add(c);
        String bad = Guard.refusal(c);
        if (bad != null) throw new AssertionError("the guard would refuse this command: " + c);
        if (!answers) throw new IOException("timed out");
        if (c.equals("echo ok")) return "ok";
        if (c.startsWith("cmd package resolve-activity")) return homeOk ? "ma.chouitv.home/.MainActivity" : "";
        if (c.startsWith("dumpsys connectivity")) return networkOk ? "2" : "0";
        if (c.equals("pm list packages -d")) { StringBuilder b = new StringBuilder(); for (String p : disabled) b.append("package:").append(p).append("\n"); return b.toString(); }
        if (c.startsWith("pm disable-user --user 0 ")) {
            String p = c.substring("pm disable-user --user 0 ".length());
            if (refuse.contains(p)) return "Exception occurred while executing: This command is illegal";
            disabled.add(p);
            if (++disables == breakAfterDisables) homeOk = false;
            return "Package " + p + " new state: disabled-user";
        }
        if (c.startsWith("pm enable ")) { disabled.remove(c.substring("pm enable ".length())); return "Package enabled"; }
        if (c.startsWith("touch ")) { files.add(c.substring(6)); return ""; }
        return "";
    }
}

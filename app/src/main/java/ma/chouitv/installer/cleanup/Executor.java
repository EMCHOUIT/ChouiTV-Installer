package ma.chouitv.installer.cleanup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ma.chouitv.installer.cleanup.CleanupData.Group;
import ma.chouitv.installer.cleanup.Planner.Item;
import ma.chouitv.installer.cleanup.Planner.Plan;

/**
 * Runs a plan: group by group, the least risky first, each package checked after it is turned off, and a health check between
 * the groups. A group that leaves the TV unhealthy is put back at once and the run stops. The safety net on the TV is armed
 * before the first change and confirmed only when everything went well.
 */
public final class Executor {
    public interface Listener {
        void started(int total);
        void package_(Group g, String pkg, boolean ok, int done, int total);
        void groupDone(Group g, boolean healthy);
    }

    public interface Sleeper { void sleep(long ms) throws InterruptedException; }

    public static final class Result {
        public final List<String> disabled = new ArrayList<>(), refused = new ArrayList<>();
        public Group rolledBack;   // set when a group left the TV unhealthy and was put back
        public boolean netLost;
    }

    private final Shell tv;
    private final Sleeper sleeper;
    private final String token;

    public Executor(Shell tv, Sleeper sleeper, String token) { this.tv = tv; this.sleeper = sleeper; this.token = token; }

    /** The TV answers, has a home and a network. Retries a few times: a Wi-Fi that blinks is not a failure. */
    public boolean healthy() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                if (!tv.run("echo ok").trim().endsWith("ok")) throw new IOException("no answer");
                String home = tv.run("cmd package resolve-activity --brief -c android.intent.category.HOME -a android.intent.action.MAIN");
                if (home == null || home.indexOf('/') < 0) return false;
                String net = tv.run("dumpsys connectivity | grep -c CONNECTED");
                int n = 0;
                try { n = Integer.parseInt(net.trim().split("\\s+")[0]); } catch (NumberFormatException ignored) { }
                return n > 0;
            } catch (IOException e) {
                try { sleeper.sleep(2500); } catch (InterruptedException ie) { return false; }
            }
        }
        return false;
    }

    /** A run by group: every package of the chosen groups. */
    public Result run(Plan plan, Set<Group> chosen, Listener l) throws IOException {
        return runSelected(plan, plan.packagesOf(chosen), l);
    }

    /** A run of exactly the ticked packages, still group by group (the least risky first) with a health check between the groups. */
    public Result runSelected(Plan plan, Set<String> selected, Listener l) throws IOException {
        Result r = new Result();
        List<Item> all = new ArrayList<>();
        for (Group g : EnumSet.allOf(Group.class)) for (Item i : plan.groups.get(g)) if (selected.contains(i.pkg)) all.add(i);
        if (all.isEmpty()) return r;
        List<String> names = new ArrayList<>();
        for (Item i : all) names.add(i.pkg);
        Deadman.arm(tv, token, names);
        l.started(all.size());
        int done = 0;
        for (Group g : EnumSet.allOf(Group.class)) {
            List<String> mine = new ArrayList<>();
            boolean any = false;
            for (Item it : plan.groups.get(g)) if (selected.contains(it.pkg)) any = true;
            if (!any) continue;
            for (Item it : plan.groups.get(g)) {
                if (!selected.contains(it.pkg)) continue;
                String cmd = Guard.disable(it.pkg);
                if (Guard.refusal(cmd) != null) { r.refused.add(it.pkg); done++; l.package_(g, it.pkg, false, done, all.size()); continue; }
                String out = tv.run(cmd);
                mine.add(it.pkg);
                done++;
                l.package_(g, it.pkg, true, done, all.size());
            }
            Set<String> off = offNow();
            for (String p : mine) { if (off.contains(p)) r.disabled.add(p); else r.refused.add(p); }
            boolean ok = healthy();
            l.groupDone(g, ok);
            if (!ok) {
                r.rolledBack = g;
                for (String p : mine) { try { tv.run(Guard.enable(p)); } catch (IOException e) { r.netLost = true; } }
                r.disabled.removeAll(mine);
                if (!r.netLost && healthy()) Deadman.confirm(tv, token);   // the TV is back: the net can stand down
                return r;
            }
        }
        Deadman.confirm(tv, token);
        return r;
    }

    private Set<String> offNow() throws IOException {
        Set<String> s = new HashSet<>();
        for (String line : tv.run("pm list packages -d").split("\n")) {
            line = line.trim();
            if (line.startsWith("package:")) s.add(line.substring(8));
        }
        return s;
    }
}

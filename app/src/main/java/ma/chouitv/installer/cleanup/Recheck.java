package ma.chouitv.installer.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * «Check again after a restart»: what a cleanup turned off, compared with what is off now. Some TVs turn apps back on by themselves
 * at every start (Netflix and Prime Video on a Xiaomi MiTV-MSSP3): those are told apart from the ones that stayed off, so the person is
 * never told that something is off when it is not.
 */
public final class Recheck {
    public final List<String> stillOff = new ArrayList<>();
    public final List<String> backOn = new ArrayList<>();      // the TV turned them on again: disabling does not hold on this TV
    public final List<String> suspects = new ArrayList<>();    // still off and they had a service running before: the first to put back if a TV feature fails

    public static Recheck compare(Restore r, Set<String> disabledNow) {
        Recheck c = new Recheck();
        for (String p : r.packages) {
            if (disabledNow.contains(p)) {
                c.stillOff.add(p);
                if (r.servicesBefore.contains(p)) c.suspects.add(p);
            } else {
                c.backOn.add(p);
            }
        }
        return c;
    }

    /** True when putting packages back needs a restart of the TV to take effect: a system service that was off at boot does not start by itself (seen on a MiTV-MSSP3: the picture menu stays blocked until the TV restarts). */
    public static boolean needsRestartAfterEnable(Restore r, List<String> enabled) {
        for (String p : enabled) if (r.servicesBefore.contains(p)) return true;
        return false;
    }
}

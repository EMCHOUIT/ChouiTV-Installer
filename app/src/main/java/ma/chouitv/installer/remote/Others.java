package ma.chouitv.installer.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** The other TVs of the family that answer when the one used last does not: offered, never taken on its own. */
public final class Others {
    private Others() {}

    public static List<TvList.Tv> answering(List<TvList.Tv> all, String currentIp, Set<String> answering) {
        List<TvList.Tv> r = new ArrayList<>();
        if (all == null || answering == null) return r;
        for (TvList.Tv t : all) if (!t.ip.equals(currentIp) && answering.contains(t.ip)) r.add(t);
        return r;
    }
}

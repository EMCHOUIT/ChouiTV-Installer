package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

/** When the TV used last does not answer but another TV of the family does: it is offered, never taken on its own. */
public class OthersTest {
    private static List<TvList.Tv> tvs() {
        LastTvMem s = new LastTvMem();
        TvList.use(s, "192.168.1.52", "Samsung");
        TvList.use(s, "192.168.1.51", "TCL");
        TvList.use(s, "192.168.1.50", "Xiaomi");
        return TvList.load(s);
    }

    private static final class LastTvMem implements LastTv.Store {
        final java.util.Map<String, String> m = new java.util.HashMap<>();
        @Override public String get(String k) { return m.get(k); }
        @Override public void put(String k, String v) { m.put(k, v); }
    }

    @Test public void theOtherTvsThatAnswerAreListedInTheOrderOfTheFamilyList() {
        List<TvList.Tv> r = Others.answering(tvs(), "192.168.1.50", new HashSet<>(Arrays.asList("192.168.1.52", "192.168.1.51")));
        assertEquals(2, r.size());
        assertEquals("192.168.1.51", r.get(0).ip);
        assertEquals("192.168.1.52", r.get(1).ip);
    }

    @Test public void theTvThatDoesNotAnswerIsNeverListedAndNeitherIsASilentOne() {
        assertEquals(0, Others.answering(tvs(), "192.168.1.50", new HashSet<>(Arrays.asList("192.168.1.50"))).size());
        assertEquals(0, Others.answering(tvs(), "192.168.1.50", new HashSet<String>()).size());
    }
}

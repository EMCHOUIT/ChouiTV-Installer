package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TvLogTest {
    private static final class Mem implements LastTv.Store {
        final Map<String, String> m = new HashMap<>();
        @Override public String get(String k) { return m.get(k); }
        @Override public void put(String k, String v) { m.put(k, v); }
    }

    @Test public void whatWasDoneToATvIsKeptNewestFirst() {
        Mem s = new Mem();
        TvLog.add(s, "192.168.1.50", 1000L, TvLog.CLEANUP, "5");
        TvLog.add(s, "192.168.1.50", 2000L, TvLog.INSTALL, "4.2");
        List<TvLog.Entry> l = TvLog.load(s, "192.168.1.50");
        assertEquals(2, l.size());
        assertEquals(TvLog.INSTALL, l.get(0).code);
        assertEquals("4.2", l.get(0).detail);
        assertEquals(1000L, l.get(1).time);
    }

    @Test public void eachTvHasItsOwnJournalAndItIsCapped() {
        Mem s = new Mem();
        for (int i = 0; i < 45; i++) TvLog.add(s, "10.0.0.1", i, TvLog.CLEANUP, String.valueOf(i));
        TvLog.add(s, "10.0.0.2", 1L, TvLog.REACTIVATE, "2");
        assertEquals(TvLog.MAX, TvLog.load(s, "10.0.0.1").size());
        assertEquals("44", TvLog.load(s, "10.0.0.1").get(0).detail);   // the newest is kept, the oldest are dropped
        assertEquals(1, TvLog.load(s, "10.0.0.2").size());
    }

    @Test public void aDetailCannotBreakTheStorageAndNonsenseIsIgnored() {
        Mem s = new Mem();
        TvLog.add(s, "10.0.0.1", 5L, TvLog.SETTINGS_SAVED, "Salon | v2\nsecond line");
        List<TvLog.Entry> l = TvLog.load(s, "10.0.0.1");
        assertEquals(1, l.size());
        assertEquals("Salon v2 second line", l.get(0).detail);
        s.put("log_10.0.0.1", "garbage\n||\nx|notanumber|y\n" + l.get(0).time + "|" + TvLog.CLEANUP + "|3");
        assertTrue(TvLog.load(s, "10.0.0.1").size() >= 1);
        assertTrue(TvLog.load(s, null).isEmpty());
        assertTrue(TvLog.load(s, "nobody").isEmpty());
    }
}

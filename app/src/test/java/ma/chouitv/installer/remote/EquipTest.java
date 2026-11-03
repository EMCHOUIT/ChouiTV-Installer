package ma.chouitv.installer.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EquipTest {
    private static final class Mem implements LastTv.Store {
        final Map<String, String> m = new HashMap<>();
        @Override public String get(String k) { return m.get(k); }
        @Override public void put(String k, String v) { m.put(k, v); }
    }

    @Test public void aTvSeenForTheFirstTimeIsAskedWhatToDoOnlyOnce() {
        Mem s = new Mem();
        assertTrue(Equip.shouldAsk(s, "192.168.1.50"));
        Equip.markAsked(s, "192.168.1.50");
        assertFalse(Equip.shouldAsk(s, "192.168.1.50"));
        assertTrue(Equip.shouldAsk(s, "192.168.1.51"));   // another TV is a new question
    }

    @Test public void aTvThatWasEquippedIsNeverAskedAndNeverOffered() {
        Mem s = new Mem();
        Equip.markEquipped(s, "192.168.1.50");
        assertTrue(Equip.isEquipped(s, "192.168.1.50"));
        assertFalse(Equip.shouldAsk(s, "192.168.1.50"));
        assertFalse(Equip.offers(s, "192.168.1.50"));
        assertTrue(Equip.offers(s, "192.168.1.51"));
    }

    @Test public void theOfferStaysOnThePageOfATvUntilItIsEquipped() {
        Mem s = new Mem();
        Equip.markAsked(s, "192.168.1.50");               // the family said «only the remote»
        assertTrue(Equip.offers(s, "192.168.1.50"));      // the page of the TV still offers it
    }

    @Test public void nonsenseGivesNoOfferAndNoCrash() {
        Mem s = new Mem();
        assertFalse(Equip.offers(s, ""));
        assertFalse(Equip.offers(s, null));
        assertFalse(Equip.shouldAsk(s, null));
    }
}

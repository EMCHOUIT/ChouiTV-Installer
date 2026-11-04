package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TvListTest {
    private static final class Mem implements LastTv.Store {
        final Map<String, String> m = new HashMap<>();
        @Override public String get(String k) { return m.get(k); }
        @Override public void put(String k, String v) { m.put(k, v); }
    }

    @Test public void aTvIsAddedWithItsDefaultNameAndTheLastUsedComesFirst() {
        Mem s = new Mem();
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        TvList.use(s, "192.168.1.51", "Xiaomi MiTV-MSSP3");
        List<TvList.Tv> l = TvList.load(s);
        assertEquals(2, l.size());
        assertEquals("192.168.1.51", l.get(0).ip);
        assertEquals("192.168.1.50", l.get(1).ip);
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");   // used again: back to the front, no duplicate
        l = TvList.load(s);
        assertEquals(2, l.size());
        assertEquals("192.168.1.50", l.get(0).ip);
    }

    @Test public void theNameTheFamilyChoseSurvivesWhenTheTvIsUsedAgain() {
        Mem s = new Mem();
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        TvList.rename(s, "192.168.1.50", "Salon");
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        assertEquals("Salon", TvList.load(s).get(0).name);
        assertEquals("Salon", LastTv.load(s).name);   // the pill of the remote reads the same name
    }

    @Test public void aRenameIsCleanedAndAnEmptyNameGoesBackToTheModel() {
        Mem s = new Mem();
        TvList.use(s, "10.0.0.7", "Sony BRAVIA");
        TvList.rename(s, "10.0.0.7", "  Chambre | des parents\n ");
        assertEquals("Chambre des parents", TvList.load(s).get(0).name);
        TvList.rename(s, "10.0.0.7", "   ");
        assertEquals("Sony BRAVIA", TvList.load(s).get(0).name);
    }

    @Test public void aTvCanBeForgottenAndTheOtherOneBecomesTheCurrent() {
        Mem s = new Mem();
        TvList.use(s, "192.168.1.50", "A");
        TvList.use(s, "192.168.1.51", "B");
        TvList.forget(s, "192.168.1.51");
        assertEquals(1, TvList.load(s).size());
        assertEquals("192.168.1.50", LastTv.load(s).ip);
        TvList.forget(s, "192.168.1.50");
        assertTrue(TvList.load(s).isEmpty());
        assertNull(LastTv.load(s));
    }

    @Test public void anOldAppWithOneRememberedTvIsReadAsAList() {
        Mem s = new Mem();
        LastTv.save(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");   // what versions before this one stored
        List<TvList.Tv> l = TvList.load(s);
        assertEquals(1, l.size());
        assertEquals("192.168.1.50", l.get(0).ip);
        assertEquals("Xiaomi MiTV-MSSP3", l.get(0).name);
    }

    @Test public void nonsenseInTheStoreGivesAnEmptyListNotACrash() {
        Mem s = new Mem();
        s.put("tvs", "garbage\n||\nnot an ip|x|y\n192.168.1.9|Salon|zz");
        List<TvList.Tv> l = TvList.load(s);
        assertEquals(1, l.size());
        assertEquals("192.168.1.9", l.get(0).ip);
    }

    @Test public void theSerialIsKeptWithTheTvAndSurvivesRenamingAndUse() {
        Mem s = new Mem();
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        TvList.setSerial(s, "192.168.1.50", "SN123");
        assertEquals("SN123", TvList.load(s).get(0).serial);
        TvList.rename(s, "192.168.1.50", "Salon");
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        assertEquals("SN123", TvList.load(s).get(0).serial);
        assertEquals("Salon", TvList.load(s).get(0).name);
    }

    @Test public void aListSavedBeforeTheSerialExistedStillLoads() {
        Mem s = new Mem();
        s.put("tvs", "192.168.1.50|Xiaomi MiTV-MSSP3|Salon\n192.168.1.51|TCL|");
        List<TvList.Tv> l = TvList.load(s);
        assertEquals(2, l.size());
        assertEquals("", l.get(0).serial);
        assertEquals("Salon", l.get(0).name);
    }

    @Test public void aTvThatMovedKeepsItsNameItsSerialAndItsPlaceInTheList() {
        Mem s = new Mem();
        TvList.use(s, "192.168.1.51", "TCL");
        TvList.use(s, "192.168.1.50", "Xiaomi MiTV-MSSP3");
        TvList.rename(s, "192.168.1.50", "Salon");
        TvList.setSerial(s, "192.168.1.50", "SN123");
        TvList.moveIp(s, "192.168.1.50", "192.168.1.77");
        List<TvList.Tv> l = TvList.load(s);
        assertEquals(2, l.size());
        assertEquals("192.168.1.77", l.get(0).ip);
        assertEquals("Salon", l.get(0).name);
        assertEquals("SN123", l.get(0).serial);
        assertEquals("the TV that was used last is still the one used last", "192.168.1.77", LastTv.load(s).ip);
        assertEquals("192.168.1.51", l.get(1).ip);
    }

    @Test public void theSettingsOfATvFollowItToItsNewAddress() {
        assertEquals("slots_192.168.1.77", TvList.migrateKey("slots_192.168.1.50", "192.168.1.50", "192.168.1.77"));
        assertEquals("vol_scale_192.168.1.77", TvList.migrateKey("vol_scale_192.168.1.50", "192.168.1.50", "192.168.1.77"));
        assertEquals("equipped_192.168.1.77", TvList.migrateKey("equipped_192.168.1.50", "192.168.1.50", "192.168.1.77"));
        assertNull("another TV", TvList.migrateKey("slots_192.168.1.5", "192.168.1.50", "192.168.1.77"));
        assertNull("a key of the app", TvList.migrateKey("vol_limit", "192.168.1.50", "192.168.1.77"));
        assertNull("not for a longer address", TvList.migrateKey("slots_192.168.1.150", "192.168.1.50", "192.168.1.77"));
    }
}

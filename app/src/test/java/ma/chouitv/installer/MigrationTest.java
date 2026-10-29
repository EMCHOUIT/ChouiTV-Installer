package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/** 3.x to 4.0: the old screensaver's settings move into the merged app, and the old app goes only when the person says so. */
public class MigrationTest {
    private static final String SETTINGS = "{\"app\":\"ChouiTV\",\"city\":\"Casablanca\"}";

    /** A TV in memory: which packages it has, what the old app exports, and what the new one accepts. */
    private static final class FakeTv implements Migration.Tv {
        final Set<String> packages = new HashSet<>();
        final List<String> log = new ArrayList<>();
        String exported = SETTINGS;
        boolean exportFails, importFails, dreamFails, uninstallFails, uninstallDoesNothing;
        String imported;

        FakeTv(String... pkgs) { packages.addAll(Arrays.asList(pkgs)); }

        @Override public boolean installed(String pkg) { return packages.contains(pkg); }
        @Override public String exportOld() throws IOException { log.add("export"); if (exportFails) throw new IOException("old app does not answer"); return exported; }
        @Override public void importInto(String json) throws IOException { log.add("import"); if (importFails) throw new IOException("error: refused"); imported = json; }
        @Override public void setDream() throws IOException { log.add("dream"); if (dreamFails) throw new IOException("settings refused"); }
        @Override public void uninstall(String pkg) throws IOException {
            log.add("uninstall " + pkg);
            if (uninstallFails) throw new IOException("Failure");
            if (!uninstallDoesNothing) packages.remove(pkg);
        }
    }

    private static final class MemStore implements Migration.Store {
        String json;
        @Override public void save(String j) { json = j; }
        @Override public String load() { return json; }
        @Override public void clear() { json = null; }
    }

    private static final class Person implements Flow.Ask {
        final boolean yes;
        final List<String> asked = new ArrayList<>();
        Person(boolean yes) { this.yes = yes; }
        @Override public boolean allow(String pkg) { asked.add(pkg); return yes; }
    }

    private static Migration.Outcome run(FakeTv tv, MemStore store, Flow.Ask ask) throws IOException {
        Migration.State st = Migration.before(tv, store);
        // 4.0 gets installed between the two calls
        tv.packages.add(Flow.HOME);
        return Migration.after(tv, store, st, ask);
    }

    @Test public void aTvWithBothAppsMovesItsSettingsAndRemovesTheOldAppAfterAYes() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER, "ma.chouitv.home", Flow.CHANNELS);
        MemStore store = new MemStore();
        Person yes = new Person(true);
        Migration.State st = Migration.before(tv, store);
        assertTrue(st.oldPresent); assertTrue(st.saved);
        assertEquals("the settings wait in the store until 4.0 is in", SETTINGS, store.json);
        assertEquals("nothing was changed on the TV before the install", Arrays.asList("export"), tv.log);
        assertEquals(Migration.Outcome.REMOVED, Migration.after(tv, store, st, yes));
        assertEquals(Arrays.asList("export", "import", "dream", "uninstall " + Flow.OLD_SAVER), tv.log);
        assertEquals(SETTINGS, tv.imported);
        assertEquals(Arrays.asList(Flow.OLD_SAVER), yes.asked);
        assertFalse(tv.packages.contains(Flow.OLD_SAVER));
        assertNull("the copy of the settings does not stay on the phone", store.json);
    }

    @Test public void aTvWithOnly40HasNothingToDo() throws Exception {
        FakeTv tv = new FakeTv(Flow.HOME, Flow.CHANNELS);
        MemStore store = new MemStore();
        Person yes = new Person(true);
        assertEquals(Migration.Outcome.NOTHING, run(tv, store, yes));
        assertTrue("not even a broadcast", tv.log.isEmpty());
        assertTrue("nobody is asked", yes.asked.isEmpty());
        assertNull(store.json);
    }

    @Test public void aFreshTvHasNothingToDo() throws Exception {
        FakeTv tv = new FakeTv();
        assertEquals(Migration.Outcome.NOTHING, run(tv, new MemStore(), new Person(true)));
        assertTrue(tv.log.isEmpty());
    }

    @Test public void ifTheImportFailsTheOldAppStaysAndNobodyIsAsked() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER, Flow.HOME);
        tv.importFails = true;
        Person yes = new Person(true);
        assertEquals(Migration.Outcome.IMPORT_FAILED, run(tv, new MemStore(), yes));
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
        assertTrue(yes.asked.isEmpty());
        assertFalse(tv.log.contains("uninstall " + Flow.OLD_SAVER));
    }

    @Test public void ifTheDreamCannotBeSetTheOldAppStaysToo() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        tv.dreamFails = true;
        Person yes = new Person(true);
        assertEquals(Migration.Outcome.IMPORT_FAILED, run(tv, new MemStore(), yes));
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
        assertTrue(yes.asked.isEmpty());
    }

    @Test public void ifTheNewAppIsNotThereTheOldAppStays() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        MemStore store = new MemStore();
        Migration.State st = Migration.before(tv, store);
        Person yes = new Person(true);
        assertEquals("4.0 never arrived", Migration.Outcome.IMPORT_FAILED, Migration.after(tv, store, st, yes));
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
        assertFalse(tv.log.contains("import"));
    }

    @Test public void ifThePersonDeclinesTheOldAppStaysAndTheSettingsAreStillMoved() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        Person no = new Person(false);
        assertEquals(Migration.Outcome.KEPT, run(tv, new MemStore(), no));
        assertEquals(Arrays.asList(Flow.OLD_SAVER), no.asked);
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
        assertEquals(SETTINGS, tv.imported);
        assertFalse(tv.log.contains("uninstall " + Flow.OLD_SAVER));
    }

    @Test public void withoutAScreenToAskNothingIsRemoved() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        assertEquals(Migration.Outcome.KEPT, run(tv, new MemStore(), null));
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
    }

    @Test public void ifTheOldSettingsCannotBeReadNothingIsImportedAndTheOldAppStays() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        tv.exportFails = true;
        Person yes = new Person(true);
        assertEquals(Migration.Outcome.NOT_SAVED, run(tv, new MemStore(), yes));
        assertTrue(tv.packages.contains(Flow.OLD_SAVER));
        assertFalse(tv.log.contains("import"));
        assertTrue(yes.asked.isEmpty());
    }

    @Test public void anExportThatIsNotASettingsObjectIsNotKept() throws Exception {
        for (String bad : new String[] {"", "   ", "error: no", "{}", null, "[1,2]"}) {
            FakeTv tv = new FakeTv(Flow.OLD_SAVER);
            tv.exported = bad;
            MemStore store = new MemStore();
            Migration.State st = Migration.before(tv, store);
            assertFalse("[" + bad + "]", st.saved);
            assertNull(store.json);
            assertEquals(Migration.Outcome.NOT_SAVED, Migration.after(tv, store, st, new Person(true)));
        }
    }

    @Test public void aRemovalTheTvDoesNotDoIsReportedNotHidden() throws Exception {
        FakeTv tv = new FakeTv(Flow.OLD_SAVER);
        tv.uninstallFails = true;
        assertEquals(Migration.Outcome.REMOVE_FAILED, run(tv, new MemStore(), new Person(true)));
        FakeTv quiet = new FakeTv(Flow.OLD_SAVER);
        quiet.uninstallDoesNothing = true;
        assertEquals("a Success that left the app there is not a removal", Migration.Outcome.REMOVE_FAILED, run(quiet, new MemStore(), new Person(true)));
    }

    @Test public void aSecondAttemptNeverUsesAFileLeftByAnotherTv() throws Exception {
        MemStore store = new MemStore();
        store.json = "{\"city\":\"OtherTvLeftThis\"}";
        FakeTv tv = new FakeTv(Flow.HOME);   // this TV has no old app
        Migration.State st = Migration.before(tv, store);
        assertNull(store.json);
        assertEquals(Migration.Outcome.NOTHING, Migration.after(tv, store, st, new Person(true)));
        assertNull(tv.imported);
    }

    @Test public void onlyTheOldScreensaverCanBeTheSubjectOfARemoval() throws Exception {
        // the plan names one package and never another, whatever the TV has
        FakeTv tv = new FakeTv(Flow.OLD_SAVER, Flow.CHANNELS, "ma.chouitv.installer");
        run(tv, new MemStore(), new Person(true));
        for (String l : tv.log) if (l.startsWith("uninstall ")) assertEquals("uninstall " + Flow.OLD_SAVER, l);
        assertTrue(tv.packages.contains(Flow.CHANNELS));
        assertTrue(tv.packages.contains(Flow.HOME));
    }

    @Test public void theFileStoreKeepsAndForgets() throws Exception {
        java.io.File f = java.io.File.createTempFile("migration", ".json");
        Migration.Store s = Migration.fileStore(f);
        s.save(SETTINGS);
        assertEquals(SETTINGS, s.load());
        s.clear();
        assertFalse(f.exists());
    }
}

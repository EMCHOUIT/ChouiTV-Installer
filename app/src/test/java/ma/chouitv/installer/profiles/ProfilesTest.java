package ma.chouitv.installer.profiles;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Named settings profiles: kept on the phone, named, renamed, deleted; and what the TV answers to a preview. */
public class ProfilesTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    // a real export, cut down: what a TV prints for the EXPORT broadcast
    private static final String EXPORT = "{\n  \"app\": \"ChouiTV\",\n  \"schema\": 1,\n  \"version\": \"3.173\",\n  \"settings\": {\n    \"cityName\": \"الدار البيضاء\",\n    \"panel\": \"verse\"\n  },\n"
        + "  \"home\": {\n    \"adhan\": {\"adhan_volume\": 20},\n    \"dock\": {\"dock\": \"com.a.b,com.c.d\"}\n  }\n}";

    private ProfileStore store() throws IOException { return new ProfileStore(new File(tmp.getRoot(), "profiles")); }

    @Test public void anExportIsValidatedBeforeItIsKept() {
        assertNull(ProfileStore.validate(EXPORT));
        assertNotNull(ProfileStore.validate(""));
        assertNotNull(ProfileStore.validate(null));
        assertNotNull(ProfileStore.validate("not json"));
        assertNotNull(ProfileStore.validate("{\"app\":\"Other\",\"schema\":1,\"settings\":{}}"));
        assertNotNull(ProfileStore.validate("{\"app\":\"ChouiTV\",\"schema\":2,\"settings\":{}}"));
        assertNotNull(ProfileStore.validate("{\"app\":\"ChouiTV\",\"schema\":1}"));
    }

    @Test public void aProfileIsSavedWithItsNameAndComesBackIdentical() throws Exception {
        ProfileStore s = store();
        Profile p = s.save("Salon", "2026-10-28 16:00", "MiTV-MSSP3", EXPORT);
        assertEquals("Salon", p.name); assertEquals("3.173", p.version);
        List<Profile> all = s.list();
        assertEquals(1, all.size());
        assertEquals(p.id, all.get(0).id);
        assertEquals("MiTV-MSSP3", all.get(0).tv);
        assertNull("what comes back is still a valid export", ProfileStore.validate(all.get(0).settingsJson));
        assertTrue(all.get(0).settingsJson.contains("الدار البيضاء"));
    }

    @Test public void theNewestComesFirstAndTheSameNameGetsANumber() throws Exception {
        ProfileStore s = store();
        s.save("Salon", "2026-09-01 10:00", "a", EXPORT);
        Profile second = s.save("salon", "2026-10-28 10:00", "b", EXPORT);   // the same name, whatever the case
        s.save("Chambre", "2026-07-01 10:00", "c", EXPORT);
        assertEquals("salon (2)", second.name);
        List<Profile> all = s.list();
        assertEquals("salon (2)", all.get(0).name);
        assertEquals("Salon", all.get(1).name);
        assertEquals("Chambre", all.get(2).name);
    }

    @Test public void aProfileIsRenamedKeepingItsContentAndNeverTakesAnotherNamesPlace() throws Exception {
        ProfileStore s = store();
        Profile a = s.save("Salon", "2026-10-28 10:00", "x", EXPORT);
        s.save("Chambre", "2026-10-27 10:00", "y", EXPORT);
        Profile r = s.rename(a.id, "Chambre");
        assertEquals("Chambre (2)", r.name);
        assertEquals("the content is untouched by a rename", new org.json.JSONObject(a.settingsJson).toString(), new org.json.JSONObject(s.find(a.id).settingsJson).toString());
        assertEquals("renaming it to its own name keeps it", "Chambre (2)", s.rename(a.id, "Chambre (2)").name);
        assertEquals(2, s.list().size());
        try { s.rename("nope", "x"); fail(); } catch (IOException expected) { }
    }

    @Test public void deletingRemovesOnlyThatProfile() throws Exception {
        ProfileStore s = store();
        Profile a = s.save("A", "2026-10-28 10:00", "x", EXPORT);
        Profile b = s.save("B", "2026-10-27 10:00", "x", EXPORT);
        assertTrue(s.delete(a.id));
        assertFalse("already gone", s.delete(a.id));
        assertEquals(1, s.list().size());
        assertEquals(b.id, s.list().get(0).id);
        assertFalse("a path in an id never leaves the folder", s.delete("../../x"));
    }

    @Test public void namesAreCleanedCutAndNeverEmpty() {
        assertEquals("Salon 1", ProfileStore.cleanName("  Salon\n\t1 ", "x"));
        assertEquals("fallback", ProfileStore.cleanName("   ", "fallback"));
        assertEquals("fallback", ProfileStore.cleanName(null, "fallback"));
        assertEquals(ProfileStore.NAME_MAX, ProfileStore.cleanName(new String(new char[200]).replace('\0', 'a'), "x").length());
        assertEquals("غرفة الجلوس", ProfileStore.cleanName("غرفة الجلوس", "x"));
    }

    @Test public void aBrokenFileIsSkippedAndTheOthersStillShow() throws Exception {
        ProfileStore s = store();
        s.save("Good", "2026-10-28 10:00", "x", EXPORT);
        File bad = new File(tmp.getRoot(), "profiles/zzzz.json");
        try (FileOutputStream o = new FileOutputStream(bad)) { o.write("{ broken".getBytes("UTF-8")); }
        assertEquals(1, s.list().size());
    }

    @Test public void savingSomethingThatIsNotAnExportIsRefused() throws Exception {
        ProfileStore s = store();
        try { s.save("x", "d", "t", "{\"hello\":1}"); fail(); } catch (IOException expected) { }
        assertTrue(s.list().isEmpty());
    }

    // ---------- what the TV answers ----------
    @Test public void aPreviewListsTheChanges() {
        Preview p = Preview.parse("preview: 2 change(s); nothing was changed, add --yes to apply them\n- city: Casablanca -> Rabat\n- panel: verse -> dhikr");
        assertNull(p.error); assertFalse(p.nothing);
        assertEquals(2, p.count);
        assertEquals("city: Casablanca -> Rabat", p.changes.get(0));
        assertEquals(2, p.changes.size());
    }

    @Test public void nothingToChangeAndErrorsAreRecognised() {
        assertTrue(Preview.parse("preview: nothing would change").nothing);
        assertEquals("not a ChouiTV settings file (it is not valid JSON)", Preview.parse("error: not a ChouiTV settings file (it is not valid JSON)").error);
        assertNotNull(Preview.parse("").error);
        assertNotNull(Preview.parse("Broadcast completed").error);
    }

    @Test public void anAppliedAnswerGivesTheCount() {
        assertEquals(9, Preview.parse("applied: 9 change(s); the settings from before are kept: saver.sh import --undo --yes puts them back").count);
    }
}

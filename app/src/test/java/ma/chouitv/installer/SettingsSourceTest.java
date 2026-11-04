package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which settings the set-up sends by default: the saved ones when there are some, else the city and the prayer chosen by hand. */
public class SettingsSourceTest {
    @Test public void savedSettingsComeFirstWhenThereAreSome() {
        assertEquals(SettingsSource.Mode.PROFILE, SettingsSource.defaultMode(1, false));
        assertEquals(SettingsSource.Mode.PROFILE, SettingsSource.defaultMode(5, false));
    }

    @Test public void withoutSavedSettingsTheCityIsChosenByHand() {
        assertEquals(SettingsSource.Mode.MANUAL, SettingsSource.defaultMode(0, false));
    }

    @Test public void aFileAlreadyChosenStaysTheSource() {
        assertEquals(SettingsSource.Mode.FILE, SettingsSource.defaultMode(3, true));
        assertEquals(SettingsSource.Mode.FILE, SettingsSource.defaultMode(0, true));
    }

    @Test public void onlyTheHandChoiceShowsTheFields() {
        assertTrue(SettingsSource.showsFields(SettingsSource.Mode.MANUAL));
        assertFalse(SettingsSource.showsFields(SettingsSource.Mode.PROFILE));
        assertFalse(SettingsSource.showsFields(SettingsSource.Mode.FILE));
    }

    @Test public void theSavedSettingChosenIsKeptElseTheNewest() {
        String[] ids = {"newest", "older", "oldest"};
        assertEquals("older", SettingsSource.pickProfile(ids, "older"));
        assertEquals("newest", SettingsSource.pickProfile(ids, "gone"));
        assertEquals("newest", SettingsSource.pickProfile(ids, null));
        assertEquals(null, SettingsSource.pickProfile(new String[0], "x"));
    }
}

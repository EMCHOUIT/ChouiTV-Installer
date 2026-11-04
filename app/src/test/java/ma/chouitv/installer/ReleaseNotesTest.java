package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class ReleaseNotesTest {
    @Test public void noNotesGivesNothingToShow() throws Exception {
        assertTrue(ReleaseNotes.from(new JSONObject("{\"versionName\":\"4.2\"}")).isEmpty());
        assertEquals("", ReleaseNotes.pick(ReleaseNotes.from(new JSONObject("{}")), "fr"));
    }

    @Test public void theNotesOfEachLanguageAreRead() throws Exception {
        ReleaseNotes n = ReleaseNotes.from(new JSONObject("{\"notes\":{\"fr\":\"Nouveau\",\"en\":\"New\",\"ar\":\"جديد\"}}"));
        assertEquals("Nouveau", ReleaseNotes.pick(n, "fr"));
        assertEquals("New", ReleaseNotes.pick(n, "en"));
        assertEquals("جديد", ReleaseNotes.pick(n, "ar"));
    }

    @Test public void aMissingLanguageFallsBackOnEnglishThenFrenchThenAnyText() throws Exception {
        assertEquals("New", ReleaseNotes.pick(ReleaseNotes.from(new JSONObject("{\"notes\":{\"fr\":\"Nouveau\",\"en\":\"New\"}}")), "ar"));
        assertEquals("Nouveau", ReleaseNotes.pick(ReleaseNotes.from(new JSONObject("{\"notes\":{\"fr\":\"Nouveau\"}}")), "ar"));
        assertEquals("One text", ReleaseNotes.pick(ReleaseNotes.from(new JSONObject("{\"notes\":\"One text\"}")), "fr"));
    }

    @Test public void theNotesAreCutToAReasonableLengthAndCleaned() throws Exception {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 900; i++) b.append('x');
        ReleaseNotes n = ReleaseNotes.from(new JSONObject().put("notes", new JSONObject().put("en", "  " + b + "  ")));
        String s = ReleaseNotes.pick(n, "en");
        assertTrue(s.length() <= ReleaseNotes.MAX);
        assertTrue(s.endsWith("…"));
        assertEquals("Hi", ReleaseNotes.pick(ReleaseNotes.from(new JSONObject("{\"notes\":{\"en\":\" Hi \"}}")), "en"));
    }

    @Test public void thingsThatAreNotTextAreIgnored() throws Exception {
        assertTrue(ReleaseNotes.from(new JSONObject("{\"notes\":42}")).isEmpty());
        assertTrue(ReleaseNotes.from(new JSONObject("{\"notes\":{\"en\":[1,2]}}")).isEmpty());
    }
}

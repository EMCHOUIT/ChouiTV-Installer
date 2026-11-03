package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateCheckTest {
    @Test public void aNewerVersionIsDetectedWhateverItsShape() {
        assertTrue(UpdateCheck.isNewer("1.8.2", "v1.9"));
        assertTrue(UpdateCheck.isNewer("1.8.2", "1.8.3"));
        assertTrue(UpdateCheck.isNewer("1.9", "v1.10"));    // 10 is more than 9: numbers, not text
        assertTrue(UpdateCheck.isNewer("1.8", "1.8.1"));
        assertTrue(UpdateCheck.isNewer("1.9", "2.0"));
    }

    @Test public void theSameOrAnOlderVersionIsNotAnUpdate() {
        assertFalse(UpdateCheck.isNewer("1.9", "v1.9"));
        assertFalse(UpdateCheck.isNewer("1.9", "1.9.0"));
        assertFalse(UpdateCheck.isNewer("1.9", "1.8.2"));
        assertFalse(UpdateCheck.isNewer("1.10", "1.9"));
    }

    @Test public void anUnreadableVersionIsNeverAnUpdate() {
        assertFalse(UpdateCheck.isNewer("1.9", ""));
        assertFalse(UpdateCheck.isNewer("1.9", null));
        assertFalse(UpdateCheck.isNewer("1.9", "latest"));
        assertFalse(UpdateCheck.isNewer("", "1.9"));
    }

    @Test public void theTagIsReadFromTheReleaseAnswer() {
        assertEquals("v1.9", UpdateCheck.tagOf("{\"url\":\"x\",\"tag_name\":\"v1.9\",\"name\":\"ChouiTV Companion 1.9\"}"));
        assertEquals("1.8.2", UpdateCheck.tagOf("{ \"tag_name\" : \"1.8.2\" }"));
        assertNull(UpdateCheck.tagOf("{\"message\":\"Not Found\"}"));
        assertNull(UpdateCheck.tagOf(""));
        assertNull(UpdateCheck.tagOf(null));
    }

    @Test public void theLanguageIsAskedOnlyTheFirstTime() {
        assertTrue(Nav.asksLanguage(false));
        assertFalse(Nav.asksLanguage(true));
        assertEquals(0, Nav.firstScreen(false, false));    // first use: the language
        assertEquals(18, Nav.firstScreen(true, false));    // later, no TV yet: straight to how to work, no language
        assertEquals(12, Nav.firstScreen(true, true));     // a known TV: «Ma télé»
    }
}

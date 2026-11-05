package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The comfort settings of the remote: the speed of a held key, the size of the words. Pure, so what is stored is never trusted blindly. */
public class ComfortTest {
    @Test public void aSavedLevelIsReadOrElseTheUsualOne() {
        assertEquals(1, Comfort.level(null, 1, 3));
        assertEquals(1, Comfort.level("", 1, 3));
        assertEquals(1, Comfort.level("abc", 1, 3));
        assertEquals(2, Comfort.level("2", 1, 3));
        assertEquals(1, Comfort.level("7", 1, 3));     // out of range: the usual one
        assertEquals(1, Comfort.level("-1", 1, 3));
    }

    @Test public void aLevelCyclesAndComesBackToTheFirst() {
        assertEquals(1, Comfort.next(0, 3));
        assertEquals(2, Comfort.next(1, 3));
        assertEquals(0, Comfort.next(2, 3));
    }

    @Test public void aHeldKeyRepeatsFasterOrSlowerButNeverFasterThanTheTvFollows() {
        assertEquals(200, Comfort.repeatMs(0));
        assertEquals(120, Comfort.repeatMs(1));
        assertEquals(80, Comfort.repeatMs(2));
        assertEquals(120, Comfort.repeatMs(9));      // unknown: the usual one
        assertEquals(400, Comfort.FIRST_REPEAT_MS);
    }

    @Test public void theWordsOfTheKeysGrowInThreeSteps() {
        assertEquals(1.0f, Comfort.textScale(0), 0.001f);
        assertEquals(1.25f, Comfort.textScale(1), 0.001f);
        assertEquals(1.5f, Comfort.textScale(2), 0.001f);
        assertEquals(1.0f, Comfort.textScale(9), 0.001f);
        assertFalse(Comfort.textOnly(0));
        assertTrue(Comfort.textOnly(1));    // big words leave no room for an icon beside them
        assertTrue(Comfort.textOnly(2));
    }

    @Test public void theDefaultsAreTheRemoteAsItHasAlwaysBeen() {
        assertEquals(1, Comfort.DEFAULT_REPEAT);
        assertEquals(0, Comfort.DEFAULT_TEXT);
    }
}

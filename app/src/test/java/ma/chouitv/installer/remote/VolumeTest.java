package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumeTest {
    @Test public void aLevelIsSetInOneCommandAndKeptBetweenZeroAndOneHundred() {
        assertEquals("cmd media_session volume --stream 3 --set 35", Volume.set(35));
        assertEquals("cmd media_session volume --stream 3 --set 0", Volume.set(-8));
        assertEquals("cmd media_session volume --stream 3 --set 100", Volume.set(250));
    }

    @Test public void aStepIsOneAdjustment() {
        assertEquals("cmd media_session volume --stream 3 --adj raise", Volume.adj(true));
        assertEquals("cmd media_session volume --stream 3 --adj lower", Volume.adj(false));
    }

    @Test public void theLevelIsReadFromTheSettingOrFromTheDump() {
        assertEquals(19, Volume.parseLevel("19\r\n"));
        assertEquals(7, Volume.parseLevel("   streamVolume:7\n"));
        assertEquals(0, Volume.parseLevel("0"));
        assertEquals(-1, Volume.parseLevel("null"));
        assertEquals(-1, Volume.parseLevel(""));
        assertEquals(-1, Volume.parseLevel(null));
        assertEquals(-1, Volume.parseLevel("/system/bin/sh: settings: inaccessible or not found"));
        assertEquals(-1, Volume.parseLevel("250"));   // not a level of a TV
    }

    @Test public void theMaximumLevelCapsWhatIsSet() {
        assertEquals(35, Volume.capped(35, 0));     // no limit
        assertEquals(70, Volume.capped(95, 70));
        assertEquals(40, Volume.capped(40, 70));
        assertEquals(0, Volume.capped(-5, 70));
        assertTrue(Volume.overLimit(71, 70));
        assertFalse(Volume.overLimit(70, 70));
        assertFalse(Volume.overLimit(100, 0));
    }

    @Test public void aTvThatRefusesTheCommandIsRecognisedFromItsAnswer() {
        assertTrue(Volume.failed("/system/bin/sh: cmd: inaccessible or not found"));
        assertTrue(Volume.failed("No shell command implementation."));
        assertTrue(Volume.failed("Unknown command: volume"));
        assertTrue(Volume.failed("Exception occurred while executing 'volume':"));
        assertFalse(Volume.failed(""));
        assertFalse(Volume.failed(null));
    }

    @Test public void withoutTheDirectCommandTheKeysAreSentInOneBatch() {
        assertEquals("input keyevent 24 24 24", Volume.keys(10, 13));
        assertEquals("input keyevent 25 25", Volume.keys(40, 38));
        assertNull(Volume.keys(20, 20));
        assertNull(Volume.keys(-1, 20));   // the level is not known: nothing to count from
        // at most 30 keys at once, so that one batch does not take half a minute
        assertEquals(30, Volume.keys(0, 100).split(" ").length - 2);
    }

    @Test public void muteRemembersTheLevelAndGivesItBack() {
        assertEquals(0, Volume.muteTarget(35, 0));       // sound on: go silent
        assertEquals(35, Volume.muteTarget(0, 35));      // silent: back to where it was
        assertEquals(Volume.DEFAULT_RETURN, Volume.muteTarget(0, 0));   // nothing remembered: a gentle level, not the maximum
    }
}

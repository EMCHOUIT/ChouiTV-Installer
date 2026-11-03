package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VolumeTest {
    @Test public void aLevelIsSetInOneCommandAndKeptBetweenZeroAndTheMaximumOfTheTv() {
        assertEquals("cmd media_session volume --stream 3 --show --set 35", Volume.set(35, 100));
        assertEquals("cmd media_session volume --stream 3 --show --set 0", Volume.set(-8, 100));
        assertEquals("cmd media_session volume --stream 3 --show --set 100", Volume.set(250, 100));
        assertEquals("cmd media_session volume --stream 3 --show --set 15", Volume.set(40, 15));   // a TV with 15 steps
    }

    @Test public void aStepIsOneAdjustment() {
        assertEquals("cmd media_session volume --stream 3 --show --adj raise", Volume.adj(true));
        assertEquals("cmd media_session volume --stream 3 --show --adj lower", Volume.adj(false));
    }

    @Test public void theMaximumIsReadFromTheTvAndTheDefaultScaleIsUsedOtherwise() {
        assertEquals(100, Volume.parseMax("   Max: 100\n"));
        assertEquals(15, Volume.parseMax("   Max: 15"));
        assertEquals(30, Volume.parseMax("Min: 0\n   Max: 30\n"));
        assertEquals(Volume.DEFAULT_MAX, Volume.parseMax(""));
        assertEquals(Volume.DEFAULT_MAX, Volume.parseMax(null));
        assertEquals(Volume.DEFAULT_MAX, Volume.parseMax("Max: 1"));      // not a scale
        assertEquals(Volume.DEFAULT_MAX, Volume.parseMax("Max: 999"));    // not a scale
    }

    @Test public void theLevelIsReadFromTheSettingOrFromTheDump() {
        assertEquals(19, Volume.parseLevel("19\r\n", 100));
        assertEquals(7, Volume.parseLevel("   streamVolume:7\n", 100));
        assertEquals(0, Volume.parseLevel("0", 15));
        assertEquals(-1, Volume.parseLevel("null", 100));
        assertEquals(-1, Volume.parseLevel("", 100));
        assertEquals(-1, Volume.parseLevel(null, 100));
        assertEquals(-1, Volume.parseLevel("/system/bin/sh: settings: inaccessible or not found", 100));
        assertEquals(-1, Volume.parseLevel("40", 15));   // above the maximum of this TV: not a level
    }

    @Test public void theFamilyLimitIsAPercentageOfTheScaleOfEachTv() {
        assertEquals(0, Volume.limitLevel(0, 100));
        assertEquals(30, Volume.limitLevel(30, 100));
        assertEquals(5, Volume.limitLevel(30, 15));      // 15 steps: 30 % is step 5
        assertEquals(9, Volume.limitLevel(30, 30));
        assertEquals(1, Volume.limitLevel(1, 15));       // never zero: that would mute the TV
        assertEquals(14, Volume.limitLevel(90, 15));
    }

    @Test public void theLimitAndTheMaximumCapWhatIsSet() {
        assertEquals(35, Volume.capped(35, 0, 100));
        assertEquals(70, Volume.capped(95, 70, 100));
        assertEquals(40, Volume.capped(40, 70, 100));
        assertEquals(0, Volume.capped(-5, 70, 100));
        assertEquals(15, Volume.capped(40, 0, 15));
        assertEquals(5, Volume.capped(12, 5, 15));
        assertTrue(Volume.overLimit(71, 70));
        assertFalse(Volume.overLimit(70, 70));
        assertFalse(Volume.overLimit(100, 0));
    }

    @Test public void aTvThatRefusesTheCommandIsRecognisedFromItsAnswer() {
        assertTrue(Volume.failed("/system/bin/sh: cmd: inaccessible or not found"));
        assertTrue(Volume.failed("No shell command implementation."));
        assertTrue(Volume.failed("Unknown command: volume"));
        assertTrue(Volume.failed("Exception occurred while executing 'volume':"));
        assertTrue(Volume.failed("usage: cmd media_session [subcommand] [options]"));
        assertTrue(Volume.failed("Error: Permission denied"));
        assertFalse(Volume.failed(""));
        assertFalse(Volume.failed(null));
        assertFalse(Volume.failed("Volume set"));
    }

    @Test public void aStepThatChangedNothingIsDetectedUnlessTheLevelIsAtAnEnd() {
        assertTrue(Volume.moved(20, 21, true, 100));
        assertTrue(Volume.moved(20, 19, false, 100));
        assertFalse(Volume.moved(20, 20, true, 100));     // the TV did nothing: use the keys
        assertTrue(Volume.moved(100, 100, true, 100));    // already at the top: nothing to move
        assertTrue(Volume.moved(0, 0, false, 100));
        assertFalse(Volume.moved(-1, 20, true, 100));     // the level was not known: no verdict
        assertFalse(Volume.moved(20, -1, true, 100));
    }

    @Test public void withoutTheDirectCommandTheKeysAreSentInOneBatch() {
        assertEquals("input keyevent 24 24 24", Volume.keys(10, 13));
        assertEquals("input keyevent 25 25", Volume.keys(40, 38));
        assertNull(Volume.keys(20, 20));
        assertNull(Volume.keys(-1, 20));
        assertEquals(30, Volume.keys(0, 100).split(" ").length - 2);
    }

    @Test public void muteRemembersTheLevelAndGivesItBack() {
        assertEquals(0, Volume.muteTarget(35, 0, 100));
        assertEquals(35, Volume.muteTarget(0, 35, 100));
        assertEquals(20, Volume.muteTarget(0, 0, 100));   // nothing remembered: a gentle level (a fifth of the scale)
        assertEquals(3, Volume.muteTarget(0, 0, 15));
    }

    @Test public void theTvTellsWhetherItGaveItsMaximum() {
        assertTrue(Volume.hasMax("   Max: 15"));
        assertTrue(Volume.hasMax("Min: 0\n   Max: 100\n"));
        assertFalse(Volume.hasMax(""));
        assertFalse(Volume.hasMax(null));
        assertFalse(Volume.hasMax("Max: 999"));
    }

    @Test public void aManualScaleAndAStepMustBeSensible() {
        assertTrue(Volume.validScale(15));
        assertTrue(Volume.validScale(100));
        assertTrue(Volume.validScale(250));
        assertFalse(Volume.validScale(1));
        assertFalse(Volume.validScale(0));
        assertFalse(Volume.validScale(251));
        assertTrue(Volume.validStep(1, 15));
        assertTrue(Volume.validStep(5, 100));
        assertFalse(Volume.validStep(0, 100));
        assertFalse(Volume.validStep(-2, 100));
        assertFalse(Volume.validStep(16, 15));   // a step larger than the scale
    }

    @Test public void aKeyMovesTheLevelByTheChosenStepAndStopsAtTheLimits() {
        assertEquals(25, Volume.stepTarget(20, 5, true, 0, 100));
        assertEquals(15, Volume.stepTarget(20, 5, false, 0, 100));
        assertEquals(0, Volume.stepTarget(3, 5, false, 0, 100));
        assertEquals(100, Volume.stepTarget(98, 5, true, 0, 100));
        assertEquals(70, Volume.stepTarget(68, 5, true, 70, 100));   // the family limit
        assertEquals(15, Volume.stepTarget(14, 2, true, 0, 15));     // a TV with 15 levels
        assertEquals(21, Volume.stepTarget(20, 1, true, 0, 100));
    }

    @Test public void withoutAKnownLevelAStepIsSeveralAdjustmentsInOneCommand() {
        assertEquals("cmd media_session volume --stream 3 --show --adj raise; cmd media_session volume --stream 3 --show --adj raise", Volume.batchAdj(2, true));
        assertEquals("cmd media_session volume --stream 3 --show --adj lower", Volume.batchAdj(1, false));
        assertEquals(30, Volume.batchAdj(99, true).split("; ").length);   // never more than 30
    }
}

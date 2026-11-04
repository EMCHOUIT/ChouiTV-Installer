package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ma.chouitv.installer.remote.RemoteLayout.Block;
import ma.chouitv.installer.remote.RemoteLayout.Edit;
import ma.chouitv.installer.remote.RemoteLayout.Kind;
import ma.chouitv.installer.remote.RemoteLayout.Template;
import ma.chouitv.installer.remote.RemoteLayout.Why;

/** The layout of the remote: a 12-column grid, ready-made templates, and the guards that keep the remote usable whatever the family does to it. */
public class RemoteLayoutTest {
    private static final int TALL = 40;

    @Test public void everyTemplateIsValidAndKeepsWhatMustStay() {
        for (Template t : Template.values()) {
            RemoteLayout l = RemoteLayout.template(t);
            assertNull(t.name(), l.problem(TALL));
            assertTrue(t.name() + " pad", l.has(Kind.PAD));
            assertTrue(t.name() + " back", l.has(Kind.BACK));
            int volumes = 0;
            for (Kind k : Kind.values()) if (RemoteLayout.isVolume(k) && l.has(k)) volumes++;
            assertEquals(t.name() + " volume", 1, volumes);
        }
    }

    @Test public void theClassicTemplateIsTheCurrentRemote() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(24, l.rows());
        assertTrue(l.has(Kind.VOL_BAR));
        assertEquals(12, l.get(Kind.VOL_BAR).w);
        assertEquals(12, l.get(Kind.PAD).w);
        assertEquals(6, l.get(Kind.PAD).row);
    }

    @Test public void theLeftHandedTemplateIsTheMirrorOfTheRightHandedOne() {
        RemoteLayout r = RemoteLayout.template(Template.RIGHT);
        RemoteLayout l = RemoteLayout.template(Template.LEFT);
        for (Block b : r.blocks()) {
            Block m = l.get(b.kind);
            assertNotNull(b.kind.name(), m);
            assertEquals(b.kind.name(), RemoteLayout.COLS - b.col - b.w, m.col);
            assertEquals(b.row, m.row);
            assertEquals(b.w, m.w);
        }
        assertTrue(r.get(Kind.VOL_ROCKER).col > RemoteLayout.COLS / 2);
        assertTrue(l.get(Kind.VOL_ROCKER).col < RemoteLayout.COLS / 2);
    }

    @Test public void theCompactTemplateFitsAShortScreen() {
        assertTrue(RemoteLayout.template(Template.COMPACT).rows() <= 16);
    }

    @Test public void theRowsAScreenOffersAreWhatItsHeightHolds() {
        assertEquals(23, RemoteLayout.maxRows(600, 26));
        assertEquals(0, RemoteLayout.maxRows(10, 26));
    }

    // ---- moving
    @Test public void aKeyMovesToAFreePlace() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        Edit e = l.move(Kind.LISTEN, 0, 15, TALL);
        assertTrue(e.ok());
        assertEquals(15, e.layout.get(Kind.LISTEN).row);
        assertEquals(0, l.get(Kind.LISTEN).row);     // the old layout is untouched
    }

    @Test public void aKeyDoesNotLandOnAnother() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        Edit e = l.move(Kind.LISTEN, 4, 0, TALL);
        assertFalse(e.ok());
        assertEquals(Why.OVERLAP, e.why);
    }

    @Test public void aKeyDoesNotLeaveTheGrid() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertEquals(Why.OFF_GRID, l.move(Kind.LISTEN, 10, 15, TALL).why);   // 3 wide at column 10 passes column 12
        assertEquals(Why.OFF_GRID, l.move(Kind.LISTEN, -1, 15, TALL).why);
        assertEquals(Why.OFF_GRID, l.move(Kind.LISTEN, 0, -1, TALL).why);
    }

    @Test public void aKeyDoesNotGoBelowWhatTheScreenHolds() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertEquals(Why.TOO_TALL, l.move(Kind.LISTEN, 0, 20, 20).why);
        assertTrue(l.move(Kind.LISTEN, 0, 17, 20).ok());   // 17 + 3 = 20 rows exactly
    }

    @Test public void theKeysThatMustStayMayStillMove() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertTrue(l.move(Kind.BACK, 0, 15, TALL).ok());
    }

    // ---- adding and removing
    @Test public void aKeyIsAddedInTheFirstFreePlace() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        Edit e = l.add(Kind.YOUTUBE, TALL);
        assertTrue(e.ok());
        assertTrue(e.layout.has(Kind.YOUTUBE));
        assertNull(e.layout.problem(TALL));
    }

    @Test public void aKeyAlreadyThereIsNotAddedTwice() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertEquals(Why.ALREADY, l.add(Kind.LISTEN, TALL).why);
    }

    @Test public void nothingIsAddedWhenThereIsNoRoom() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(Why.NO_ROOM, l.add(Kind.YOUTUBE, l.rows()).why);
    }

    @Test public void theKeysThatMustStayCannotBeRemoved() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(Why.REQUIRED, l.remove(Kind.PAD).why);
        assertEquals(Why.REQUIRED, l.remove(Kind.BACK).why);
        assertEquals(Why.REQUIRED, l.remove(Kind.VOL_BAR).why);
    }

    @Test public void theOtherKeysCanBeRemoved() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        Edit e = l.remove(Kind.HOME);
        assertTrue(e.ok());
        assertFalse(e.layout.has(Kind.HOME));
        assertTrue(e.layout.has(Kind.BACK));
    }

    // ---- sizes and the volume
    @Test public void aKeyOnlyTakesTheSizesPlannedForIt() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertEquals(Why.BAD_SIZE, l.resize(Kind.LISTEN, 5, 5, TALL).why);
        assertTrue(l.resize(Kind.WRITE, 8, 3, TALL).why != Why.BAD_SIZE);
        assertEquals(Why.BAD_SIZE, l.resize(Kind.PAD, 10, 10, TALL).why);
    }

    @Test public void aKeyDoesNotGrowOverItsNeighbour() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(Why.OVERLAP, l.resize(Kind.LISTEN, 8, 3, TALL).why);
    }

    @Test public void thePadCanBeBigOrCompact() {
        RemoteLayout l = RemoteLayout.template(Template.COMPACT);
        assertEquals(9, l.get(Kind.PAD).w);
        assertTrue(l.resize(Kind.PAD, 12, 12, TALL).why != Why.BAD_SIZE);
    }

    @Test public void thereIsAlwaysExactlyOneFormOfTheVolume() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(Why.ALREADY, l.add(Kind.VOL_ROCKER, TALL).why);
        Edit e = l.swapVolume(Kind.VOL_PILL, TALL);
        assertTrue(e.ok());
        assertTrue(e.layout.has(Kind.VOL_PILL));
        assertFalse(e.layout.has(Kind.VOL_BAR));
        assertNull(e.layout.problem(TALL));
    }

    @Test public void swappingTheVolumeToWhatDoesNotFitIsRefused() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertEquals(Why.NO_ROOM, l.swapVolume(Kind.VOL_ROCKER, l.rows()).why);
    }

    @Test public void swappingToANonVolumeKeyIsRefused() {
        assertEquals(Why.VOLUME_FORM, RemoteLayout.template(Template.CLASSIC).swapVolume(Kind.HOME, TALL).why);
    }

    // ---- saving
    @Test public void aLayoutSurvivesBeingSavedAndRead() {
        for (Template t : Template.values()) {
            RemoteLayout l = RemoteLayout.template(t);
            RemoteLayout back = RemoteLayout.decode(l.encode());
            assertNotNull(t.name(), back);
            assertEquals(t.name(), l.encode(), back.encode());
        }
    }

    @Test public void whatIsSavedIsNeverTrustedBlindly() {
        assertNull(RemoteLayout.decode(null));
        assertNull(RemoteLayout.decode(""));
        assertNull(RemoteLayout.decode("garbage"));
        assertNull(RemoteLayout.decode("v1;PAD:0,0,12,12"));                       // no Back, no volume
        assertNull(RemoteLayout.decode("v1;PAD:0,0,12,12;BACK:0,0,4,3;VOL_BAR:0,12,12,3"));   // two keys on the same place
        assertNull(RemoteLayout.decode("v1;PAD:0,0,12,12;BACK:0,12,4,3;VOL_BAR:0,15,12,3;NOPE:0,18,4,3"));   // a key that does not exist
        assertNull(RemoteLayout.decode("v1;PAD:0,0,12,12;BACK:0,12,4,3;VOL_BAR:0,15,12,3;HOME:0,18,5,5"));   // a size not planned
        assertNull(RemoteLayout.decode("v2;PAD:0,0,12,12;BACK:0,12,4,3;VOL_BAR:0,15,12,3"));   // a version we do not know
    }

    @Test public void whenWhatIsSavedIsUnusableTheClassicRemoteComesBack() {
        assertEquals(RemoteLayout.template(Template.CLASSIC).encode(), RemoteLayout.orClassic(null).encode());
        assertEquals(RemoteLayout.template(Template.CLASSIC).encode(), RemoteLayout.orClassic("garbage").encode());
        String ok = RemoteLayout.template(Template.ELDER).encode();
        assertEquals(ok, RemoteLayout.orClassic(ok).encode());
    }

    @Test public void aLayoutThatDoesNotFitThisScreenIsReported() {
        RemoteLayout l = RemoteLayout.template(Template.CLASSIC);
        assertNull(l.problem(24));
        assertEquals("tall", l.problem(23));
    }
}

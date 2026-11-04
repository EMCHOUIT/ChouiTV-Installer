package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/** The end of a set-up says what was done on which TV, and what can be undone. */
public class SetupSummaryTest {
    @Test public void theInstallIsAlwaysTold() {
        List<SetupSummary.Line> l = SetupSummary.lines(null, 0);
        assertEquals(1, l.size());
        assertEquals(SetupSummary.Kind.INSTALLED, l.get(0).kind);
    }

    @Test public void theSettingsAndTheAppsTurnedOffAreToldWithTheirDetail() {
        List<SetupSummary.Line> l = SetupSummary.lines("Salon du Maroc", 21);
        assertEquals(3, l.size());
        assertEquals(SetupSummary.Kind.SETTINGS, l.get(1).kind);
        assertEquals("Salon du Maroc", l.get(1).arg);
        assertEquals(SetupSummary.Kind.APPS_OFF, l.get(2).kind);
        assertEquals("21", l.get(2).arg);
    }

    @Test public void aBlankSettingsSourceOrNoAppTurnedOffIsNotTold() {
        assertEquals(1, SetupSummary.lines("  ", 0).size());
        assertEquals(2, SetupSummary.lines("x", 0).size());
        assertEquals(2, SetupSummary.lines(null, 3).size());
    }

    @Test public void onlyWhatWasDoneCanBeUndone() {
        assertTrue(SetupSummary.canUndoSettings("Salon"));
        assertFalse(SetupSummary.canUndoSettings(null));
        assertFalse(SetupSummary.canUndoSettings(""));
        assertTrue(SetupSummary.canUndoApps(1));
        assertFalse(SetupSummary.canUndoApps(0));
    }
}

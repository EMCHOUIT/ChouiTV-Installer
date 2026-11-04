package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The preview before a set-up: which steps will run, where the guided path starts, how long it takes. */
public class EquipPlanTest {
    @Test public void withTheCleanUpThePathStartsAtItAndTakesLonger() {
        assertEquals(0, EquipPlan.startStep(true));
        assertEquals(8, EquipPlan.minutes(true));
    }

    @Test public void withoutTheCleanUpThePathStartsAtTheInstall() {
        assertEquals(1, EquipPlan.startStep(false));
        assertEquals(4, EquipPlan.minutes(false));
    }

    @Test public void theCleanUpIsAskedAgainAtTheEndOnlyWhenItWasLeftOut() {
        assertFalse(EquipPlan.offerCleanUpAtTheEnd(true));
        assertTrue(EquipPlan.offerCleanUpAtTheEnd(false));
    }

    @Test public void theNavigationOfThePreviewGoesBackToTheRemote() {
        assertEquals(25, Nav.backTarget(31, false, false, false));
        assertTrue(Nav.showsBack(31, false, false, false));
        assertFalse(Nav.showsTabs(31));
    }
}

package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Where «back» goes from each screen, whether the visible Back control shows, and the double press that leaves the app. */
public class NavTest {
    @Test public void theMappingIsTheOneTheSystemBackAlwaysHad() {
        assertEquals(12, Nav.backTarget(18, false, false, false));
        assertEquals(27, Nav.backTarget(12, false, false, false));
        assertEquals(12, Nav.backTarget(19, false, false, false));
        assertEquals(19, Nav.backTarget(20, false, false, false));
        assertEquals(19, Nav.backTarget(21, false, false, false));
        assertEquals(18, Nav.backTarget(22, false, false, false));
        assertEquals(18, Nav.backTarget(24, false, false, false));
        assertEquals(17, Nav.backTarget(15, false, false, false));
        assertEquals("the search for a TV, opened from the remote, goes back to the remote", 25, Nav.backTarget(2, false, false, false));
        assertEquals("the three steps, opened from the remote, go back to the remote", 25, Nav.backTarget(1, false, false, false));
        assertEquals("a guided path keeps its own steps", 1, Nav.backTarget(2, false, true, false));
    }

    @Test public void aGuidedPathGoesBackToItsOwnListInsteadOfTheHub() {
        assertEquals(19, Nav.backTarget(1, false, true, false));
        for (int s : new int[] {11, 13, 14, 17}) {
            assertEquals(12, Nav.backTarget(s, false, false, false));
            assertEquals(19, Nav.backTarget(s, false, true, false));
        }
        for (int s : new int[] {8, 9, 10}) {
            assertEquals(12, Nav.backTarget(s, false, false, false));
            assertEquals(19, Nav.backTarget(s, false, true, false));
            assertEquals("a cleanup started from the end of an install goes back to that end", 5, Nav.backTarget(s, false, true, true));
        }
    }

    @Test public void nothingGoesBackWhileSomethingRuns() {
        assertEquals(Nav.BLOCKED, Nav.backTarget(2, true, false, false));
        assertEquals(Nav.BLOCKED, Nav.backTarget(12, true, false, false));
        assertEquals("the progress screen", Nav.BLOCKED, Nav.backTarget(23, false, false, false));
    }

    @Test public void theEndOfTheLineLeavesTheAppOnlyAfterTheDoublePress() {
        assertEquals(Nav.EXIT, Nav.backTarget(0, false, false, false));
        assertEquals(Nav.EXIT, Nav.backTarget(3, false, false, false));
        assertEquals(Nav.EXIT, Nav.backTarget(4, false, false, false));
        assertEquals(Nav.EXIT, Nav.backTarget(5, false, false, false));
    }

    @Test public void theVisibleControlIsOnEveryScreenThatCanGoBackSafely() {
        assertFalse("language screen", Nav.showsBack(0, false, false, false));
        assertFalse("progress", Nav.showsBack(23, false, false, false));
        assertFalse("install", Nav.showsBack(3, false, false, false));
        assertFalse("cleanup running", Nav.showsBack(9, false, false, false));
        assertFalse("personalise", Nav.showsBack(4, false, false, false));
        assertFalse("done", Nav.showsBack(5, false, false, false));
        for (int s : new int[] {1, 2, 8, 10, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 24}) assertTrue("step " + s, Nav.showsBack(s, false, false, false));
    }

    @Test public void theSecondPressCountsOnlyWithinTwoSeconds() {
        assertFalse("first press", Nav.exitNow(0, 5000));
        assertTrue(Nav.exitNow(5000, 6999));
        assertTrue(Nav.exitNow(5000, 7000));
        assertFalse(Nav.exitNow(5000, 7001));
    }

    @Test public void aTaskGoesStraightToTheKnownTvOnlyWhenNothingGuidesTheUser() {
        assertTrue(Nav.startsDirect(false, true));    // free mode, a TV was used before: no «Préparez la télé», no scan
        assertFalse(Nav.startsDirect(false, false));  // first time: the usual path
        assertFalse(Nav.startsDirect(true, true));    // a guided path keeps its own steps
        assertFalse(Nav.startsDirect(true, false));
    }

    @Test public void theJournalAndTheFixScreenGoBackToThePageOfTheTv() {
        assertEquals(12, Nav.backTarget(29, false, false, false));
        assertEquals(12, Nav.backTarget(30, false, false, false));
        assertTrue(Nav.showsBack(29, false, false, false));
        assertTrue(Nav.showsBack(30, false, false, false));
    }

    @Test public void everyScreenOfTheAppCanBeVisitedWithoutAnyTv() {
        for (int s : new int[] {0, 1, 2, 25, 26, 27, 28}) assertTrue("step " + s, Nav.opensWithoutTv(s));
        for (int s : new int[] {3, 8, 11, 12, 14, 17, 29, 30}) assertFalse("step " + s + " works on a TV", Nav.opensWithoutTv(s));
    }

    @Test public void theRemoteIsTheHomeAndNothingHasATabBar() {
        assertEquals(25, Nav.firstScreen(true, true));     // a TV is known: the app opens on its remote
        assertEquals(25, Nav.firstScreen(true, false));    // no TV yet: still the remote (a demo), never a waiting or preparation screen
        assertEquals(0, Nav.firstScreen(false, false));    // the very first use: the language
        assertEquals(Nav.EXIT, Nav.backTarget(25, false, false, false));
        assertEquals(25, Nav.backTarget(27, false, false, false));    // «My TVs» goes back to the remote
        assertEquals(27, Nav.backTarget(12, false, false, false));    // a TV's page goes back to «My TVs»
        assertEquals(25, Nav.backTarget(26, false, false, false));    // «Options» goes back to the remote
        assertEquals(12, Nav.backTarget(14, false, false, false));    // a task goes back to the TV's page
        assertFalse(Nav.showsBack(25, false, false, false));
        assertFalse("the three tabs are roots: no visible Back", Nav.showsBack(27, false, false, false));
        assertFalse(Nav.showsBack(26, false, false, false));
        assertTrue(Nav.showsBack(12, false, false, false));
    }

    @Test public void theThreeTabsAreTheRemoteTheTvsAndMore() {
        assertEquals(0, Nav.tabOf(25));
        assertEquals(1, Nav.tabOf(27));
        assertEquals(2, Nav.tabOf(26));
        assertEquals("a TV's page, its journal and its fix belong to «My TVs»", 1, Nav.tabOf(12));
        assertEquals(1, Nav.tabOf(29));
        assertEquals(1, Nav.tabOf(30));
        assertEquals(25, Nav.tabRoot(0));
        assertEquals(27, Nav.tabRoot(1));
        assertEquals(26, Nav.tabRoot(2));
    }

    @Test public void theBarIsOnlyWhereTheFamilyLivesNotInASetUpOrACleanUp() {
        for (int s : new int[] {25, 26, 27, 12, 29, 30}) assertTrue("step " + s, Nav.showsTabs(s));
        for (int s : new int[] {0, 1, 2, 3, 4, 5, 8, 9, 10, 11, 13, 14, 15, 17, 23, 24}) assertFalse("step " + s, Nav.showsTabs(s));
    }

    @Test public void theLayoutScreensLiveUnderMoreAndCanBeVisitedWithNoTv() {
        assertEquals("the choice of layout goes back to «More»", 26, Nav.backTarget(32, false, false, false));
        assertEquals("the editor goes back to the choice", 32, Nav.backTarget(33, false, false, false));
        assertEquals(2, Nav.tabOf(32));
        assertEquals("the editor takes the whole screen", -1, Nav.tabOf(33));
        assertTrue(Nav.opensWithoutTv(32));
        assertTrue(Nav.opensWithoutTv(33));
        assertTrue(Nav.showsBack(32, false, false, false));
        assertTrue(Nav.showsBack(33, false, false, false));
    }

    @Test public void theComfortScreenLivesUnderMoreAndCanBeVisitedWithNoTv() {
        assertEquals(26, Nav.backTarget(34, false, false, false));
        assertEquals(2, Nav.tabOf(34));
        assertTrue(Nav.opensWithoutTv(34));
        assertTrue(Nav.showsBack(34, false, false, false));
    }
}

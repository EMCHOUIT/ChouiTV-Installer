package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Which way a key goes: the fast link kept open to the TV when it works, «input keyevent» (slow but sure) when it does not, and when to try the fast link again. */
public class KeyRouteTest {
    @Test public void withoutALinkTheSlowWaySendsTheKeyAndTheLinkIsTriedAtOnce() {
        KeyRoute r = new KeyRoute();
        assertEquals(KeyRoute.Via.SHELL, r.via());
        assertTrue("a first try is allowed", r.mayTry(1000));
    }

    @Test public void aWorkingLinkTakesTheKeys() {
        KeyRoute r = new KeyRoute();
        r.up();
        assertEquals(KeyRoute.Via.CHANNEL, r.via());
        assertFalse("no second link while one works", r.mayTry(1000));
    }

    @Test public void aFailureSendsKeysTheSlowWayAndWaitsBeforeTryingAgain() {
        KeyRoute r = new KeyRoute();
        r.up();
        r.down(10_000);
        assertEquals(KeyRoute.Via.SHELL, r.via());
        assertFalse(r.mayTry(10_000 + 4_999));
        assertTrue(r.mayTry(10_000 + 5_000));
    }

    @Test public void theWaitGrowsWithEachFailureAndIsCapped() {
        KeyRoute r = new KeyRoute();
        long t = 0;
        r.down(t);   assertEquals(5_000, r.waitMs());
        r.down(t);   assertEquals(15_000, r.waitMs());
        r.down(t);   assertEquals(60_000, r.waitMs());
        r.down(t);   assertEquals("never more than a minute", 60_000, r.waitMs());
    }

    @Test public void aWorkingLinkForgetsTheFailures() {
        KeyRoute r = new KeyRoute();
        r.down(0); r.down(0);
        r.up();
        r.down(100_000);
        assertEquals("back to the short wait", 5_000, r.waitMs());
    }

    @Test public void comingBackToTheAppAllowsTriesAgain() {
        KeyRoute r = new KeyRoute();
        r.down(0); r.down(0);
        r.stop();
        r.resume();
        assertTrue("a fresh start: a try is allowed at once", r.mayTry(0));
        r.down(0);
        assertEquals("the failures before leaving are forgotten", 5_000, r.waitMs());
    }

    @Test public void aLinkThatWasClosedOnPurposeIsNotAWorkingLinkAnyMore() {
        KeyRoute r = new KeyRoute();
        r.up();
        assertFalse("a link works: no second one", r.mayTry(0));
        r.closed();                          // the app let go of it (another TV, the app left)
        assertEquals(KeyRoute.Via.SHELL, r.via());
        assertTrue("a new link may be tried at once: this is not a failure", r.mayTry(0));
        r.down(0);
        assertEquals("no failure was counted", 5_000, r.waitMs());
    }

    @Test public void aLinkToAnotherTvIsStale() {
        assertTrue(KeyRoute.stale(true, "192.168.1.50", "192.168.1.51"));   // the link of TV1 while the app talks to TV2
        assertTrue("the TV was lost: its link goes too", KeyRoute.stale(true, "192.168.1.50", ""));
        assertTrue(KeyRoute.stale(true, null, "192.168.1.51"));
        assertFalse(KeyRoute.stale(true, "192.168.1.51", "192.168.1.51"));
        assertFalse("no link, nothing to close", KeyRoute.stale(false, "", "192.168.1.51"));
    }

    @Test public void closingTheAppStopsTheTries() {
        KeyRoute r = new KeyRoute();
        r.stop();
        assertFalse(r.mayTry(1_000_000));
        assertEquals(KeyRoute.Via.SHELL, r.via());
    }
}

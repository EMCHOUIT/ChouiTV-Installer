package ma.chouitv.installer.guide;

import static org.junit.Assert.*;

import org.junit.Test;

import ma.chouitv.installer.guide.Outcome.Status;

public class OutcomeTest {
    @Test public void everythingDoneWithoutProblemIsASuccess() {
        assertEquals(Status.SUCCESS, Outcome.of(26, 26, false));
        assertEquals("nothing to do is not a failure", Status.SUCCESS, Outcome.of(0, 0, false));
    }

    @Test public void someDoneIsIncomplete() {
        assertEquals(Status.PARTIAL, Outcome.of(26, 24, false));
        assertEquals("all done but something went wrong on the way", Status.PARTIAL, Outcome.of(5, 5, true));
        assertEquals(Status.PARTIAL, Outcome.of(26, 1, true));
    }

    @Test public void nothingDoneIsAFailure() {
        assertEquals(Status.FAILED, Outcome.of(26, 0, false));
        assertEquals(Status.FAILED, Outcome.of(3, 0, true));
        assertEquals("a problem with nothing asked", Status.FAILED, Outcome.of(0, 0, true));
    }

    @Test public void moreDoneThanAskedStillCounts() {
        assertEquals(Status.SUCCESS, Outcome.of(2, 3, false));
    }

    @Test public void percentIsWholeStepsOverTheTotalAndStaysInRange() {
        assertEquals(0, Outcome.percent(0, 3));
        assertEquals(33, Outcome.percent(1, 3));
        assertEquals(66, Outcome.percent(2, 3));
        assertEquals(100, Outcome.percent(3, 3));
        assertEquals(100, Outcome.percent(9, 3));
        assertEquals(0, Outcome.percent(-2, 3));
        assertEquals("no steps: nothing left to wait for", 100, Outcome.percent(0, 0));
    }
}

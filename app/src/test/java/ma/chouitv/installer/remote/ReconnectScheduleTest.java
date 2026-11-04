package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** The quiet tries while the app is open and the TV does not answer: often at first, then less. */
public class ReconnectScheduleTest {
    @Test public void everyThreeSecondsForTheFirstHalfMinute() {
        assertEquals(3000, Reconnect.backgroundDelayMs(0));
        assertEquals(3000, Reconnect.backgroundDelayMs(29999));
    }

    @Test public void thenEveryTenSecondsUpToTwoMinutes() {
        assertEquals(10000, Reconnect.backgroundDelayMs(30000));
        assertEquals(10000, Reconnect.backgroundDelayMs(119999));
    }

    @Test public void thenEveryHalfMinute() {
        assertEquals(30000, Reconnect.backgroundDelayMs(120000));
        assertEquals(30000, Reconnect.backgroundDelayMs(3_600_000));
    }

    @Test public void aNegativeTimeIsTheStart() {
        assertEquals(3000, Reconnect.backgroundDelayMs(-5));
    }
}

package ma.chouitv.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The component names of the merged app, built the way the TV is told about them. */
public class FlowTest {
    @Test public void theMergedAppHasOnePackageAndKeepsTheScreensaverClassNames() {
        assertEquals("ma.chouitv.home", Flow.HOME);
        assertEquals("ma.chouitv.screensaver", Flow.OLD_SAVER);
        assertEquals("ma.chouitv.home/ma.chouitv.screensaver.ScreenDream", Flow.DREAM);
        assertEquals("ma.chouitv.home/ma.chouitv.screensaver.SaverReceiver", Flow.RECEIVER);
        assertEquals("ma.chouitv.screensaver/.SaverReceiver", Flow.OLD_RECEIVER);
    }

    @Test public void theBroadcastGoesToTheMergedAppWithTheUnchangedActions() {
        assertEquals("am broadcast -n ma.chouitv.home/ma.chouitv.screensaver.SaverReceiver -a ma.chouitv.screensaver.GET",
            Flow.saverCommand(Flow.RECEIVER, "GET", ""));
        assertEquals("am broadcast -n ma.chouitv.home/ma.chouitv.screensaver.SaverReceiver -a ma.chouitv.screensaver.SET --es city 'Rabat'",
            Flow.saverCommand(Flow.RECEIVER, "SET", "--es city " + Flow.q("Rabat")));
        assertEquals("am broadcast -n ma.chouitv.screensaver/.SaverReceiver -a ma.chouitv.screensaver.EXPORT",
            Flow.saverCommand(Flow.OLD_RECEIVER, "EXPORT", ""));
    }

    @Test public void theDreamIsTheMergedAppsAndNeverTheOldOne() {
        String[] cmds = Flow.dreamCommands();
        assertEquals(3, cmds.length);
        assertEquals("settings put secure screensaver_components ma.chouitv.home/ma.chouitv.screensaver.ScreenDream", cmds[1]);
        assertEquals("settings put secure screensaver_default_component ma.chouitv.home/ma.chouitv.screensaver.ScreenDream", cmds[2]);
        for (String c : cmds) assertFalse(c, c.contains("ma.chouitv.screensaver/"));
    }

    @Test public void aTvStillOn3xIsSpokenToThroughItsOldReceiver() {
        assertEquals(Flow.OLD_RECEIVER, Flow.receiverFor(177, true));
        assertEquals("4.0 is there: the merged app answers even if the old one is kept", Flow.RECEIVER, Flow.receiverFor(Trust.MIN_VERSION_CODE, true));
        assertEquals(Flow.RECEIVER, Flow.receiverFor(180, false));
        assertEquals("no home, no old app: the new receiver, which will say so", Flow.RECEIVER, Flow.receiverFor(-1, false));
        assertTrue(Flow.receiverFor(-1, true).equals(Flow.OLD_RECEIVER));
    }
}

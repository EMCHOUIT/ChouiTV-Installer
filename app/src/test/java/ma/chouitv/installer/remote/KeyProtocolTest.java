package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/** What the phone and the small program kept running on the TV (helper/Keys.java) say to each other: a key code per line, «ok» or «err …» back. */
public class KeyProtocolTest {
    @Test public void aKeyIsOneLine() {
        assertEquals("19\n", KeyProtocol.line(Remote.UP));
        assertEquals("23\n", KeyProtocol.line(Remote.OK));
        assertEquals("26\n", KeyProtocol.line(Remote.POWER));
    }

    @Test public void onlyTheKnownKeysAreSent() {
        try { KeyProtocol.line(9999); fail("an unknown key must be refused"); } catch (IllegalArgumentException expected) { }
        try { KeyProtocol.line(-1); fail("a negative key must be refused"); } catch (IllegalArgumentException expected) { }
        assertTrue(Remote.isKnown(Remote.PASTE));
        assertFalse(Remote.isKnown(0));
    }

    @Test public void onlyTheNavigationKeysTakeTheFastLink() {
        for (int k : new int[] {Remote.UP, Remote.DOWN, Remote.LEFT, Remote.RIGHT, Remote.OK, Remote.BACK, Remote.HOME, Remote.MENU})
            assertTrue("navigation key " + k, Remote.isFast(k));
        // the volume, mute, power and paste keys keep the slow way: on a TV tried, a volume key injected by the fast link did not act in one state of the home screen
        for (int k : new int[] {Remote.VOLUME_UP, Remote.VOLUME_DOWN, Remote.MUTE, Remote.POWER, Remote.PASTE, Remote.WAKEUP, 9999})
            assertFalse("not a fast key: " + k, Remote.isFast(k));
    }

    @Test public void theAnswerOkMeansTheKeyWasInjected() {
        assertTrue(KeyProtocol.ok("ok"));
        assertTrue(KeyProtocol.ok("ok\r"));
        assertTrue(KeyProtocol.ok("  ok 3ms "));
        assertFalse(KeyProtocol.ok("err java.lang.SecurityException"));
        assertFalse(KeyProtocol.ok("okay then"));   // not the same word
        assertFalse(KeyProtocol.ok(""));
        assertFalse(KeyProtocol.ok(null));
    }

    @Test public void theProgramSaysReadyWhenItCanInject() {
        assertTrue(KeyProtocol.ready("ready"));
        assertTrue(KeyProtocol.ready("ready\r"));
        assertFalse(KeyProtocol.ready("err no input manager"));
        assertFalse(KeyProtocol.ready("Exception in thread \"main\""));
        assertFalse(KeyProtocol.ready(null));
    }

    @Test public void theProgramIsStartedFromTheHelperJar() {
        assertEquals("CLASSPATH=" + Remote.HELPER_REMOTE + " app_process /system/bin Keys", KeyProtocol.command());
    }
}

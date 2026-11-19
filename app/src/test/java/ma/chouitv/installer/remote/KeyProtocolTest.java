package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

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

    @Test public void aTextIsOneLineOfBase64() {
        String l = KeyProtocol.textLine("héllo مرحبا");
        assertTrue(l.startsWith("T ") && l.endsWith("\n"));
        String b64 = l.substring(2, l.length() - 1);
        assertEquals("héllo مرحبا", new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8));
        assertFalse("one line: no newline inside", b64.contains("\n"));
    }

    @Test public void aTextWithANewlineStaysOneLine() {
        String l = KeyProtocol.textLine("a\nb");
        assertEquals(1, l.split("\n").length);
    }

    @Test public void aLongTextIsCutInPiecesOfAtMost200CharactersWithoutBreakingACharacter() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 199; i++) b.append('x');
        b.append("\uD83D\uDE00");   // an emoji at the cut: two units, one character
        b.append("yz");
        List<String> parts = KeyProtocol.chunks(b.toString());
        assertEquals(2, parts.size());
        assertEquals(199 + 2, parts.get(0).length());   // the emoji stays whole in the first piece (200 characters)
        assertEquals("yz", parts.get(1));
        assertEquals(b.toString(), String.join("", parts));
    }

    @Test public void aShortTextIsOnePiece() {
        assertEquals(1, KeyProtocol.chunks("hello").size());
        assertEquals(0, KeyProtocol.chunks("").size());
    }

    @Test public void deleteAndEnterAreKeysOfTheFastLink() {
        assertEquals(67, Remote.DEL);
        assertEquals(66, Remote.ENTER);
        assertTrue(Remote.isKnown(Remote.DEL) && Remote.isKnown(Remote.ENTER));
        assertTrue(Remote.isFast(Remote.DEL) && Remote.isFast(Remote.ENTER));
        assertEquals("67\n", KeyProtocol.line(Remote.DEL));
        assertEquals("66\n", KeyProtocol.line(Remote.ENTER));
    }

    @Test public void theSlowWayDeletesSeveralCharactersWithOneCommand() {
        assertEquals("input keyevent 67 67 67", Remote.keys(Remote.DEL, 3));
        assertEquals("input keyevent 67", Remote.keys(Remote.DEL, 1));
        try { Remote.keys(9999, 2); fail("an unknown key must be refused"); } catch (IllegalArgumentException expected) { }
        try { Remote.keys(Remote.DEL, 0); fail("at least one key"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void clearingTheWholeFieldIsOneLineAndTheEndKeyIsAFastKey() {
        assertEquals("A\n", KeyProtocol.clearLine());
        assertEquals(123, Remote.MOVE_END);
        assertTrue(Remote.isKnown(Remote.MOVE_END) && Remote.isFast(Remote.MOVE_END));
        assertEquals("123\n", KeyProtocol.line(Remote.MOVE_END));
    }

    @Test public void theTextOfTheFocusedFieldIsAskedWithOneLine() {
        assertEquals("R\n", KeyProtocol.fieldLine());
    }

    @Test public void theAnswerToAReadGivesTheTextOfTheField() {
        String b64 = Base64.getEncoder().encodeToString("héllo مرحبا 12".getBytes(StandardCharsets.UTF_8));
        assertEquals("héllo مرحبا 12", KeyProtocol.parseField("ok text " + b64));
        assertEquals("an empty field is an empty text", "", KeyProtocol.parseField("ok text"));
        assertEquals("", KeyProtocol.parseField("ok text \r"));
        assertNull("no field with the input focus, or a password", KeyProtocol.parseField("ok none"));
        assertNull(KeyProtocol.parseField("ok"));
        assertNull(KeyProtocol.parseField("err no accessibility"));
        assertNull(KeyProtocol.parseField("Terminated"));
        assertNull(KeyProtocol.parseField(null));
        assertNull("not base64", KeyProtocol.parseField("ok text !!!not base64!!!"));
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

    @Test public void onlyAnErrorLineIsARefusal() {
        assertTrue(KeyProtocol.isError("err refused"));
        assertTrue(KeyProtocol.isError("err no clipboard\r"));
        assertFalse(KeyProtocol.isError("Terminated"));
        assertFalse(KeyProtocol.isError("ok"));
        assertFalse(KeyProtocol.isError(null));
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

package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What goes to the TV when the text of the phone's field changes: the TV's field is kept the same by deleting from the end and typing, never by rewriting it. */
public class LiveTextTest {
    private static void assertOp(LiveText.Op op, int dels, String text) {
        assertEquals("keys to delete", dels, op.dels);
        assertEquals("text to type", text, op.text);
    }

    @Test public void aTypedLetterIsTypedOnTheTv() {
        LiveText t = new LiveText();
        assertOp(t.edit("a"), 0, "a");
        assertOp(t.edit("ab"), 0, "b");
    }

    @Test public void aPastedWordIsTypedInOnePiece() {
        assertOp(new LiveText().edit("Game of Thrones"), 0, "Game of Thrones");
    }

    @Test public void aBackspaceIsOneDelete() {
        LiveText t = new LiveText();
        t.edit("abc");
        assertOp(t.edit("ab"), 1, "");
    }

    @Test public void anAutoCorrectedWordDeletesFromTheEndAndTypesTheRest() {
        LiveText t = new LiveText();
        t.edit("helo");
        assertOp(t.edit("hello"), 1, "lo");   // «o» deleted, «lo» typed: the cursor of the TV stays at the end
    }

    @Test public void anEditInTheMiddleRetypesTheEnd() {
        LiveText t = new LiveText();
        t.edit("abcd");
        assertOp(t.edit("abXcd"), 2, "Xcd");
    }

    @Test public void clearingTheFieldDeletesEverythingThatWasTyped() {
        LiveText t = new LiveText();
        t.edit("abc");
        assertOp(t.edit(""), 3, "");
    }

    @Test public void nothingChangedSendsNothing() {
        LiveText t = new LiveText();
        t.edit("abc");
        assertNull(t.edit("abc"));
    }

    @Test public void arabicAndLatinAndDigitsAreCountedByCharacter() {
        LiveText t = new LiveText();
        t.edit("مسلسل Game 2");
        assertOp(t.edit("مسلسل Game"), 2, "");   // the space and the digit
        assertOp(t.edit("مسلسل"), 5, "");        // " Game"
    }

    @Test public void aCharacterMadeOfTwoUnitsIsOneDelete() {
        LiveText t = new LiveText();
        t.edit("a😀");   // a, then an emoji: two UTF-16 units, one character
        assertOp(t.edit("a"), 1, "");
    }

    @Test public void whatTheTvConfirmedIsKnownSeparately() {
        LiveText t = new LiveText();
        LiveText.Op a = t.edit("ab");
        t.confirmed(a);
        t.edit("abc");                     // sent, not confirmed
        t.lost();                          // the link went down: the queued part is gone
        assertEquals("ab", t.onTv());
        assertOp(t.pending("abc"), 0, "c");
        assertOp(t.edit("abc"), 0, "c");   // typing again starts from what the TV really has
    }

    @Test public void whatIsPendingTakesTheDeletionsIntoAccount() {
        LiveText t = new LiveText();
        t.confirmed(t.edit("abc"));
        assertOp(t.pending("abXY"), 1, "XY");   // the TV has «abc»: «c» deleted, «XY» typed
        assertNull(t.pending("abc"));
    }

    @Test public void theSlowWayMarksEverythingAsOnTheTv() {
        LiveText t = new LiveText();
        t.edit("abc");
        t.slowSent("abc");
        assertEquals("abc", t.onTv());
        assertNull(t.pending("abc"));
        assertOp(t.edit("abcd"), 0, "d");
    }

    @Test public void theCountOfCharactersIsByCodePoint() {
        assertEquals(0, LiveText.chars(""));
        assertEquals(3, LiveText.chars("abc"));
        assertEquals(2, LiveText.chars("a😀"));
        assertTrue(LiveText.chars("مرحبا") == 5);
    }
}

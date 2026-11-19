package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** The live keyboard: what the phone sends to the TV as the person types, and what it does when the link fails (never losing the text). */
public class LiveKeyboardTest {
    /** A TV link that records what it is given. {@code acks}: the TV confirms each line at once. */
    private static final class FakeLink implements LiveKeyboard.Link {
        boolean up = true, acks = true;
        final List<String> sent = new ArrayList<>();
        final List<Runnable> unconfirmed = new ArrayList<>();
        @Override public boolean up() { return up; }
        @Override public boolean del(int n, Runnable ok) { if (!up) return false; sent.add("del" + n); done(ok); return true; }
        @Override public boolean text(String s, Runnable ok) { if (!up) return false; sent.add("text:" + s); done(ok); return true; }
        @Override public boolean enter() { if (!up) return false; sent.add("enter"); return true; }
        @Override public boolean clearAll(Runnable ok) { if (!up) return false; sent.add("clearAll"); done(ok); return true; }
        @Override public boolean moveToEnd() { if (!up) return false; sent.add("end"); return true; }
        @Override public boolean readField(java.util.function.Consumer<String> onText) { if (!up) return false; sent.add("read"); onText.accept(fieldOfTheTv); return true; }
        String fieldOfTheTv = "";
        private void done(Runnable ok) { if (ok == null) return; if (acks) ok.run(); else unconfirmed.add(ok); }
    }

    @Test public void itStartsReadyWhenTheLinkIsUp() {
        assertEquals(LiveKeyboard.Mode.READY, new LiveKeyboard(new FakeLink()).mode());
    }

    @Test public void itStartsReconnectingWhenThereIsNoLink() {
        FakeLink l = new FakeLink(); l.up = false;
        assertEquals(LiveKeyboard.Mode.RECONNECTING, new LiveKeyboard(l).mode());
    }

    @Test public void eachChangeOfTheFieldGoesToTheTv() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("a"); k.typed("ab"); k.typed("a");
        assertEquals("[text:a, text:b, del1]", l.sent.toString());
        assertEquals(LiveKeyboard.Mode.DIRECT, k.mode());
        assertEquals(0, k.waiting());
    }

    @Test public void aLongPasteIsCutInPiecesAndKeepsItsOrder() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 450; i++) b.append((char) ('a' + i % 26));
        k.typed(b.toString());
        assertEquals(3, l.sent.size());
        assertEquals(200, l.sent.get(0).length() - "text:".length());
        assertEquals(200, l.sent.get(1).length() - "text:".length());
        assertEquals(50, l.sent.get(2).length() - "text:".length());
        StringBuilder back = new StringBuilder();
        for (String s : l.sent) back.append(s.substring("text:".length()));
        assertEquals(b.toString(), back.toString());
    }

    @Test public void theEnterKeyGoesToTheTv() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("tv");
        k.enter();
        assertEquals("[text:tv, enter]", l.sent.toString());
    }

    @Test public void aLinkLostWhileTypingKeepsTheTextAndCountsWhatWaits() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("ab");
        l.up = false;
        k.typed("abcd");
        assertEquals(LiveKeyboard.Mode.RECONNECTING, k.mode());
        assertEquals("the two letters not on the TV", 2, k.waiting());
    }

    @Test public void whenTheLinkIsBackTheWaitingTextGoesOut() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("ab");
        l.up = false;
        k.typed("abcd");
        l.sent.clear();
        l.up = true;
        k.tick(1_000);
        assertEquals("[text:cd]", l.sent.toString());
        assertEquals(LiveKeyboard.Mode.DIRECT, k.mode());
        assertEquals(0, k.waiting());
    }

    @Test public void aLineTheTvDidNotConfirmWhenTheLinkFailedIsSentAgainFromWhatTheTvHas() {
        FakeLink l = new FakeLink();
        l.acks = false;                    // the TV confirms nothing
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");                    // sent, not confirmed
        k.linkLost();                      // the link broke
        l.sent.clear();
        l.acks = true;
        k.tick(1_000);
        assertEquals("[text:abc]", l.sent.toString());
    }

    @Test public void aConfirmationAfterTheFailureIsIgnored() {
        FakeLink l = new FakeLink();
        l.acks = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");
        k.linkLost();
        for (Runnable r : l.unconfirmed) r.run();   // a late answer of the dead link
        assertEquals("the TV is not taken to have the text", 3, k.waiting());
    }

    @Test public void afterSixSecondsWithoutALinkItGoesToTheSlowMode() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.tick(0);
        k.typed("abc");
        k.tick(5_999);
        assertEquals(LiveKeyboard.Mode.RECONNECTING, k.mode());
        k.tick(6_000);
        assertEquals(LiveKeyboard.Mode.SLOW, k.mode());
        assertEquals(3, k.waiting());
    }

    @Test public void inTheSlowModeNothingGoesOutUntilSendAndTheTextIsKept() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.tick(0); k.tick(6_000);
        k.typed("abc"); k.typed("abcd");
        assertTrue(l.sent.isEmpty());
        LiveText.Op op = k.pendingForSlowSend();
        assertEquals(0, op.dels);
        assertEquals("abcd", op.text);
    }

    @Test public void afterTheSlowSendNothingWaitsAnyMore() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.tick(0); k.tick(6_000);
        k.typed("abc");
        k.slowSent();
        assertEquals(0, k.waiting());
        assertNull(k.pendingForSlowSend());
        k.typed("abcd");
        assertEquals("only what was typed since", "d", k.pendingForSlowSend().text);
    }

    @Test public void tryingLiveAgainFromTheSlowModeGoesBackToDirectWhenTheLinkIsThere() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.tick(0); k.tick(6_000);
        k.typed("abc");
        l.up = true;
        k.retryLive(10_000);
        assertEquals(LiveKeyboard.Mode.RECONNECTING, k.mode());
        k.tick(10_100);
        assertEquals(LiveKeyboard.Mode.DIRECT, k.mode());
        assertEquals("[text:abc]", l.sent.toString());
    }

    @Test public void aTvThatRefusesPutsTheKeyboardInTheErrorStateAndKeepsTheText() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");
        k.linkRefused();
        assertEquals(LiveKeyboard.Mode.ERROR, k.mode());
        k.typed("abcd");
        assertEquals("kept, not lost", 1, k.waiting());
        k.retryLive(1_000);
        assertEquals(LiveKeyboard.Mode.RECONNECTING, k.mode());
    }

    @Test public void fromTheErrorStateTheSlowModeCanBeChosen() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");
        k.linkRefused();
        k.chooseSlow();
        assertEquals(LiveKeyboard.Mode.SLOW, k.mode());
    }

    @Test public void clearAllEmptiesTheWholeFieldOfTheTvNotOnlyWhatWasTyped() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");
        l.sent.clear();
        k.clearAll();
        assertEquals("[clearAll]", l.sent.toString());
        k.typed("");                         // the phone's field is emptied too: nothing more to send
        assertEquals("[clearAll]", l.sent.toString());
        k.typed("x");
        assertEquals("[clearAll, text:x]", l.sent.toString());   // typed on an empty field: no delete first
        assertEquals(0, k.waiting());
    }

    @Test public void clearAllWorksOnATextThatWasNeverTypedOnThePhone() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.clearAll();                        // the field was filled with the TV's own keyboard
        assertEquals("[clearAll]", l.sent.toString());
    }

    @Test public void clearAllWithoutALinkJustEmptiesTheWaitingText() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("abc");
        k.clearAll();
        assertTrue(l.sent.isEmpty());
        assertEquals(0, k.waiting());
        assertEquals(LiveKeyboard.Mode.RECONNECTING, k.mode());
    }

    @Test public void aBackspaceOnAnEmptyFieldOfThePhoneDeletesOneCharacterOnTheTv() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.deleteOnTv();
        k.deleteOnTv();
        assertEquals("[del1, del1]", l.sent.toString());
        assertEquals("the phone's field is not changed by it", 0, k.chars());
    }

    @Test public void aBackspaceOnTheTvDoesNothingWithoutALiveLink() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.deleteOnTv();
        assertTrue(l.sent.isEmpty());
    }

    @Test public void theTextThatIsOnTheTvIsTakenAsTheStartingPointWhenTheFieldIsStillEmpty() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        assertTrue(k.preloaded("from the TV"));
        assertEquals("[end]", l.sent.toString());          // the TV's cursor goes to the end: what is typed comes after
        k.typed("from the TV");                              // the phone's field is filled with it: nothing to send
        assertEquals("[end]", l.sent.toString());
        k.typed("from the TV!");
        k.typed("from the T");
        assertEquals("[end, text:!, del2]", l.sent.toString());
        assertEquals(0, k.waiting());
    }

    @Test public void theFieldOfTheTvCanBeAskedForWhileTheLinkIsLive() {
        FakeLink l = new FakeLink(); l.fieldOfTheTv = "already there";
        LiveKeyboard k = new LiveKeyboard(l);
        final String[] got = {null};
        assertTrue(k.readExisting(t -> got[0] = t));
        assertEquals("already there", got[0]);
        FakeLink down = new FakeLink(); down.up = false;
        assertFalse("without a live link there is nothing to read", new LiveKeyboard(down).readExisting(t -> { }));
    }

    @Test public void theTextOfTheTvIsNotTakenOnceThePersonHasTyped() {
        FakeLink l = new FakeLink();
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("hello");
        l.sent.clear();
        assertFalse(k.preloaded("from the TV"));
        assertTrue(l.sent.isEmpty());
    }

    @Test public void nothingIsTakenFromAnEmptyFieldOrWithoutALink() {
        FakeLink l = new FakeLink();
        assertFalse(new LiveKeyboard(l).preloaded(""));
        assertFalse(new LiveKeyboard(l).preloaded(null));
        FakeLink down = new FakeLink(); down.up = false;
        assertFalse(new LiveKeyboard(down).preloaded("x"));
    }

    @Test public void theCountIsTheNumberOfCharactersOfTheField() {
        LiveKeyboard k = new LiveKeyboard(new FakeLink());
        k.typed("مرحبا 12");
        assertEquals(8, k.chars());
    }

    @Test public void aKeyboardWhoseLinkWasNeverUpDoesNotClaimToBeLive() {
        FakeLink l = new FakeLink(); l.up = false;
        LiveKeyboard k = new LiveKeyboard(l);
        k.typed("a");
        assertFalse(k.mode() == LiveKeyboard.Mode.DIRECT);
    }
}

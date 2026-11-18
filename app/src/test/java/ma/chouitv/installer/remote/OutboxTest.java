package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * What waits to go to the TV: key presses (a few at most: a finger held on an arrow sends them faster than the TV takes them) and lines of
 * typed text (never dropped, never reordered: each one is a letter somebody typed).
 */
public class OutboxTest {
    private static String line(Outbox.Item i) { return i == null ? null : i.line; }

    @Test public void keysGoOutInTheOrderTheyCame() {
        Outbox q = new Outbox();
        q.offerKey(Remote.UP); q.offerKey(Remote.OK);
        assertEquals(2, q.size());
        assertEquals("19\n", line(q.poll()));
        assertEquals("23\n", line(q.poll()));
        assertNull(q.poll());
    }

    @Test public void aHeldArrowDoesNotGrowTheQueue() {
        Outbox q = new Outbox();
        for (int i = 0; i < 40; i++) q.offerKey(Remote.DOWN);
        assertEquals(Outbox.MAX_KEYS, q.size());
    }

    @Test public void whenFullADifferentKeyReplacesTheOldestKey() {
        Outbox q = new Outbox();
        for (int i = 0; i < Outbox.MAX_KEYS; i++) q.offerKey(Remote.DOWN);
        q.offerKey(Remote.OK);
        assertEquals(Outbox.MAX_KEYS, q.size());
        String last = null; Outbox.Item i;
        while ((i = q.poll()) != null) last = i.line;
        assertEquals("the new key goes out last", "23\n", last);
    }

    @Test public void linesOfTextAreNeverDropped() {
        Outbox q = new Outbox();
        for (int i = 0; i < 100; i++) q.offerLine("T " + i + "\n", null);
        assertEquals(100, q.size());
        for (int i = 0; i < 100; i++) assertEquals("T " + i + "\n", line(q.poll()));
    }

    @Test public void textAndKeysKeepTheirOrder() {
        Outbox q = new Outbox();
        q.offerLine("67\n", null);
        q.offerLine("T YQ==\n", null);
        q.offerKey(Remote.ENTER);
        assertEquals("67\n", line(q.poll()));
        assertEquals("T YQ==\n", line(q.poll()));
        assertEquals("66\n", line(q.poll()));
    }

    @Test public void aFullQueueOfKeysNeverDropsALine() {
        Outbox q = new Outbox();
        q.offerLine("T YQ==\n", null);
        for (int i = 0; i < 10; i++) q.offerKey(Remote.DOWN);
        q.offerKey(Remote.OK);
        assertEquals("T YQ==\n", line(q.poll()));
        assertEquals(Outbox.MAX_KEYS, q.size());
    }

    @Test public void aLineRemembersWhatToDoWhenTheTvConfirmsIt() {
        Outbox q = new Outbox();
        final boolean[] done = {false};
        q.offerLine("T YQ==\n", () -> done[0] = true);
        Outbox.Item i = q.poll();
        assertEquals(-1, i.code);
        i.onConfirmed.run();
        assertTrue(done[0]);
        q.offerKey(Remote.UP);
        assertEquals("a key has its code, for the report of a failure", Remote.UP, q.poll().code);
    }

    @Test public void clearEmptiesIt() {
        Outbox q = new Outbox();
        q.offerKey(Remote.UP); q.offerLine("T YQ==\n", null);
        q.clear();
        assertEquals(0, q.size());
        assertNull(q.poll());
    }

    @Test public void takeWaitsForSomething() throws Exception {
        final Outbox q = new Outbox();
        final String[] got = {null};
        Thread t = new Thread(() -> { try { got[0] = q.take().line; } catch (InterruptedException ignored) { } });
        t.start();
        Thread.sleep(50);
        assertNull(got[0]);
        q.offerLine("T YQ==\n", null);
        t.join(2000);
        assertEquals("T YQ==\n", got[0]);
    }

    @Test public void takeEndsWhenTheThreadIsInterrupted() throws Exception {
        final Outbox q = new Outbox();
        final boolean[] interrupted = {false};
        Thread t = new Thread(() -> { try { q.take(); } catch (InterruptedException e) { interrupted[0] = true; } });
        t.start();
        Thread.sleep(50);
        t.interrupt();
        t.join(2000);
        assertTrue(interrupted[0]);
    }
}

package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The keys waiting to go to the TV: a few at most, so that a finger held on an arrow never builds a queue the TV keeps scrolling after the release. */
public class KeyQueueTest {
    @Test public void keysGoOutInTheOrderTheyCame() {
        KeyQueue q = new KeyQueue();
        q.offer(Remote.UP); q.offer(Remote.OK);
        assertEquals(2, q.size());
        assertEquals(Integer.valueOf(Remote.UP), q.poll());
        assertEquals(Integer.valueOf(Remote.OK), q.poll());
        assertNull(q.poll());
    }

    @Test public void aHeldArrowDoesNotGrowTheQueue() {
        KeyQueue q = new KeyQueue();
        for (int i = 0; i < 40; i++) q.offer(Remote.DOWN);   // the finger stays down while the TV is slow
        assertEquals(KeyQueue.MAX, q.size());
        int n = 0;
        while (q.poll() != null) n++;
        assertEquals("only a few repeats are left to play after the release", KeyQueue.MAX, n);
    }

    @Test public void whenFullADifferentKeyReplacesTheOldestOne() {
        KeyQueue q = new KeyQueue();
        for (int i = 0; i < KeyQueue.MAX; i++) q.offer(Remote.DOWN);
        q.offer(Remote.OK);   // a real tap must not be lost behind the repeats
        assertEquals(KeyQueue.MAX, q.size());
        Integer last = null, c;
        while ((c = q.poll()) != null) last = c;
        assertEquals("the new key is the last one to go out", Integer.valueOf(Remote.OK), last);
    }

    @Test public void clearEmptiesIt() {
        KeyQueue q = new KeyQueue();
        q.offer(Remote.UP);
        q.clear();
        assertEquals(0, q.size());
        assertNull(q.poll());
    }

    @Test public void takeWaitsForAKey() throws Exception {
        final KeyQueue q = new KeyQueue();
        final int[] got = {-1};
        Thread t = new Thread(() -> { try { got[0] = q.take(); } catch (InterruptedException ignored) { } });
        t.start();
        Thread.sleep(50);
        assertEquals("nothing yet", -1, got[0]);
        q.offer(Remote.BACK);
        t.join(2000);
        assertEquals(Remote.BACK, got[0]);
    }

    @Test public void takeEndsWhenTheThreadIsInterrupted() throws Exception {
        final KeyQueue q = new KeyQueue();
        final boolean[] interrupted = {false};
        Thread t = new Thread(() -> { try { q.take(); } catch (InterruptedException e) { interrupted[0] = true; } });
        t.start();
        Thread.sleep(50);
        t.interrupt();
        t.join(2000);
        assertTrue(interrupted[0]);
    }
}

package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

/** The link kept open to the TV: sends the keys one after the other, waits for each answer, and says when it can no longer be trusted (so that the key goes the slow way). */
public class KeyLinkTest {
    /** A TV that answers from a script: «ok», an error, nothing (a dead link), or a broken connection. */
    private static final class FakeTv implements KeyLink.Duplex {
        final List<String> written = Collections.synchronizedList(new ArrayList<String>());
        final BlockingQueue<String> answers = new LinkedBlockingQueue<>();   // "" = timeout, "!" = connection closed
        volatile boolean closed;
        @Override public void write(String s) throws IOException { if (closed) throw new IOException("closed"); written.add(s); }
        @Override public String readLine() throws IOException {
            try {
                String a = answers.poll(300, TimeUnit.MILLISECONDS);
                if (a == null || a.isEmpty()) throw new SocketTimeoutException("no answer");
                if (a.equals("!")) throw new IOException("the TV closed the connection");
                return a;
            } catch (InterruptedException e) { throw new IOException("interrupted"); }
        }
        @Override public void close() { closed = true; }
    }

    private KeyLink link;
    @After public void stop() { if (link != null) link.close(); }

    private static final class Failures implements KeyLink.Listener {
        final List<Integer> codes = Collections.synchronizedList(new ArrayList<Integer>());
        final CountDownLatch first = new CountDownLatch(1);
        volatile boolean delivered;
        @Override public void failed(int code, boolean mayHaveReachedTheTv, Exception why) { codes.add(code); delivered = mayHaveReachedTheTv; first.countDown(); }
    }

    @Test public void eachKeyIsSentAsALineAndItsAnswerIsAwaited() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.answers.add("ok"); tv.answers.add("ok");
        assertTrue(link.offer(Remote.UP));
        assertTrue(link.offer(Remote.OK));
        long end = System.currentTimeMillis() + 2000;
        while (tv.written.size() < 2 && System.currentTimeMillis() < end) Thread.sleep(10);
        assertEquals("19\n", tv.written.get(0));
        assertEquals("23\n", tv.written.get(1));
        assertTrue(link.isUp());
        assertTrue(f.codes.isEmpty());
    }

    @Test public void anErrorFromTheTvPutsTheLinkDownAndHandsTheKeyBack() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.answers.add("err java.lang.SecurityException");
        link.offer(Remote.BACK);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertEquals(Integer.valueOf(Remote.BACK), f.codes.get(0));
        assertFalse("the TV said it did not inject it: the key can safely go the slow way", f.delivered);
        assertFalse(link.isUp());
        assertFalse("a link that is down takes no more keys: they go the slow way", link.offer(Remote.OK));
    }

    @Test public void aTvThatDoesNotAnswerIsDeclaredDeadAtOnce() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.answers.add("");   // the answer times out
        link.offer(Remote.DOWN);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertEquals(Integer.valueOf(Remote.DOWN), f.codes.get(0));
        assertTrue("sent, no answer: it may have been injected, so it must not be sent twice", f.delivered);
        assertFalse(link.isUp());
    }

    @Test public void aBrokenConnectionIsReported() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.closed = true;   // the write itself fails
        link.offer(Remote.LEFT);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertFalse("the write itself failed: nothing reached the TV", f.delivered);
        assertFalse(link.isUp());
    }

    @Test public void closingStopsItAndClosesTheConnection() throws Exception {
        FakeTv tv = new FakeTv();
        link = new KeyLink(tv, new Failures());
        link.close();
        assertTrue(tv.closed);
        assertFalse(link.isUp());
        assertFalse(link.offer(Remote.OK));
    }

    @Test public void aHeldArrowOnASlowTvLeavesOnlyAFewKeysToPlay() throws Exception {
        FakeTv tv = new FakeTv();
        link = new KeyLink(tv, new Failures());
        for (int i = 0; i < 50; i++) link.offer(Remote.DOWN);   // the TV has not answered the first one yet
        Thread.sleep(50);
        for (int i = 0; i < 60; i++) tv.answers.add("ok");
        Thread.sleep(400);
        assertTrue("at most the key in flight and the few kept: " + tv.written.size(), tv.written.size() <= KeyQueue.MAX + 1);
    }
}

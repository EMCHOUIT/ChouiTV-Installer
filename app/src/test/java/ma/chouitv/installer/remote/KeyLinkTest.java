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
        volatile Exception why;
        @Override public void failed(int code, boolean mayHaveReachedTheTv, Exception why) { codes.add(code); delivered = mayHaveReachedTheTv; this.why = why; first.countDown(); }
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

    @Test public void aLineOfTextIsSentAsItIsAndConfirmedWhenTheTvSaysOk() throws Exception {
        FakeTv tv = new FakeTv();
        link = new KeyLink(tv, new Failures());
        final CountDownLatch confirmed = new CountDownLatch(1);
        tv.answers.add("ok");
        assertTrue(link.offerLine("T YQ==\n", confirmed::countDown));
        assertTrue(confirmed.await(2, TimeUnit.SECONDS));
        assertEquals("T YQ==\n", tv.written.get(0));
    }

    @Test public void textAndKeysLeaveInTheOrderTheyWereGiven() throws Exception {
        FakeTv tv = new FakeTv();
        link = new KeyLink(tv, new Failures());
        for (int i = 0; i < 3; i++) tv.answers.add("ok");
        link.offerLine("67\n", null);
        link.offerLine("T YQ==\n", null);
        link.offer(Remote.ENTER);
        long end = System.currentTimeMillis() + 2000;
        while (tv.written.size() < 3 && System.currentTimeMillis() < end) Thread.sleep(10);
        assertEquals("[67\n, T YQ==\n, 66\n]", tv.written.toString());
    }

    @Test public void aLineThatFailsIsReportedWithoutAKeyCodeAndIsNotConfirmed() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        final boolean[] confirmed = {false};
        tv.answers.add("err no clipboard");
        link.offerLine("T YQ==\n", () -> confirmed[0] = true);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertEquals(Integer.valueOf(-1), f.codes.get(0));
        assertFalse(confirmed[0]);
        assertFalse(link.offerLine("T Yg==\n", null));
    }

    @Test public void anAnswerThatIsAnErrorIsSaidToBeRefusedAndASilenceIsNot() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.answers.add("err java.lang.SecurityException");
        link.offer(Remote.BACK);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertTrue("the TV answered an error: refused", f.why instanceof KeyLink.Refused);

        FakeTv tv2 = new FakeTv();
        Failures f2 = new Failures();
        KeyLink silent = new KeyLink(tv2, f2);
        tv2.answers.add("");
        silent.offer(Remote.BACK);
        assertTrue(f2.first.await(2, TimeUnit.SECONDS));
        assertFalse("a silence is not a refusal", f2.why instanceof KeyLink.Refused);
        silent.close();
    }

    @Test public void aLineThatIsNeitherOkNorAnErrorIsABrokenLinkNotARefusal() throws Exception {
        FakeTv tv = new FakeTv();
        Failures f = new Failures();
        link = new KeyLink(tv, f);
        tv.answers.add("Terminated");   // what the TV's shell prints when the program is killed
        link.offer(Remote.OK);
        assertTrue(f.first.await(2, TimeUnit.SECONDS));
        assertFalse("the link is down, the TV did not refuse anything", f.why instanceof KeyLink.Refused);
        assertFalse(link.isUp());
    }

    @Test public void aHeldArrowOnASlowTvLeavesOnlyAFewKeysToPlay() throws Exception {
        FakeTv tv = new FakeTv();
        link = new KeyLink(tv, new Failures());
        for (int i = 0; i < 50; i++) link.offer(Remote.DOWN);   // the TV has not answered the first one yet
        Thread.sleep(50);
        for (int i = 0; i < 60; i++) tv.answers.add("ok");
        Thread.sleep(400);
        assertTrue("at most the key in flight and the few kept: " + tv.written.size(), tv.written.size() <= Outbox.MAX_KEYS + 1);
    }
}

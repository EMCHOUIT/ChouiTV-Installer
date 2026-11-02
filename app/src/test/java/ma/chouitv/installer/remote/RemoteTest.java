package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Test;

import ma.chouitv.installer.cleanup.Shell;

public class RemoteTest {
    /** A TV that records the lines it is given and answers from a script. */
    private static final class Tv implements Shell {
        final List<String> log = new ArrayList<>();
        String clipAnswer = "set via 3 params";
        String startAnswer = "Starting: Intent { cmp=x }";
        String packages = "";
        @Override public String run(String c) throws IOException {
            log.add(c);
            if (c.contains("app_process")) return clipAnswer;
            if (c.startsWith("pm list packages")) return packages;
            if (c.startsWith("am start")) return startAnswer;
            return "";
        }
    }

    @Test public void theKeysAreOnlyTheKnownOnes() {
        assertEquals("input keyevent 19", Remote.key(Remote.UP));
        assertEquals("input keyevent 23", Remote.key(Remote.OK));
        assertEquals("input keyevent 164", Remote.key(Remote.MUTE));
        assertEquals("input keyevent 26", Remote.key(Remote.POWER));   // puts the TV to sleep or wakes it
        try { Remote.key(9999); fail("an unknown key must be refused"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void theOpenButtonsStartTheRightComponents() {
        assertEquals("am start -n ma.chouitv.home/.MainActivity", Remote.open(Remote.Target.HOME));
        assertEquals("am start -n ma.chouitv.home/.tilawah.MainActivity", Remote.open(Remote.Target.LISTEN));
        assertEquals("am start -n com.majnioui.tv.channels/.MainActivity", Remote.open(Remote.Target.CHANNELS));
        assertEquals("am start -n com.android.systemui/.Somnambulator", Remote.open(Remote.Target.SAVER));
    }

    @Test public void textGoesThroughTheClipboardAndThePasteKeyWhateverTheAlphabet() throws Exception {
        Tv tv = new Tv();
        String arabic = "مرحبا ChouiTV ١٢٣";
        Remote.sendText(tv, arabic);
        assertEquals(2, tv.log.size());
        assertTrue(tv.log.get(0).startsWith("CLASSPATH=" + Remote.HELPER_REMOTE + " app_process /system/bin Clip set "));
        String b64 = tv.log.get(0).substring(tv.log.get(0).lastIndexOf(' ') + 1);
        assertEquals(arabic, new String(Base64.getDecoder().decode(b64), "UTF-8"));
        assertEquals("input keyevent 279", tv.log.get(1));
    }

    @Test public void aTvThatRefusesTheClipboardIsReportedAndNothingIsPasted() {
        Tv tv = new Tv();
        tv.clipAnswer = "no setPrimaryClip";
        try { Remote.sendText(tv, "abc"); fail("expected an IOException"); } catch (IOException expected) { }
        assertEquals(1, tv.log.size());   // the paste key was not sent
    }

    @Test public void emptyOrTooLongTextIsRefusedBeforeAnythingIsSent() {
        Tv tv = new Tv();
        for (String bad : new String[] {"", "   ", null}) {
            try { Remote.sendText(tv, bad); fail("expected a refusal for: " + bad); } catch (IllegalArgumentException | IOException expected) { }
        }
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < Remote.MAX_TEXT + 1; i++) big.append('a');
        try { Remote.sendText(tv, big.toString()); fail("expected a refusal"); } catch (IllegalArgumentException | IOException expected) { }
        assertTrue(tv.log.isEmpty());
    }

    @Test public void thePasswordOfAFieldIsNeverLoggedByTheCommandItself() throws Exception {
        Tv tv = new Tv();
        Remote.sendText(tv, "secret-pass");
        for (String line : tv.log) assertFalse("the clear text must not travel in a command line", line.contains("secret-pass"));
    }

    @Test public void anAppIsInstalledOnlyWhenPmListsExactlyThatPackage() throws Exception {
        Tv tv = new Tv();
        tv.packages = "package:com.majnioui.tv.channels.helper\n";
        assertFalse(Remote.installed(tv, "com.majnioui.tv.channels"));
        tv.packages = "package:com.majnioui.tv.channels\n";
        assertTrue(Remote.installed(tv, "com.majnioui.tv.channels"));
        try { Remote.installed(tv, "bad; reboot"); fail("a package name must be checked"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void aStartIsReportedAsDoneRefusedOrNotOpenable() throws Exception {
        Tv tv = new Tv();
        assertEquals(Remote.Started.OK, Remote.start(tv, Remote.Target.HOME));
        tv.startAnswer = "Starting: Intent { cmp=x }\nException occurred while executing start:\njava.lang.SecurityException: Permission Denial: starting Intent ... not exported from uid 10117";
        assertEquals(Remote.Started.NOT_OPENABLE, Remote.start(tv, Remote.Target.LISTEN));
        tv.startAnswer = "Error: Activity class {a/b} does not exist.";
        assertEquals(Remote.Started.NOT_OPENABLE, Remote.start(tv, Remote.Target.CHANNELS));
    }
}

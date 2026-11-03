package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class RemoteV4Test {
    private static final class Mem implements LastTv.Store {
        final Map<String, String> m = new HashMap<>();
        @Override public String get(String k) { return m.get(k); }
        @Override public void put(String k, String v) { m.put(k, v); }
    }

    @Test public void theLastTvIsRememberedWithItsAddressAndName() {
        Mem s = new Mem();
        assertNull(LastTv.load(s));
        LastTv.save(s, "192.168.1.50", "Salon");
        LastTv t = LastTv.load(s);
        assertEquals("192.168.1.50", t.ip);
        assertEquals("Salon", t.name);
    }

    @Test public void aBrokenOrEmptyMemoryGivesNoTvInsteadOfACrash() {
        Mem s = new Mem();
        s.put("last_ip", "not an address");
        assertNull(LastTv.load(s));
        s.put("last_ip", "");
        assertNull(LastTv.load(s));
    }

    @Test public void forgettingTheTvEmptiesTheMemory() {
        Mem s = new Mem();
        LastTv.save(s, "10.0.0.7", "TV2");
        LastTv.forget(s);
        assertNull(LastTv.load(s));
    }

    @Test public void theReconnectWaitsLongerAndGivesUpAfterFiveTries() {
        assertEquals(1000, Reconnect.delayMs(1));
        assertEquals(2000, Reconnect.delayMs(2));
        assertEquals(4000, Reconnect.delayMs(3));
        assertEquals(8000, Reconnect.delayMs(4));
        assertEquals(8000, Reconnect.delayMs(5));
        assertFalse(Reconnect.giveUp(5));
        assertTrue(Reconnect.giveUp(6));
    }

    @Test public void aDroppedLinkIsRetriedButARefusedKeyIsNot() {
        assertTrue(Reconnect.worthRetrying(new IOException("Connection reset")));
        assertTrue(Reconnect.worthRetrying(new IOException("Broken pipe")));
        assertTrue(Reconnect.worthRetrying(new java.net.SocketTimeoutException("timeout")));
        assertFalse(Reconnect.worthRetrying(new IllegalArgumentException("not a remote key")));
        assertFalse(Reconnect.worthRetrying(new SecurityException("denied")));
    }

    @Test public void sixSlotsStartEmptyAndKeepWhatTheUserChose() {
        AppSlots a = AppSlots.parse("");
        for (int i = 0; i < AppSlots.COUNT; i++) assertNull(a.get(i));
        a = a.with(1, "com.netflix.ninja").with(4, "org.example.quran");
        AppSlots back = AppSlots.parse(a.serialize());
        assertEquals("com.netflix.ninja", back.get(1));
        assertEquals("org.example.quran", back.get(4));
        assertNull(back.get(0));
        assertEquals(1, back.slotOf("com.netflix.ninja"));
        assertEquals(-1, back.slotOf("com.nothing"));
    }

    @Test public void clearingASlotAndBadValuesAreHandled() {
        AppSlots a = AppSlots.parse("").with(2, "com.netflix.ninja").with(2, null);
        assertNull(a.get(2));
        assertNull(AppSlots.parse("a b;rm -rf,,,,,").get(0));   // not a package name: dropped
        try { AppSlots.parse("").with(6, "com.x.y"); fail("only six slots"); } catch (IndexOutOfBoundsException expected) { }
        try { AppSlots.parse("").with(0, "bad name; reboot"); fail("not a package"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void theSlotsOfEachTvAreSeparate() {
        Mem s = new Mem();
        AppSlots.save(s, "Salon", AppSlots.parse("").with(0, "com.netflix.ninja"));
        assertEquals("com.netflix.ninja", AppSlots.load(s, "Salon").get(0));
        assertNull(AppSlots.load(s, "Chambre").get(0));
    }

    @Test public void theAppsListIsReadFromPmAndNamedForHumans() {
        String out = "package:com.netflix.ninja\r\npackage:com.google.android.youtube.tv\npackage:org.example.quranplayer\npackage:com.amazon.amazonvideo.livingroom\njunk\n";
        List<String> pk = Apps.parse(out);
        assertEquals(4, pk.size());
        assertEquals("Netflix", Apps.label("com.netflix.ninja"));
        assertEquals("YouTube", Apps.label("com.google.android.youtube.tv"));
        assertEquals("Prime Video", Apps.label("com.amazon.amazonvideo.livingroom"));
        assertEquals("Quranplayer", Apps.label("org.example.quranplayer"));
    }

    @Test public void theNameAndIconTheTvShowsAreReadFromTheHelperAnswer() {
        String oscar = java.util.Base64.getEncoder().encodeToString("Oscar TV".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String arabic = java.util.Base64.getEncoder().encodeToString("القرآن".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String png = java.util.Base64.getEncoder().encodeToString(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        String out = "com.drama.mp4|" + oscar + "|" + png + "\r\norg.example.quran|" + arabic + "|\nxyz.unknown||\nnot a line\n";
        Map<String, Apps.Info> m = Apps.parseInfo(out);
        assertEquals("Oscar TV", m.get("com.drama.mp4").label);
        assertEquals(4, m.get("com.drama.mp4").icon.length);
        assertEquals("القرآن", m.get("org.example.quran").label);
        assertNull(m.get("org.example.quran").icon);
        assertNull(m.get("xyz.unknown"));   // nothing usable: the caller falls back to its own naming
        assertEquals(2, m.size());
    }

    @Test public void theNameCommandAsksOnlyForValidPackages() {
        String c = Remote.appInfo(Arrays.asList("com.drama.mp4", "com.netflix.ninja"));
        assertEquals("CLASSPATH=/data/local/tmp/chouitv-clip.jar app_process /system/bin AppInfo com.drama.mp4 com.netflix.ninja", c);
        try { Remote.appInfo(Arrays.asList("com.ok.app", "bad; reboot")); fail("not a package"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void theRealAnswerOfAnAndroid11TvIsUnderstood() {
        String out = "21 activities found:\n  Activity #0:\n    priority=3 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\n"
            + "    com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity\n  Activity #3:\n"
            + "    com.mediatek.wwtv.tvcenter/com.mitv.livetv.nav.TurnkeyUiMainActivity\n    xyz.netfly/com.video_cloud.SplashActivity\n"
            + "    com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity\n";
        List<String> pk = Apps.parse(out);
        assertEquals(Arrays.asList("com.google.android.youtube.tv", "com.mediatek.wwtv.tvcenter", "xyz.netfly", "com.amazon.amazonvideo.livingroom"), pk);
    }

    @Test public void theSystemAndOurOwnAppsAreNotOfferedAsChoices() {
        List<String> shown = Apps.choices(Arrays.asList("com.netflix.ninja", "ma.chouitv.home", "com.android.settings", "org.example.app"));
        assertTrue(shown.contains("com.netflix.ninja"));
        assertTrue(shown.contains("org.example.app"));
        assertFalse(shown.contains("ma.chouitv.home"));   // it has its own button
        assertFalse(shown.contains("com.android.settings"));
    }

    @Test public void launchingAnAppAndOpeningTheSettingsBuildTheRightCommands() {
        assertEquals("monkey -p com.netflix.ninja -c android.intent.category.LEANBACK_LAUNCHER 1", Remote.launch("com.netflix.ninja"));
        assertEquals("am start -a android.settings.SETTINGS", Remote.settings());
        try { Remote.launch("x; reboot"); fail("not a package"); } catch (IllegalArgumentException expected) { }
    }
}

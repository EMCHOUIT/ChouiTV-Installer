package ma.chouitv.installer.cleanup;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ma.chouitv.installer.cleanup.CleanupData.Group;
import ma.chouitv.installer.cleanup.Planner.Plan;

/** The person's own apps and the ticked list: what is recommended and why, what is never offered, and that only ticked apps are touched. */
public class UserAppsTest {
    // real formats, taken from a TV
    private static final String BOOT =
        "61 receivers found:\n  Receiver #0:\n    priority=1000 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false\n    android/com.android.server.BootReceiver\n"
        + "  Receiver #1:\n    priority=1000 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false\n    com.mediatek.backgrounddetection/.BootReceiver\n"
        + "  Receiver #2:\n    priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=false\n    com.nova.tv/.BootReceiver\n";
    private static final String MEM =
        "Applications Memory Usage (in Kilobytes):\nUptime: 1\n\nTotal PSS by process:\n    137,872K: ma.chouitv.screensaver (pid 32439 / activities)\n"
        + "     47,673K: com.google.android.webview:sandboxed_process0:org.chromium.content.app.SandboxedProcessService0:0 (pid 843)\n"
        + "     31,155K: ver3.ycntivi.off (pid 7077)\n     52,000K: com.nova.tv (pid 8000)\n     10,240K: ver3.ycntivi.off:remote (pid 7100)\n\nTotal PSS by OOM adjustment:\n   999,999K: Native\n";
    private static final String USAGE =
        "      package=mitv.service totalTimeUsed=\"00:00\" lastTimeUsed=\"1970-01-01 00:00:00\" totalTimeVisible=\"00:00\" lastTimeVisible=\"1970-01-01 00:00:00\" totalTimeFS=\"00:00\" lastTimeFS=\"1970-01-01 00:00:00\" appLaunchCount=0 \n"
        + "      package=com.nova.tv totalTimeUsed=\"00:10\" lastTimeUsed=\"2026-09-01 20:00:00\" totalTimeVisible=\"00:10\" lastTimeVisible=\"2026-09-01 20:05:00\" totalTimeFS=\"00:00\" lastTimeFS=\"1970-01-01 00:00:00\" appLaunchCount=3 \n"
        + "      package=org.videolan.vlc totalTimeUsed=\"00:10\" lastTimeUsed=\"2026-10-26 20:00:00\" totalTimeVisible=\"00:10\" lastTimeVisible=\"2026-10-26 20:05:00\" totalTimeFS=\"00:00\" lastTimeFS=\"1970-01-01 00:00:00\" appLaunchCount=3 \n";
    private static final long TODAY = Facts.days(2026, 10, 28);

    private static CleanupData data() throws Exception {
        return CleanupData.parse("{\"schema\":1,\"consensusMin\":20,\"keep\":[\"com.keep.me\"],\"homes\":[],\"genericModels\":[],\"genericDevices\":[],"
            + "\"packages\":[\"com.xm.webcontent\",\"com.netflix.ninja\",\"com.tcl.bi\"],\"groups\":[\"preinstalled\",\"optional\",\"tracking\"],"
            + "\"display\":{\"com.netflix.ninja\":\"Netflix\"},\"names\":{\"ar\":{\"com.netflix.ninja\":\"نتفليكس\"}},"
            + "\"lists\":[{\"id\":\"consensus\",\"source\":\"consensus\",\"brands\":[],\"models\":[],\"devices\":[],\"p\":[0,1,2]}]}");
    }

    private static Facts facts(String... user) {
        Facts f = new Facts();
        f.user.addAll(Arrays.asList(user));
        f.autostart.addAll(Facts.componentPackages(BOOT));
        f.ramMb.putAll(Facts.memory(MEM));
        f.usage(USAGE, TODAY);
        return f;
    }

    private static Planner.Tv tv(Facts f, String... installed) {
        return new Planner.Tv("Xiaomi", "Xiaomi", "MiTV-X", "x", new HashSet<>(Arrays.asList(installed)), new HashSet<String>(), f);
    }

    // ---------- the parsers ----------
    @Test public void packagesAreReadFromAListing() {
        assertEquals(new HashSet<>(Arrays.asList("a.b", "c.d.e")), Facts.packages("package:a.b\r\npackage:c.d.e\n\n"));
        assertTrue(Facts.packages(null).isEmpty());
    }

    @Test public void bootReceiversNameTheirPackages() {
        Set<String> s = Facts.componentPackages(BOOT);
        assertTrue(s.contains("com.nova.tv")); assertTrue(s.contains("com.mediatek.backgrounddetection")); assertFalse("the bare \"android\" is the system itself", s.contains("android"));
        assertFalse("the header lines are not packages", s.contains("61"));
    }

    @Test public void memoryIsSummedPerPackageInMegabytesAndStopsAtTheNextSection() {
        Map<String, Integer> m = Facts.memory(MEM);
        assertEquals(Integer.valueOf(135), m.get("ma.chouitv.screensaver"));
        assertEquals("two processes of one app add up", Integer.valueOf(40), m.get("ver3.ycntivi.off"));
        assertNull("a sandboxed process is not an app on its own line of the next section", m.get("Native"));
    }

    @Test public void usageGivesDaysSinceTheLastUseAndTheEpochMeansNever() {
        Facts f = facts("com.nova.tv");
        assertTrue(f.usageKnown);
        assertEquals(Integer.valueOf(57), f.daysSinceUse.get("com.nova.tv"));
        assertEquals(Integer.valueOf(2), f.daysSinceUse.get("org.videolan.vlc"));
        assertFalse("never used: no entry", f.daysSinceUse.containsKey("mitv.service"));
    }

    @Test public void withoutAnyUsageStatisticsNothingIsKnown() {
        Facts f = new Facts();
        f.usage("nothing here\n", TODAY);
        assertFalse(f.usageKnown);
        f.usage(USAGE.split("\n")[0] + "\n", TODAY);
        assertFalse("only epoch dates: the TV gave no real statistics", f.usageKnown);
    }

    @Test public void componentListsGivePackages() {
        assertEquals(new HashSet<>(Arrays.asList("a.b", "c.d")), Facts.componentList("a.b/.Ime\nc.d/.Svc\n"));
        assertEquals(new HashSet<>(Arrays.asList("a.b", "c.d")), Facts.componentList("a.b/.A:c.d/.B"));
        assertTrue(Facts.componentList("null").isEmpty());
    }

    // ---------- the plan ----------
    @Test public void anAppThatStartsWithTheTvAndIsNotUsedIsRecommended() throws Exception {
        Plan p = Planner.plan(data(), tv(facts("com.nova.tv", "org.videolan.vlc", "ver3.ycntivi.off"), "com.xm.webcontent"));
        assertTrue(p.userApps.get("com.nova.tv").recommended);
        assertFalse("used two days ago", p.userApps.get("org.videolan.vlc").recommended);
        assertFalse("does not start with the TV", p.userApps.get("ver3.ycntivi.off").recommended);
        assertEquals("the recommended app comes first", "com.nova.tv", p.groups.get(Group.USER).get(0).pkg);
        assertEquals(57, p.userApps.get("com.nova.tv").daysSinceUse);
    }

    @Test public void anAppThatDoesNotHoldMemoryIsNotRecommendedEvenIfItStartsWithTheTvAndIsUnused() throws Exception {
        Facts f = facts("com.nova.tv", "sleepy.app.z");
        f.autostart.add("sleepy.app.z");
        Plan p = Planner.plan(data(), tv(f));
        assertTrue(p.userApps.get("com.nova.tv").recommended);
        assertFalse("not running: no cost to save, only a guess", p.userApps.get("sleepy.app.z").recommended);
        assertTrue(p.userApps.get("sleepy.app.z").autostart);
    }

    @Test public void aVpnAndOurChannelsAppAreNeverRecommendedOrOffered() throws Exception {
        Facts f = facts("io.privado.android", "my.vpn.app", "com.majnioui.tv.channels");
        for (String x : new String[] {"io.privado.android", "my.vpn.app"}) { f.autostart.add(x); f.ramMb.put(x, 80); }
        Plan p = Planner.plan(data(), tv(f));
        assertFalse(p.userApps.get("io.privado.android").recommended);
        assertFalse(p.userApps.get("my.vpn.app").recommended);
        assertFalse("our Channels app is not for the list", p.userApps.containsKey("com.majnioui.tv.channels"));
    }

    @Test public void streamingAppsAreOfferedWheneverInstalledEvenIfTheChosenListDoesNotNameThem() throws Exception {
        CleanupData d = CleanupData.parse("{\"schema\":1,\"consensusMin\":20,\"keep\":[],\"homes\":[],\"genericModels\":[],\"genericDevices\":[],"
            + "\"packages\":[\"com.tcl.bi\",\"com.netflix.ninja\",\"com.amazon.amazonvideo.livingroom\"],\"groups\":[\"tracking\",\"optional\",\"optional\"],"
            + "\"lists\":[{\"id\":\"consensus\",\"source\":\"consensus\",\"brands\":[],\"models\":[],\"devices\":[],\"p\":[0]}]}");
        Plan p = Planner.plan(d, tv(facts(), "com.tcl.bi", "com.netflix.ninja", "com.amazon.amazonvideo.livingroom"));
        assertEquals(2, p.groups.get(Group.OPTIONAL).size());
        assertFalse("never ticked for the person", p.defaultSelection().contains("com.netflix.ninja"));
        Planner.Tv off = new Planner.Tv("X", "X", "M", "d", new HashSet<>(Arrays.asList("com.netflix.ninja")), new HashSet<>(Arrays.asList("com.netflix.ninja")), facts());
        assertTrue("already off: not offered", Planner.plan(d, off).groups.get(Group.OPTIONAL).isEmpty());
    }

    @Test public void xiaomiHomeScreensAreOfferedOnlyWhenChouiTVIsTheHome() throws Exception {
        Facts f = facts();
        f.chouiHome = true;
        Plan p = Planner.plan(data(), tv(f, "com.mitv.tvhome.atv", "com.mitv.tvhome.mitvplus", "com.mitv.tvhome.michannel", "com.google.android.tvlauncher"));
        Set<String> pre = new HashSet<>();
        for (Planner.Item i : p.groups.get(Group.PREINSTALLED)) pre.add(i.pkg);
        assertTrue(pre.contains("com.mitv.tvhome.atv")); assertTrue(pre.contains("com.mitv.tvhome.mitvplus"));
        assertFalse("only the two", pre.contains("com.mitv.tvhome.michannel"));
        assertFalse("another home is never offered", pre.contains("com.google.android.tvlauncher"));
        assertTrue("ticked by default", p.defaultSelection().containsAll(Arrays.asList("com.mitv.tvhome.atv", "com.mitv.tvhome.mitvplus")));
        f.chouiHome = false;
        Plan none = Planner.plan(data(), tv(f, "com.mitv.tvhome.atv", "com.mitv.tvhome.mitvplus"));
        assertTrue("the TV would be left without a home", none.groups.get(Group.PREINSTALLED).isEmpty());
        Plan off = Planner.plan(data(), new Planner.Tv("X", "X", "M", "d", new HashSet<>(Arrays.asList("com.mitv.tvhome.atv")), new HashSet<>(Arrays.asList("com.mitv.tvhome.atv")), f));
        assertTrue(off.groups.get(Group.PREINSTALLED).isEmpty());
    }

    @Test public void withoutUsageStatisticsNothingIsRecommended() throws Exception {
        Facts f = facts("com.nova.tv");
        f.usage(null, TODAY);
        Plan p = Planner.plan(data(), tv(f));
        assertFalse(p.userApps.get("com.nova.tv").recommended);
        assertEquals(-1, p.userApps.get("com.nova.tv").daysSinceUse);
    }

    @Test public void neverOfferedOurOwnGoogleProtectedHomeOrKeyboardApps() throws Exception {
        Facts f = facts("ma.chouitv.home", "ma.chouitv.installer", "com.google.android.youtube.tv", "com.keep.me", "com.my.launcher", "com.my.keyboard", "com.x.imeapp", "ok.app.one");
        f.sensitive.add("com.x.imeapp");
        Plan p = Planner.plan(data(), tv(f));
        Set<String> got = p.userApps.keySet();
        assertEquals(new HashSet<>(Arrays.asList("com.my.keyboard", "ok.app.one")), got);
        assertFalse("a keyboard is listed but never recommended", p.userApps.get("com.my.keyboard").recommended);
    }

    @Test public void anAppAlreadyInAListOrAlreadyOffIsNotListedTwice() throws Exception {
        Facts f = facts("com.xm.webcontent", "off.app.x", "new.app.y");
        Planner.Tv t = new Planner.Tv("Xiaomi", "Xiaomi", "MiTV-X", "x", new HashSet<>(Arrays.asList("com.xm.webcontent")), new HashSet<>(Arrays.asList("off.app.x")), f);
        Plan p = Planner.plan(data(), t);
        assertEquals(new HashSet<>(Arrays.asList("new.app.y")), p.userApps.keySet());
        assertEquals(1, p.groups.get(Group.PREINSTALLED).size());
    }

    @Test public void theDefaultSelectionHasTheListsAndTheRecommendedAppsButNotStreamingOrTheRest() throws Exception {
        Plan p = Planner.plan(data(), tv(facts("com.nova.tv", "org.videolan.vlc"), "com.xm.webcontent", "com.netflix.ninja", "com.tcl.bi"));
        Set<String> sel = p.defaultSelection();
        assertTrue(sel.contains("com.xm.webcontent")); assertTrue(sel.contains("com.tcl.bi")); assertTrue(sel.contains("com.nova.tv"));
        assertFalse("streaming is for the person to tick", sel.contains("com.netflix.ninja"));
        assertFalse(sel.contains("org.videolan.vlc"));
    }

    @Test public void namesAreReadableAndArabicWhereWeHaveIt() throws Exception {
        CleanupData d = data();
        assertEquals("Netflix", d.label("com.netflix.ninja", "fr"));
        assertEquals("نتفليكس", d.label("com.netflix.ninja", "ar"));
        assertEquals("an unknown package is made readable, never shown raw", "Pkg", d.label("some.pkg", "en"));
    }

    // ---------- the run ----------
    @Test public void onlyTheTickedPackagesAreTurnedOffAndTheOthersAreNeverTouched() throws Exception {
        FakeTv t = new FakeTv();
        Plan p = Planner.plan(data(), tv(facts("com.nova.tv", "org.videolan.vlc"), "com.xm.webcontent", "com.netflix.ninja", "com.tcl.bi"));
        Set<String> ticked = new HashSet<>(Arrays.asList("com.nova.tv", "com.netflix.ninja"));
        Executor.Result r = new Executor(t, n -> { }, "0123456789abcdef").runSelected(p, ticked, new Executor.Listener() {
            public void started(int total) { assertEquals(2, total); }
            public void package_(Group g, String pkg, boolean ok, int done, int total) { }
            public void groupDone(Group g, boolean healthy) { }
        });
        assertEquals(new HashSet<>(ticked), new HashSet<>(r.disabled));
        assertEquals(new HashSet<>(ticked), t.disabled);
        for (String c : t.log) assertFalse("never touched: " + c, c.contains("webcontent") || c.contains("com.tcl.bi") || c.contains("vlc"));
    }

    @Test public void aTickedUserAppIsTurnedOffInItsOwnGroupAfterTheListsAndTheRunRollsBackIfTheTvIsUnwell() throws Exception {
        FakeTv t = new FakeTv();
        t.breakAfterDisables = 2;   // the TV loses its home once two apps are off
        Plan p = Planner.plan(data(), tv(facts("com.nova.tv"), "com.xm.webcontent", "com.tcl.bi"));
        Executor.Result r = new Executor(t, n -> { }, "0123456789abcdef").runSelected(p, p.defaultSelection(), new Executor.Listener() {
            public void started(int total) { }
            public void package_(Group g, String pkg, boolean ok, int done, int total) { }
            public void groupDone(Group g, boolean healthy) { }
        });
        assertNotNull("a group left the TV unwell", r.rolledBack);
        assertTrue("what the unwell group turned off was put back", t.disabled.size() < 3);
    }

    @Test public void theRunByGroupStillWorks() throws Exception {
        FakeTv t = new FakeTv();
        Plan p = Planner.plan(data(), tv(facts(), "com.xm.webcontent", "com.tcl.bi", "com.netflix.ninja"));
        Executor.Result r = new Executor(t, n -> { }, "0123456789abcdef").run(p, java.util.EnumSet.of(Group.TRACKING), new Executor.Listener() {
            public void started(int total) { }
            public void package_(Group g, String pkg, boolean ok, int done, int total) { }
            public void groupDone(Group g, boolean healthy) { }
        });
        assertEquals(Arrays.asList("com.tcl.bi"), r.disabled);
    }
}
